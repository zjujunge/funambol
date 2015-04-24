/*
 * Funambol is a mobile platform developed by Funambol, Inc. 
 * Copyright (C) 2003 - 2009 Funambol, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission 
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY FUNAMBOL, FUNAMBOL DISCLAIMS THE 
 * WARRANTY OF NON INFRINGEMENT  OF THIRD PARTY RIGHTS.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License 
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 * 
 * You can contact Funambol, Inc. headquarters at 643 Bair Island Road, Suite 
 * 305, Redwood City, CA 94063, USA, or at email address info@funambol.com.
 * 
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 * 
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * "Powered by Funambol" logo. If the display of the logo is not reasonably 
 * feasible for technical reasons, the Appropriate Legal Notices must display
 * the words "Powered by Funambol".
 */

package com.funambol.android.integration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.FileOutputStream;

import android.app.Instrumentation;
import android.os.Environment;
import android.os.StatFs;

import com.funambol.android.AndroidCustomization;
import com.funambol.android.App;
import com.funambol.android.AppInitializer;
import com.funambol.android.controller.AndroidController;
import com.funambol.client.customization.Customization;
import com.funambol.client.source.AppSyncSourceManager;
import com.funambol.client.test.media.MediaRobot;
import com.funambol.client.test.basic.BasicUserCommands;
import com.funambol.client.test.util.TestFileManager;
import com.funambol.org.json.me.JSONObject;
import com.funambol.platform.FileAdapter;
import com.funambol.sapisync.source.FileSyncSource;
import com.funambol.sync.SyncConfig;
import com.funambol.sync.SyncSource;
import com.funambol.util.Log;
import com.funambol.util.StringUtil;
import java.util.Enumeration;


public class AndroidMediaRobot extends MediaRobot {
   
    private static final String TAG_LOG = "AndroidMediaRobot";
    
    private static final String LOCAL_STORAGE_FILLER_PREFIX = "__LOCAL_STORAGE_FILLER__";
    private static final String LOCAL_STORAGE_FILLER_SUFFIX = ".tmp";
    private static final FilenameFilter LOCAL_STORAGE_FILLER_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String filename) {
            if (filename.endsWith(LOCAL_STORAGE_FILLER_SUFFIX)) {
                return filename.startsWith(LOCAL_STORAGE_FILLER_PREFIX);
            }
            return false;
        }
    };
    
    private Instrumentation instrumentation = null;

    public AndroidMediaRobot(Instrumentation instrumentation, TestFileManager fileManager) {
        super(fileManager);
        this.instrumentation = instrumentation;
    }

    @Override
    protected AppSyncSourceManager getAppSyncSourceManager() {
        if (appSourceManager == null) {
            AppInitializer appInitializer = App.i().getAppInitializer();
            appSourceManager = appInitializer.getAppSyncSourceManager();
        }
        return appSourceManager;
    }

    @Override
    protected SyncConfig getSyncConfig() {
        return AndroidController.getInstance().getConfiguration().getSyncConfig();
    }

    @Override
    protected void fillLocalStorage() {
        Log.trace(TAG_LOG, "Filling local storage with fake data");

        String sdDir = Environment.getExternalStorageDirectory().toString();
        StatFs statistics = new StatFs(sdDir);
        statistics.restat(sdDir);
        int blockSize = statistics.getBlockSize();
        float availableBytes = (float)statistics.getAvailableBlocks() * (float)blockSize;

        long availableMega = (long)(availableBytes / ((float)(1024*1024)));
        if (Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG_LOG, "availableMega=" + availableMega + " on " + sdDir + " where block size is " + blockSize);
        }
        
        long megaToFill = availableMega - 1;
        if (megaToFill <= 0) {
            Log.trace(TAG_LOG, "Device already full");
            return;
        }
        
        try {
            createFileWithSizeOnDevice(megaToFill, null, null);
        } catch (IOException e) {   
            Log.error(TAG_LOG, "Cannot create temporary file or fill it with fake data", e);
        }
    }
    
    @Override
    protected void restoreLocalStorage() {
        
        // rm __LOCAL_STORAGE_FILLER__*.tmp
        File[] foam = Environment.getExternalStorageDirectory().listFiles(
                LOCAL_STORAGE_FILLER_FILTER);
        if (foam != null) {
            for (File file : foam) {
                if (Log.isLoggable(Log.TRACE)) {
                    Log.trace(TAG_LOG, "Deleting temporary file " + file.getAbsolutePath());
                }
                file.delete();
            }
        }
    }

    @Override
    protected void createFileWithSizeOnDevice(long megaSize, String header, String footer) throws IOException {
        if (megaSize <= 0) {
            Log.trace(TAG_LOG, "Cannot create file with a size of zero or less megabytes");
        }

        Log.trace(TAG_LOG, "(It will take some time... Please wait)");

        // We keep a file size limit of 4GB which is the limit on FAT32 file
        // systems (such as SDCard)
        do {
            File foam = File.createTempFile(
                    LOCAL_STORAGE_FILLER_PREFIX, 
                    LOCAL_STORAGE_FILLER_SUFFIX, 
                    Environment.getExternalStorageDirectory());
            foam.deleteOnExit(); // unreliable!!

            if (foam == null) {
                if (Log.isLoggable(Log.TRACE)) {
                    Log.trace(TAG_LOG, "Cannot fill temporary file ");
                }
            }

            RandomAccessFile expandingFoam = new RandomAccessFile(foam, "rw");

            long fileSize = megaSize;
            if (fileSize > 512) {
                // clip to 512Mb files
                fileSize = 512; 
            }
            megaSize = megaSize - fileSize;

            fileSize = fileSize * 1024 * 1024;
            if (Log.isLoggable(Log.TRACE)) {
                Log.trace(TAG_LOG, "Creating a temp file of size: " + fileSize);
            }

            expandingFoam.setLength(fileSize);
        
            //write the header of the file
            if (!StringUtil.isNullOrEmpty(header)) {
                Log.trace(TAG_LOG, "writing header of size " + header.length());
                expandingFoam.seek(0);
                expandingFoam.write(header.getBytes("UTF-8"));
            }

            //write the footer of the file
            if (!StringUtil.isNullOrEmpty(footer)) {
                byte[] byteFooter = footer.getBytes("UTF-8");
                Log.trace(TAG_LOG, "writing footer of size " + byteFooter.length);
                long displacement = fileSize - byteFooter.length;
                if (displacement < 0) {
                    displacement = 0;
                }
                expandingFoam.seek(displacement);
                expandingFoam.write(byteFooter);
            }

            expandingFoam.close();

            if (Log.isLoggable(Log.TRACE)) {
                Log.trace(TAG_LOG, "Created a " + fileSize + "-byte " +
                        "temporary file called " + foam.getAbsolutePath());
            }

        } while(megaSize > 0);
    }

    protected String getMediaHubPath() {
        Customization customization = AndroidController.getInstance().getCustomization();
        
        String sdCardRoot = Environment.getExternalStorageDirectory().toString();
        String directoryName = ((AndroidCustomization)customization).getDefaultFilesSDCardDir();

        StringBuffer defaultDirBuilder = new StringBuffer();
        defaultDirBuilder.append(sdCardRoot);
        defaultDirBuilder.append("/");
        defaultDirBuilder.append(directoryName);

        return defaultDirBuilder.toString();
    }

    @Override
    public void createFile(String fileName, long fileSize) throws Exception {
        if (Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG_LOG, "Creating file in mediahub folder with name " + fileName + 
                    " and size " + fileSize);
        }

        String tgtDir = getMediaHubPath();
        File defaultDir = new File(tgtDir);
        boolean result = defaultDir.mkdirs();
        if (result)
            Log.trace(TAG_LOG, "Base folder created.");
        else
            Log.trace(TAG_LOG, "Cannot create base folder.");

        File file = new File(defaultDir, fileName);
        if (Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG_LOG, "Complete file name will be: " + file.getAbsolutePath());
        }
        
        Log.trace(TAG_LOG, "Creation in progress... It will take some time, please wait)");
        RandomAccessFile expandingFoam = new RandomAccessFile(file, "rw");
        expandingFoam.setLength(fileSize);
        expandingFoam.close();
        
        Log.trace(TAG_LOG, "Created!");
    }

}
