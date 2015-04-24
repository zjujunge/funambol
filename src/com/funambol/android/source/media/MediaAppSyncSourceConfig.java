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

import java.io.File;

import android.os.Environment;
import android.content.Context;

import com.funambol.client.source.AppSyncSourceConfig;
import com.funambol.client.customization.Customization;
import com.funambol.client.configuration.Configuration;
import com.funambol.platform.DeviceInfo;
import com.funambol.sapisync.source.FileSyncSource;
import com.funambol.sync.SourceConfig;
import com.funambol.sync.SyncSource;

import com.funambol.util.Log;

public class MediaAppSyncSourceConfig extends AppSyncSourceConfig {

    private static final String TAG_LOG = "MediaAppSyncSourceConfig";

    protected static final String CONF_KEY_INCLUDE_OLDER_MEDIA = "CONF_KEY_INCLUDE_OLDER_MEDIA";
    protected static final String CONF_KEY_BUCKET_ID = "CONF_KEY_BUCKET_ID";

    private final String[] MEDIA_DIRECTORY_CANDIDATES = {"Camera", "100MEDIA", "100ANDRO"};

    protected boolean includeOlderMedia    = false;

    protected String bucketId;

    protected final MediaAppSyncSource mediaAppSyncSource;

    protected Context context;


    public MediaAppSyncSourceConfig(Context context, MediaAppSyncSource appSource,
                                    Customization customization, Configuration configuration)
    {
        super(appSource, customization, configuration);
        this.mediaAppSyncSource = appSource;
        this.context = context;
    }

    @Override
    public void save() {
        int sourceId = appSource.getId();
        // Save the custom fields
        StringBuffer key;

        key = new StringBuffer();
        key.append(CONF_KEY_INCLUDE_OLDER_MEDIA).append("-").append(sourceId);
        configuration.saveBooleanKey(key.toString(), includeOlderMedia);

        key = new StringBuffer();
        key.append(CONF_KEY_BUCKET_ID).append("-").append(sourceId);
        configuration.saveStringKey(key.toString(), bucketId);
        // Now save the basic configuration (this must be done after save the
        // basic stuff)
        super.save();
    }

    @Override
    public void load(SourceConfig sourceConfig) {
        int sourceId = appSource.getId();
        StringBuffer key;
        key = new StringBuffer();
        key.append(CONF_KEY_INCLUDE_OLDER_MEDIA).append("-").append(sourceId);
        includeOlderMedia = configuration.loadBooleanKey(key.toString(), includeOlderMedia);

        key = new StringBuffer();
        key.append(CONF_KEY_BUCKET_ID).append("-").append(sourceId);
        bucketId = configuration.loadStringKey(key.toString(), bucketId);
        if (bucketId == null) {
            initBucketId();
        }
        super.load(sourceConfig);
    }

    public void setIncludeOlderMedia(boolean value) {
        includeOlderMedia = value;
        dirty = true;

        SyncSource source = mediaAppSyncSource.getSyncSource();
        //change the value in the children sync source
        if (source != null && source instanceof  FileSyncSource) {
            ((FileSyncSource)source).setOldestItemTimestamp( value
                        ? FileSyncSource.NO_LIMIT_ON_ITEM_AGE
                        : configuration.getFirstRunTimestamp());
        }
    }
    
    public boolean getIncludeOlderMedia() {
        return includeOlderMedia;
    }

    public String getBucketId() {
        return bucketId;
    }

    @Override
    public void migrateConfig(String from, String to, SourceConfig config) {
        super.migrateConfig(from, to, config);
        if (from == null) {
            // In version v9 we added video sync source. We need to query the
            // server for its capabilities
            if (Log.isLoggable(Log.INFO)) {
                Log.info(TAG_LOG, "Migrating config from null to 1");
            }
            configuration.setForceServerCapsRequest(true);
        } else if (VERSION_2.equals(from)) {
            // In version 2 some devices could have stored a wrong bucket id
            // In this version we must update it even if it has been already set
            if (Log.isLoggable(Log.INFO)) {
                Log.info(TAG_LOG, "Media bucket id needs to be re-initialized");
            }
            initBucketId();
        }
    }

    protected void initBucketId() {
        DeviceInfo devInfo = new DeviceInfo(context);
        if (Log.isLoggable(Log.INFO)) {
            Log.info(TAG_LOG, "Initializing bucket id " + devInfo.getManufacturer());
        }
        try {
            // Check what folders are available under /sdcard/DCIM. If there is only
            // one, then that's the media folder. If we find more than one, then we
            // apply some magic to understand what's the right one
            String mediaRoot = Environment.getExternalStorageDirectory().toString() + File.separator
                + "DCIM" + File.separator;
            File f = new File(mediaRoot);
            if (f.exists()) {
                if (Log.isLoggable(Log.INFO)) {
                    Log.info(TAG_LOG, "Looking for media directory under: " + mediaRoot);
                }
                String candidateDirectory = null;
                int maxMediaItemsCount = 0;

                String items[] = f.list();
                if (items != null) {
                    for(int i=0;i<items.length;++i) {
                        String item = items[i];
                        File fileItem = new File(mediaRoot + item);

                        // We ignore hidden directories (whose name start with .)
                        if (!item.startsWith(".") && fileItem.isDirectory()) {
                            if (Log.isLoggable(Log.INFO)) {
                                Log.info(TAG_LOG, "Found directory: " + item);
                            }
                            if(!isCandidateForMediaDirectory(item)) {
                                if (Log.isLoggable(Log.INFO)) {
                                    Log.info(TAG_LOG, "This is not a candidate for media directory");
                                }
                                continue;
                            }
                            String[] mediaItems = fileItem.list();
                            if(mediaItems != null) {
                                if (Log.isLoggable(Log.INFO)) {
                                    Log.info(TAG_LOG, "Directory " + item +
                                            " contains " + mediaItems.length + " items");
                                }
                                if(mediaItems.length > maxMediaItemsCount
                                        || candidateDirectory == null) {
                                    candidateDirectory = item;
                                    maxMediaItemsCount = mediaItems.length;
                                }
                            }
                        }
                    }
                }
                // Update the bucket id with the new media directory
                if(candidateDirectory != null) {
                    bucketId = candidateDirectory;
                }
            }
        } catch(Throwable t) {
            Log.error(TAG_LOG, "Failed to detect media directory", t);
        } finally {
            // If the bucket id has not been set we use the Camera directory as default
            if(bucketId == null) {
                Log.error(TAG_LOG, "Media directory has not been properly "
                        + "detected. Use Camera as default");
                bucketId = "Camera";
            }
        }
        if (Log.isLoggable(Log.INFO)) {
            Log.info(TAG_LOG, "Bucket id: " + bucketId);
        }
    }

    private boolean isCandidateForMediaDirectory(String dir) {
        for(String directory : MEDIA_DIRECTORY_CANDIDATES) {
            if(directory.equalsIgnoreCase(dir)) {
                return true;
            }
        }
        return false;
    }
}
 
