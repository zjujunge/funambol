/**
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

package com.funambol.android.source.media;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;

import com.funambol.android.App;
import com.funambol.android.AndroidCustomization;
import com.funambol.android.providers.MediaContentProvider;
import com.funambol.sync.SourceConfig;
import com.funambol.sync.SyncItem;
import com.funambol.sync.SyncSource;
import com.funambol.sync.SyncException;
import com.funambol.sync.client.ChangesTracker;

import com.funambol.client.configuration.Configuration;
import com.funambol.client.source.FunambolFileSyncSource;

import com.funambol.util.Log;


public abstract class MediaSyncSource extends FunambolFileSyncSource {

    private static final String TAG_LOG = "MediaSyncSource";
    
    protected Configuration configuration;
    protected MediaAppSyncSource mediaAppSyncSource;
    protected Context context;

    //------------------------------------------------------------- Constructors

    /**
     * MediaSyncSource constructor: initialize source config.
     * 
     * @param config
     * @param tracker
     * @param mediaAppSyncSource
     * @param configuration
     */
    public MediaSyncSource(SourceConfig config, ChangesTracker tracker,
                           String dataDirectory, String tempDirectory,
                           MediaAppSyncSource mediaAppSyncSource, Configuration configuration,
                           Context context)
    {
        super(config,
              tracker,
              dataDirectory,
              tempDirectory,
              mediaAppSyncSource.getConfig().getMaxItemSize(),
              ((MediaAppSyncSourceConfig) mediaAppSyncSource.getConfig()).getIncludeOlderMedia()
                  ? NO_LIMIT_ON_ITEM_AGE
                  : configuration.getFirstRunTimestamp(),
              AndroidCustomization.getInstance());
        
        this.mediaAppSyncSource = mediaAppSyncSource;
        this.configuration = configuration;
        this.context = context;
    }

    @Override
    public void beginSync(int syncMode, boolean resume) throws SyncException {
        super.beginSync(syncMode, resume);
        // This source does not propagate updates. Even if they are computed we
        // discard them and the corresponding counter must be reset
        clientReplaceItemsNumber = 0;
    }

    protected abstract Uri getProviderUri();

    @Override
    public void endSync() throws SyncException {
        super.endSync();
        // We must save the configuration here, because we are sure this code is
        // executed no matter where the sync is started from
        mediaAppSyncSource.getConfig().saveSourceSyncConfig();
        mediaAppSyncSource.getConfig().commit();
    }

    public void clearTracker() {
        if (Log.isLoggable(Log.INFO)) {
            Log.info(TAG_LOG, "Emptying the media tracker");
        }
        tracker.empty();
    }

    @Override
    protected int addItem(SyncItem item) throws SyncException {
        int status = super.addItem(item);
        if (status == SyncSource.SUCCESS_STATUS) {
            if (Log.isLoggable(Log.DEBUG)) {
                Log.debug(TAG_LOG, "Starting a media scan for " + item.getKey());
            }
            MediaScannerConnection scanner = null;
            try {
                scanner = new MediaScannerConnection(App.i().getApplicationContext(), null);
                scanner.connect();
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                }
                if (scanner.isConnected()) {
                    if (Log.isLoggable(Log.DEBUG)) {
                        Log.debug(TAG_LOG, "Requesting scan for file " + item.getKey());
                    }
                    scanner.scanFile(item.getKey(), null);
                }
            } finally {
                if (scanner != null) {
                    scanner.disconnect();
                }
            }
        }
        return status;
    }

    @Override
    public int deleteItem(String key) throws SyncException {
        long id = findIdFromKey(key);
        if(id != -1) {
            Uri itemUri = ContentUris.withAppendedId(getProviderUri(), id);
            int delRows = context.getContentResolver().delete(itemUri, null, null);
            if (delRows != 1) {
                Log.error(TAG_LOG, "Cannot remove media from DB " + delRows);
            }
        }
        return super.deleteItem(key);
    }

    protected long findIdFromKey(String key) {
        StringBuffer selection = new StringBuffer();
        selection.append(MediaContentProvider._DATA).append("='").append(key).append("'");
        Cursor cursor = context.getContentResolver().query(getProviderUri(),
                null, selection.toString(), null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            // The item no longer exists
            if (Log.isLoggable(Log.INFO)) {
                Log.info(TAG_LOG, "Cannot find id for key " + key);
            }
            return -1;
        }
        try {
            int idIdx = cursor.getColumnIndexOrThrow(MediaContentProvider._ID);
            long id = cursor.getLong(idIdx);
            return id;
        } finally {
            cursor.close();
        }
    }

    @Override
    public SyncItem getNextUpdatedItem() throws SyncException {
        // Ignore outgoing updates and set the item status to success
        SyncItem item = super.getNextUpdatedItem();
        if(item != null) {
            setItemStatus(item.getKey(), SyncSource.SUCCESS_STATUS);
        }
        return null;
    }

    @Override
    protected int updateItem(SyncItem item) throws SyncException {
        // Ignore incoming updates
        return SyncSource.SUCCESS_STATUS;
    }
    
    @Override
    protected boolean isDeleteAllItemsAllowed() {
        //for media, deletion of local files is not allowed
        return false;
    }

}

