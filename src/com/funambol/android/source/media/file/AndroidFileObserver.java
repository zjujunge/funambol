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
package com.funambol.android.source.media.file;

import android.content.Context;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;

import android.os.FileObserver;

import com.funambol.android.AndroidCustomization;
import com.funambol.android.services.AutoSyncServiceHandler;
import com.funambol.client.configuration.Configuration;

import com.funambol.client.controller.SynchronizationController;
import com.funambol.client.source.AppSyncSource;
import com.funambol.util.Log;
import com.funambol.util.StringUtil;

/**
 * Observer for mediahub files folder
 *
 */
public class AndroidFileObserver extends FileObserver {

    private static final String TAG_LOG = "AndroidFileObserver";

    private String baseDirectory = null;

    private Hashtable<AppSyncSource, String[]> sources = new Hashtable<AppSyncSource,String[]>();

    private static final String[] ALL_EXTENSIONS = new String[0];

    private Context context;

    private Configuration configuration;

    public AndroidFileObserver(AppSyncSource appSource, String baseDirectory,
            String extensions[], Context context, Configuration configuration) {
        super(baseDirectory, FileObserver.CLOSE_NOWRITE | FileObserver.CLOSE_WRITE |
                FileObserver.DELETE | FileObserver.MOVED_FROM | FileObserver.MOVED_TO);
        this.baseDirectory = baseDirectory;
        this.context = context;
        this.configuration = configuration;
        add(appSource, extensions);
    }

    public void add(AppSyncSource appSource, String extensions[]) {
        if (extensions != null) {
            sources.put(appSource, extensions);
        } else {
            sources.put(appSource, ALL_EXTENSIONS);
        }
    }

   /* (non-Javadoc)
     * @see android.os.FileObserver#onEvent(int, java.lang.String)
     */
    @Override
    public void onEvent(int event, String path) {
        if(Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG_LOG, "Detected change type=" + event + " path=" + path);
        }
        // Sometimes the system triggers events with path=null and unknown event codes
        if(!StringUtil.isNullOrEmpty(path)) {

            if(!configuration.isC2SPushEnabled()) {
                if(Log.isLoggable(Log.TRACE)) {
                    Log.trace(TAG_LOG, "C2S push is not enabled");
                }
                return;
            }

            // The given path includes only the file path relative to the
            // monitored directory
            path = getFileFullName(path);

            // Check the source to which this file belongs to
            AppSyncSource appSource = findSource(path);
            if (appSource != null) {
                triggerSynchronizationForSource(appSource);
            } else {
                if (Log.isLoggable(Log.TRACE)) {
                    Log.trace(TAG_LOG, "Modified a file without source associated");
                }
            }
        }
    }

    protected void triggerSynchronizationForSource(AppSyncSource appSource) {

        Vector sourcesToSync = new Vector();
        sourcesToSync.addElement(appSource);

        int delay = AndroidCustomization.getInstance().getC2SPushDelay();
        AutoSyncServiceHandler autoSyncHandler = new AutoSyncServiceHandler(context);
        autoSyncHandler.startSync(SynchronizationController.PUSH, sourcesToSync, delay);
    }

    private AppSyncSource findSource(String path) {
        Enumeration keys = sources.keys();
        while(keys.hasMoreElements()) {
            AppSyncSource appSource = (AppSyncSource)keys.nextElement();
            String extensions[] = sources.get(appSource);
            if (extensions == ALL_EXTENSIONS) {
                return appSource;
            }
            for(int i=0;i<extensions.length;++i) {
                String ext = extensions[i];
                if (path.endsWith(ext)) {
                    return appSource;
                }
            }
        }
        return null;
    }

    private String getFileFullName(String fileName) {
        StringBuffer fullName = new StringBuffer();
        fullName.append(baseDirectory);
        fullName.append("/");
        fullName.append(fileName);
        return fullName.toString();
    }

}
