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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import android.os.FileObserver;
import android.test.AndroidTestCase;
import android.text.TextUtils;

import com.funambol.util.Log;

public class FileObserverTest extends AndroidTestCase {
    private static final String TAG_LOG = "FileObserverTest";
    
    private boolean testFolderCreated = false;
    private int backupLogLevel;
    private TestFileObserver fileObserver;
    File basePath;

    @Override
    protected void setUp() throws Exception {
        Log.setLogLevel(Log.DEBUG);

        createTestFolder();
        fileObserver = new TestFileObserver(basePath.getAbsolutePath());
    }

    @Override
    protected void tearDown() throws Exception {
        Log.setLogLevel(backupLogLevel);
    }
    
    
    public void testCreateFile() throws Exception {
        Log.debug(TAG_LOG, "testCreateFile has started");
        assertEquals("Not empty events queue", 0, fileObserver.events.size());

        fileObserver.startWatching();

        //create a temp file
        File tempFile = File.createTempFile("file_observer_test", ".txt", basePath);
        // make file changes and wait for them
        assertTrue("File doesn't exist", tempFile.exists());
        //CREATE, OPEN, CLOSE_NOWRITE
        waitForEvent();
        waitForEvent();
        waitForEvent();
        fileObserver.stopWatching();
        assertEquals("Wrong events in queue", 3, fileObserver.events.size());
        
        tempFile.delete();
    }
    
    public void testDeleteFile() throws Exception {
        Log.debug(TAG_LOG, "testDeleteFile has started");
        assertEquals("Not empty events queue", 0, fileObserver.events.size());

        //create a temp file
        File tempFile = File.createTempFile("file_observer_test", ".txt", basePath);
        // make file changes and wait for them
        assertTrue("File doesn't exist", tempFile.exists());
        
        //clear event queue
        fileObserver.events.clear();
        assertEquals("Not empty events queue", 0, fileObserver.events.size());
        fileObserver.startWatching();

        tempFile.delete();
        assertFalse("File not deleted", tempFile.exists());

        //DELETE
        waitForEvent();
        fileObserver.stopWatching();
        assertEquals("Wrong events in queue", 1, fileObserver.events.size());
    }

    public void testFileChanged() throws Exception {
        Log.debug(TAG_LOG, "testFileChanged has started");
        //create a temp file
        File tempFile = File.createTempFile("file_observer_test", ".txt", basePath);
        // make file changes and wait for them
        assertTrue("File doesn't exist", tempFile.exists());
        
        //clear event queue
        fileObserver.events.clear();
        assertEquals("Not empty events queue", 0, fileObserver.events.size());
        fileObserver.startWatching();

        FileOutputStream out = new FileOutputStream(tempFile);
        out.write(0x20);
        out.close();

        //MODIFY, OPEN, MODIFY, CLOSE_WRITE
        waitForEvent();
        waitForEvent();
        waitForEvent();
        waitForEvent();
        fileObserver.stopWatching();
        assertEquals("Wrong events in queue", 4, fileObserver.events.size());
        
        tempFile.delete();
    }
    
    public void testFileMoved() throws Exception {
        Log.debug(TAG_LOG, "testFileMoved has started");
        //create a temp file
        File tempFile = File.createTempFile("file_observer_test", ".txt", basePath);
        // make file changes and wait for them
        assertTrue("File doesn't exist", tempFile.exists());
        
        //clear event queue
        fileObserver.events.clear();
        assertEquals("Not empty events queue", 0, fileObserver.events.size());

        File tempFile2 = new File(tempFile.getParent(), "test_12312.txt");
        if (tempFile2.exists()) tempFile2.delete();
        
        fileObserver.startWatching();
        boolean success = tempFile.renameTo(tempFile2);
        assertTrue("file wasn't moved", success);

        //MOVED_FROM, MOVED_TO
        waitForEvent();
        waitForEvent();
        fileObserver.stopWatching();
        assertEquals("Wrong events in queue", 2, fileObserver.events.size());
        
        tempFile2.delete();
    }

    
    private void waitForEvent() {
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
    

    
    /**
     * Private class for recording event from {@link FileObserver}
     */
    private static class FileObserverRecord {
        private final int event;
        private final String path;

        public FileObserverRecord(int event, String path) {
            this.event = event;
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public int getEvent() {
            return event;
        }
    }

    /**
     * Private class that extends FileObserver
     */
    private static class TestFileObserver extends FileObserver {
        public List<FileObserverRecord> events = new ArrayList<FileObserverRecord>();

        public TestFileObserver(String path) {
            super(path);
        }

        public void onEvent(int event, String path) {
            synchronized (this) {
                //sometimes the observer creates dirty events with null path
                if (!TextUtils.isEmpty(path)) {
                    events.add(new FileObserverRecord(event, path));
                    Log.debug(TAG_LOG, "event: " + getEventString((Integer)event) + " path: " + path);
                }
                this.notifyAll();
            }
        }
        
        private String getEventString(int event) {
            switch (event) {
                case  FileObserver.ACCESS:
                    return "ACCESS";
                case FileObserver.MODIFY:
                    return "MODIFY";
                case FileObserver.ATTRIB:
                    return "ATTRIB";
                case FileObserver.CLOSE_WRITE:
                    return "CLOSE_WRITE";
                case FileObserver.CLOSE_NOWRITE:
                    return "CLOSE_NOWRITE";
                case FileObserver.OPEN:
                    return "OPEN";
                case FileObserver.MOVED_FROM:
                    return "MOVED_FROM";
                case FileObserver.MOVED_TO:
                    return "MOVED_TO";
                case FileObserver.CREATE:
                    return "CREATE";
                case FileObserver.DELETE:
                    return "DELETE";
                case FileObserver.DELETE_SELF:
                    return "DELETE_SELF";
                case FileObserver.MOVE_SELF:
                    return "MOVE_SELF";
                default:
                    return "UNKNOWN";
            }
        }
    }
}
