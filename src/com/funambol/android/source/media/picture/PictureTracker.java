/*
 * Funambol is a mobile platform developed by Funambol, Inc.
 * Copyright (C) 2010 Funambol, Inc.
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

package com.funambol.android.source.media.picture;

import java.util.Enumeration;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.funambol.android.AndroidCustomization;
import com.funambol.android.IntKeyValueSQLiteStore;
import com.funambol.android.providers.MediaContentProvider;

import com.funambol.android.providers.PicturesContentProvider;
import com.funambol.android.source.media.MediaTracker;

import com.funambol.storage.StringKeyValueStore;
import com.funambol.client.configuration.Configuration;
import com.funambol.client.customization.Customization;
import com.funambol.client.source.AppSyncSource;
import com.funambol.storage.StringKeyValuePair;
import com.funambol.storage.StringKeyValueSQLiteStore;
import com.funambol.sync.SourceConfig;
import com.funambol.util.Log;


public class PictureTracker extends MediaTracker {
    private static final String TAG = "PictureTracker";

    public PictureTracker(Context context, StringKeyValueStore status, AppSyncSource appSource,
                          Configuration configuration) {
        super(context, status, appSource, configuration);
    }

    protected Uri getProviderUri() {
        return PicturesContentProvider.EXTERNAL_CONTENT_URI;
    }

    /**
     * Creates a tracker data store for pictures.
     */
    public static StringKeyValueSQLiteStore createTrackerStore(Context context,
            SourceConfig sc, PictureAppSyncSourceConfig config,
            Customization customization) {

        StringBuffer trackerStoreName = new StringBuffer();
        trackerStoreName.append(sc.getName());
        trackerStoreName.append("_fn");

        StringKeyValueSQLiteStore trackerStore = new StringKeyValueSQLiteStore(context,
                ((AndroidCustomization)customization).getFunambolSQLiteDbName(),
                trackerStoreName.toString());

        if(!config.getUseFileNameTrackerStore()) {
            // In v9 the pictures tracker used the picture ID as key, in new
            // version it uses the filename instead
            if (Log.isLoggable(Log.DEBUG)) {
                Log.debug(TAG, "Migrating pictures tracker store");
            }
            IntKeyValueSQLiteStore oldTrackerStore = null;
            try {
                oldTrackerStore = new IntKeyValueSQLiteStore(context,
                    ((AndroidCustomization)customization).getFunambolSQLiteDbName(),
                    sc.getName());

                Enumeration keyValuePairs = oldTrackerStore.keyValuePairs();
                while(keyValuePairs.hasMoreElements()) {
                    StringKeyValuePair pair = (StringKeyValuePair)keyValuePairs.nextElement();
                    String key = pair.getKey();
                    String value = pair.getValue();
                    String newKey = readFileNameFromId(context, key);
                    if(newKey != null) {
                        trackerStore.add(newKey, value);
                    }
                    // Do not track unexisting items which are actually not
                    // propagated to the server
                }
                trackerStore.save();
                config.setUseFileNameTrackerStore(true);
                config.save();
            } catch(Throwable t) {
                Log.error(TAG, "Failed to migrate pictures tracker store", t);
            } finally {
                try {
                    if(oldTrackerStore != null) {
                        oldTrackerStore.reset();
                    }
                } catch(Throwable t) { }
            }
        }
        return trackerStore;
    }

    private static String readFileNameFromId(Context context, String id) {
        StringBuffer selection = new StringBuffer();
        selection.append(MediaContentProvider._ID).append("=").append(id);
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    PicturesContentProvider.EXTERNAL_CONTENT_URI,
                    new String[] { MediaContentProvider._DATA },
                    selection.toString(), null, null);
            if(cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            } else {
                return null;
            }
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
    }
}


