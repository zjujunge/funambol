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

package com.funambol.android.source.media.file;

import java.io.File;

import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.Activity;
import android.content.res.Resources;

import com.funambol.android.activities.settings.AndroidSettingsUISyncSource;
import com.funambol.android.activities.settings.TwoLinesCheckBox;
import com.funambol.androidsync.R;
import com.funambol.client.configuration.Configuration;
import com.funambol.util.Log;
import com.funambol.util.StringUtil;


public class FileSettingsUISyncSource extends AndroidSettingsUISyncSource {

    private static final String TAG = "FileSettingsUISyncSource";

    private TextView fileSizeAlert;
    private TextView lblAlert;
    private boolean syncOlderInitialValue;

    public FileSettingsUISyncSource(Activity activity) {

        super(activity);

        // Converts 12 dip into its equivalent px
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, r.getDisplayMetrics());
        
        fileSizeAlert = new TextView(activity);
        fileSizeAlert.setPadding((int)px, fileSizeAlert.getPaddingTop(), 0,
                fileSizeAlert.getPaddingBottom());
        fileSizeAlert.setTextAppearance(this.getContext(), R.style.funambol_small_text);
        
        lblAlert = new TextView(activity);
        lblAlert.setPadding((int)px, lblAlert.getPaddingTop(), 0,
                lblAlert.getPaddingBottom());
        lblAlert.setTextAppearance(this.getContext(), R.style.funambol_small_text);
    }

    @Override
    public void layout() {
        super.layout();

        //first of all, get the default mediahub folder
        String mediaHubPath = "---";
        if (null != appSyncSource) {
            FileAppSyncSourceConfig config = ((FileAppSyncSourceConfig)appSyncSource.getConfig());
            if (null != config) {
                File file = new File(config.getBaseDirectory());
                mediaHubPath = file.getName();
            } else {
                Log.error(TAG, "config is null " + appSyncSource.getConfig());
            }
        } else {
            Log.error(TAG, "appSyncSource is null");
        }
        String alertMessage = loc.getLanguage("conf_file_path_text")
                .replaceAll("__FOLDER__", mediaHubPath);
        

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        
        lblAlert.setText(alertMessage);
        mainLayout.addView(lblAlert, lp);
        
        fileSizeAlert.setText(loc.getLanguage("description_file"));
        mainLayout.addView(fileSizeAlert, lp);
    }
}
