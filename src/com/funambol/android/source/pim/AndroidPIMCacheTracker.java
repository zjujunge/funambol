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

package com.funambol.android.source.pim;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import android.content.Context;

import com.funambol.android.source.AndroidChangesTracker;
import com.funambol.sync.client.CacheTracker;
import com.funambol.sync.SyncItem;
import com.funambol.storage.StringKeyValueStore;
import com.funambol.syncml.protocol.SyncML;
import com.funambol.util.Base64;
import com.funambol.util.Log;

/**
 * This is a specialization of the CacheTracker which differs mainly in the
 * fingerprint computation. In particular:
 *
 * <ul>
 *   <li> 
 *     item's content in entirely brought into memory, so this tracker is
 *     suitable for PIM sources where items are rather small 
 *   </li>
 *   <li>
 *     MD5 is computed via Java SE MessageDigest which guarantees better
 *     performance compared to the pure Java implementation we have in our APIs
 *   </li>
 * </ul>
 *
 */
public class AndroidPIMCacheTracker extends CacheTracker implements AndroidChangesTracker {

    private static final String TAG_LOG = "AndroidPIMCacheTracker";

    public AndroidPIMCacheTracker(Context context, StringKeyValueStore status) {
        super(status);
    }

    @Override
    protected String computeFingerprint(SyncItem item) {
        if (Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG_LOG, "computeFingerprint");
        }
        InputStream is = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            is = item.getInputStream();
            byte data[] = new byte[is.available()];
            is.read(data);

            if (Log.isLoggable(Log.TRACE)) {
                Log.trace(TAG_LOG, "Item content: " + new String(data));
            }

            md.update(data);
            byte[] md5 = md.digest();

            String res = new String(Base64.encode(md5));

            if (Log.isLoggable(Log.TRACE)) {
                Log.trace(TAG_LOG, "MD5=" + res);
            }
            return res;
        } catch (IOException ioe) {
            Log.error(TAG_LOG, "Cannot read item content", ioe);
            return "";
        } catch (Exception e) {
            Log.error(TAG_LOG, "Cannot compute fingerprint", e);
            return "";
        } finally {
            try {
                // Close the stream
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    public boolean hasChanges() {
        boolean result = false;
        begin(SyncML.ALERT_CODE_FAST, false);
        result |= getNewItemsCount() > 0;
        result |= getUpdatedItemsCount() > 0;
        result |= getDeletedItemsCount() > 0;
        end();
        return result;
    }
}
