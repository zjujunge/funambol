package com.funambol.android.source.media.video;

import android.app.Activity;

import com.funambol.android.source.media.MediaSettingsUISyncSource;
import com.funambol.sync.SyncSource;

public class VideoSettingsUISyncSource extends MediaSettingsUISyncSource {

    public VideoSettingsUISyncSource(Activity activity) {
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
                text = loc.getLanguage("description_two_way_video");
                break;
            case SyncSource.INCREMENTAL_UPLOAD:
                text = loc.getLanguage("description_upload_video");
                break;
            case SyncSource.INCREMENTAL_DOWNLOAD:
                text = loc.getLanguage("description_download_video");
                break;
            default:
                // Nothing
        }
        syncDirectionLabelField.setText(text);
    }

}
