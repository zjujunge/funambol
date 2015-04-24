package com.funambol.android.source.media.picture;

import android.app.Activity;

import com.funambol.android.source.media.MediaSettingsUISyncSource;
import com.funambol.sync.SyncSource;

public class PictureSettingsUISyncSource extends MediaSettingsUISyncSource {

    public PictureSettingsUISyncSource(Activity activity) {
        super(activity);
    }
    
    @Override
    protected boolean hasSyncDirectionLabel() {
         return true;
    }
    
    @Override
    protected void setSyncDirectionLabel() {
        String text = "";
        switch (getSyncMode()) {
            case SyncSource.INCREMENTAL_SYNC:
                text = loc.getLanguage("description_two_way_picture");
                break;
            case SyncSource.INCREMENTAL_UPLOAD:
                text = loc.getLanguage("description_upload_picture");
                break;
            case SyncSource.INCREMENTAL_DOWNLOAD:
                text = loc.getLanguage("description_download_picture");
                break;
            default:
                // Nothing
        }
        syncDirectionLabelField.setText(text);
    }

}
