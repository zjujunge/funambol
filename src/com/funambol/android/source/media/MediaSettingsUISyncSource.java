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

import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.app.Activity;
import android.content.res.Resources;

import com.funambol.android.activities.settings.AndroidSettingsUISyncSource;
import com.funambol.android.activities.settings.TwoLinesCheckBox;
import com.funambol.androidsync.R;
import com.funambol.client.configuration.Configuration;
import com.funambol.sync.SyncSource;
import com.funambol.util.Log;
import com.funambol.util.StringUtil;


public class MediaSettingsUISyncSource extends AndroidSettingsUISyncSource {

    private static final String TAG = "MediaSettingsUISyncSource";

    protected TextView syncDirectionLabelField = null;
    private TwoLinesCheckBox syncOlder;
    private boolean syncOlderInitialValue;

    public MediaSettingsUISyncSource(Activity activity) {

        super(activity);
        
        if (hasSyncDirectionLabel()) {
            // Converts 12 dip into its equivalent px
            Resources r = getResources();
            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, r.getDisplayMetrics());
            
            syncDirectionLabelField = new TextView(activity);
            syncDirectionLabelField.setPadding((int)px, syncDirectionLabelField.getPaddingTop(), 0,
                    syncDirectionLabelField.getPaddingBottom());
            syncDirectionLabelField.setTextAppearance(this.getContext(), R.style.funambol_small_text);
            
            // Register a listener on the spinner
            syncModeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                        int arg2, long arg3) {
                    setSyncDirectionLabel();
                }
                public void onNothingSelected(AdapterView<?> arg0) { 
                    setSyncDirectionLabel();                    
                }               
            });
        }

        syncOlder = new TwoLinesCheckBox(activity);
        syncOlder.setPadding(0, syncOlder.getPaddingTop(), 0,
                syncOlder.getPaddingBottom());
    }

    /**
     * Returns whether this sync source has or not a sync direction label to
     * provide information to the user immediately below the sync direction
     * selector.
     *
     * @return true if the sync direction label has to be displayed
     */
    protected boolean hasSyncDirectionLabel() {
        return false; // Default
    }
    
    /**
     * Changes the text of the sync direction label so that it matches the
     * selection of the sync direction control.
     */
    protected void setSyncDirectionLabel() {

        if (syncDirectionLabelField != null) {

            // By default, this method is unused (no sync direction label).
            // It has to be overridden on the subclasses and filled with the proper
            // content.

            String text = "";
            switch (getSyncMode()) {
                case SyncSource.INCREMENTAL_SYNC:
                    // ...
                    break;
                case SyncSource.INCREMENTAL_UPLOAD:
                    // ...
                    break;
                case SyncSource.INCREMENTAL_DOWNLOAD:
                    // ...
                    break;
                default:
                    // ...
            }
            syncDirectionLabelField.setText(text);
        }
    }
    
    @Override
    public void setSyncMode(int mode) {
        super.setSyncMode(mode);
        if (hasSyncDirectionLabel()) {
            setSyncDirectionLabel();
        }
    }

    @Override
    public void saveSettings(Configuration configuration) {
        if (Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG, "Saving custom settings for media source");
        }
        super.saveSettings(configuration);
        MediaAppSyncSourceConfig config = (MediaAppSyncSourceConfig)appSyncSource.getConfig();
        config.setIncludeOlderMedia(syncOlder.isChecked());
    }

    @Override
    public void loadSettings(Configuration configuration) {
        if (Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG, "Loading custom settings for media source");
        }
        super.loadSettings(configuration);
        MediaAppSyncSourceConfig config = (MediaAppSyncSourceConfig)appSyncSource.getConfig();
        boolean checked = config.getIncludeOlderMedia();
        syncOlder.setChecked(checked);
        syncOlderInitialValue = checked;
    }

    @Override
    public boolean hasChanges() {
        boolean changes = super.hasChanges();
        changes |= (syncOlderInitialValue != syncOlder.isChecked());
        return changes;
    }

    @Override
    public void layout() {
        super.layout();
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

        if (hasSyncDirectionLabel()) {
            setSyncDirectionLabel();
            mainLayout.addView(syncDirectionLabelField, lp);
        }
        
        String sourceNameLC = appSyncSource.getName().toLowerCase();

        String uploadOlder = StringUtil.replaceAll(
            loc.getLanguage("conf_upload_older_media"),
            "__source__", sourceNameLC);
        String uploadOlderWarning = StringUtil.replaceAll(
            loc.getLanguage("conf_upload_older_media_warning"),
            "__source__", sourceNameLC);

        syncOlder.setText1(uploadOlder);
        syncOlder.setText2(uploadOlderWarning);
        

        mainLayout.addView(syncOlder, lp);
    }
}
