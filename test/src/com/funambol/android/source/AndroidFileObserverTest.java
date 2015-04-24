/**
 * Funambol is a mobile platform developed by Funambol, Inc. 
 * Copyright (C) 2003 - 2011 Funambol, Inc.
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

package com.funambol.android.source;

import android.content.Context;
import java.io.File;

import android.os.Environment;
import android.test.AndroidTestCase;
import com.funambol.android.AndroidAppSyncSource;
import com.funambol.android.AndroidAppSyncSourceManager;
import com.funambol.android.AndroidConfiguration;
import com.funambol.android.AndroidCustomization;
import com.funambol.android.AndroidLocalization;
import com.funambol.android.source.media.file.AndroidFileObserver;
import com.funambol.client.configuration.Configuration;
import com.funambol.client.customization.Customization;
import com.funambol.client.localization.Localization;
import com.funambol.client.source.AppSyncSource;
import com.funambol.client.source.AppSyncSourceManager;
import com.funambol.platform.FileAdapter;
import com.funambol.util.Log;

import java.io.OutputStream;

public class AndroidFileObserverTest extends AndroidTestCase {
    private static final String TAG_LOG = "AndroidFileObserverTest";
    
    private boolean testFolderCreated = false;
    
    private TestAndroidFileObserver fileObserver;
    private AppSyncSource testss = new AndroidAppSyncSource("testss");

    private File basePath;

    @Override
    protected void setUp() throws Exception {
        Log.setLogLevel(Log.TRACE);

        createTestFolder();

        Customization customization = AndroidCustomization.getInstance();
        Localization localization = AndroidLocalization.getInstance(mContext);
        AppSyncSourceManager manager = AndroidAppSyncSourceManager.getInstance(
                customization, localization, mContext);
        Configuration configuration = AndroidConfiguration.getInstance(mContext,
                customization,
                manager);
        configuration.setC2SPushEnabled(true);

        fileObserver = new TestAndroidFileObserver(testss,
                basePath.getAbsolutePath(), null, mContext, configuration);
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testFileCreated() throws Exception {
        Log.debug(TAG_LOG, "testFileCreated has started");

        fileObserver.startWatching();

        FileAdapter tempFile = new FileAdapter(getFullPath("temp.txt"));
        tempFile.create();
        tempFile.close();

        tempFile = new FileAdapter(getFullPath("temp.txt"));

        assertTrue("File doesn't exist", tempFile.exists());

        waitForTriggeredSync();

        assertEquals(fileObserver.lastSource, testss);
        fileObserver.lastSource = null;

        fileObserver.stopWatching();

        tempFile.delete();
    }

    public void testFileUpdated() throws Exception {
        Log.debug(TAG_LOG, "testFileUpdated has started");

        FileAdapter tempFile = new FileAdapter(getFullPath("temp.txt"));
        tempFile.create();
        tempFile.close();
        
        fileObserver.startWatching();

        tempFile = new FileAdapter(getFullPath("temp.txt"));
        OutputStream os = tempFile.openOutputStream();
        os.write('h');
        os.write('e');
        os.write('l');
        os.write('l');
        os.write('0');
        os.close();
        tempFile.close();

        waitForTriggeredSync();

        assertEquals(fileObserver.lastSource, testss);
        fileObserver.lastSource = null;

        fileObserver.stopWatching();

        tempFile = new FileAdapter(getFullPath("temp.txt"));
        tempFile.delete();
        tempFile.close();
    }

    public void testFileDeleted() throws Exception {
        Log.debug(TAG_LOG, "testFileDeleted has started");

        FileAdapter tempFile = new FileAdapter(getFullPath("temp.txt"));
        tempFile.create();
        tempFile.close();

        fileObserver.startWatching();

        tempFile = new FileAdapter(getFullPath("temp.txt"));
        tempFile.delete();
        tempFile.close();

        waitForTriggeredSync();

        assertEquals(fileObserver.lastSource, testss);
        fileObserver.lastSource = null;

        fileObserver.stopWatching();
    }

    public void testFileRenamed() throws Exception {
        Log.debug(TAG_LOG, "testFileRenamed has started");

        fileObserver.startWatching();

        FileAdapter tempFile = new FileAdapter(getFullPath("temp.txt"));
        tempFile.create();
        tempFile.close();

        tempFile = new FileAdapter(getFullPath("temp.txt"));
        
        assertTrue("File doesn't exist", tempFile.exists());

        tempFile.rename(getFullPath("temp_renamed.txt"));
        tempFile.close();

        waitForTriggeredSync();

        assertEquals(fileObserver.lastSource, testss);
        fileObserver.lastSource = null;

        fileObserver.stopWatching();

        tempFile = new FileAdapter(getFullPath("temp_renamed.txt"));
        tempFile.delete();
        tempFile.close();
    }

    private String getFullPath(String fileName) {
        return basePath.getAbsolutePath() + "/" + fileName;
    }

    /**
     * Creates test folder
     */
    protected void createTestFolder() {
        //android executes testAndroidTestCaseSetupPropertly in addition to
        //other tests
        synchronized (this) {
            if (testFolderCreated) return;
            
            File baseTestPath;
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                baseTestPath = Environment.getExternalStorageDirectory();
            } else {
                baseTestPath = getContext().getCacheDir();
            }
            basePath = new File(baseTestPath, "observer");
            basePath.mkdir();
            assertTrue("Directory doesn't exists", basePath.isDirectory());
            testFolderCreated = true;
            
            Log.debug(TAG_LOG, "Base path for FileObserver tests: " + basePath.getAbsolutePath());
        }
    }

    private void waitForTriggeredSync() {
        synchronized (fileObserver) {
            boolean done = false;
            while (!done) {
                try {
                    fileObserver.wait(2000);
                    done = true;
                } catch (InterruptedException e) {
                }
            }
        }
    }
    private class TestAndroidFileObserver extends AndroidFileObserver {

        public AppSyncSource lastSource;
        
        public TestAndroidFileObserver(AppSyncSource appSource, String baseDirectory,
            String extensions[], Context context, Configuration configuration) {
            super(appSource, baseDirectory, extensions, context, configuration);
        }

        @Override
        protected void triggerSynchronizationForSource(AppSyncSource appSource) {
            synchronized (this) {
                lastSource = appSource;
                this.notifyAll();
            }
        }
    }
}
