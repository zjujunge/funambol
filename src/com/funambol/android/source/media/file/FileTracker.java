/*
 * Funambol is a mobile platform developed by Funambol, Inc.
 * Copyright (C) 2008 Funambol, Inc.
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

import com.funambol.android.source.AndroidChangesTracker;
import com.funambol.platform.FileAdapter;
import com.funambol.sapisync.source.CacheTrackerWithRenames;
import com.funambol.sapisync.source.FileSyncSource.ItemsSorter;
import com.funambol.sync.SyncItem;
import com.funambol.storage.StringKeyValueStore;
import com.funambol.sync.SyncSource;
import com.funambol.sync.client.TrackerException;
import com.funambol.util.Log;
import java.util.Enumeration;
import java.util.Hashtable;

public class FileTracker extends CacheTrackerWithRenames
        implements AndroidChangesTracker {

    private static final String TAG_LOG = "FileTracker";

    private ItemsSorter itemsSorter = null;

    public FileTracker(String sourceName, StringKeyValueStore keyStore) {
        super(sourceName, keyStore);
    }

    @Override
    protected String computeFingerprint(SyncItem item) {
        if (Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG_LOG, "computeFingerprint");
        }
        long size = 0;
        long modified = 0;

        String fileName = item.getKey();
        FileAdapter file = null;
        try {
            file = new FileAdapter(fileName);
            size = file.getSize();
            modified = file.lastModified();
            StringBuffer fp = new StringBuffer();
            fp.append(modified).append("-").append(size);
            String ret = fp.toString();
            if (Log.isLoggable(Log.TRACE)) {
                Log.trace(TAG_LOG, "Fingerprint is: " + ret);
            }
            return ret;
        } catch(Exception ex) {
            Log.error(TAG_LOG, "Cannot compute fingerprint of: " + fileName, ex);
            return "";
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {
                }
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

    @Override
    public void begin(int syncMode, boolean reset) throws TrackerException {
        if(itemsSorter != null) {
            itemsSorter.setItemsMetadata(new Hashtable());
        }
        super.begin(syncMode, reset);
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
}


