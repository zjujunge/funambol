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

package com.funambol.android.source.media;

import android.content.Context;
import android.net.Uri;
import android.database.Cursor;

import com.funambol.android.source.AndroidChangesTracker;
import com.funambol.android.providers.MediaContentProvider;

import com.funambol.client.configuration.Configuration;
import com.funambol.client.source.AppSyncSource;
import com.funambol.sapisync.source.FileSyncSource.ItemsSorter;
import com.funambol.storage.StringKeyValueStore;
import com.funambol.sync.client.CacheTracker;
import com.funambol.sync.SyncItem;
import com.funambol.sync.SyncSource;
import com.funambol.sync.client.TrackerException;

import com.funambol.util.Log;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * An AndroidChangesTracker implementation that aims to keep trace of media
 * files' informations. The main method is used to compute the fingerprint for
 * a given media file.
 */
public abstract class MediaTracker extends CacheTracker implements AndroidChangesTracker {

    private static final String TAG_LOG = "MediaTracker";

    protected AppSyncSource appSource;
    protected Configuration configuration;

    protected Context context = null;

    private ItemsSorter itemsSorter = null;

    /**
     * Default Constructor
     * @param context the Context object related to this tracker
     * @param status the StringKeyValueStore object for this tracker
     */
    public MediaTracker(Context context, StringKeyValueStore status,
                        AppSyncSource appSource, Configuration configuration) {
        super(status);
        this.context = context;
        this.appSource = appSource;
        this.configuration = configuration;
    }

    /**
     * Calculates the fingerprint of a media item.
     * @param item the SyncItem object that contains the file for which the
     * computation must be done
     * @return String the String formatted fingerprint of the file related to
     * the given SyncItem object
     */
    @Override
    protected String computeFingerprint(SyncItem item) {
        if (Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG_LOG, "computeFingerprint");
        }
        Cursor cursor = null;
        try {
            StringBuffer selection = new StringBuffer();
            selection.append(MediaContentProvider._DATA).append("='")
                    .append(item.getKey()).append("'");
            cursor = context.getContentResolver().query(getProviderUri(), null,
                    selection.toString(), null, null);
            String size;
            String added;
            if (cursor != null && cursor.moveToFirst()) {
                long s = cursor.getLong(cursor.getColumnIndexOrThrow(MediaContentProvider.SIZE));
                long a = cursor.getLong(cursor.getColumnIndexOrThrow(MediaContentProvider.DATE_ADDED));
                size = Long.toString(s);
                added = Long.toString(a);
            } else {
                Log.error(TAG_LOG, "Cannot compute fingerprint because the item cannot be found");
                size = "";
                added = "";
            }
            StringBuffer fp = new StringBuffer();
            fp.append(added).append("-").append(size);
            String ret = fp.toString();
            if (Log.isLoggable(Log.TRACE)) {
                Log.trace(TAG_LOG, "Fingerprint is: " + ret);
            }
            return ret;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean hasChanges() {
        boolean result = false;
        begin(SyncSource.INCREMENTAL_SYNC, false);
        result |= getNewItemsCount() > 0;
        result |= getUpdatedItemsCount() > 0;
        result |= getDeletedItemsCount() > 0;
        end();
        return result;
    }

    public void setItemsSorter(ItemsSorter sorter) {
        itemsSorter = sorter;
    }

    @Override
    public Enumeration getNewItems() throws TrackerException {
        Enumeration result = super.getNewItems();
        if(itemsSorter != null) {
            if(Log.isLoggable(Log.DEBUG)) {
                Log.debug(TAG_LOG, "Sorting new items keys");
            }
            result = itemsSorter.sort(result, newItems.size());
        }
        return result;
    }
    
    protected abstract Uri getProviderUri();
}


