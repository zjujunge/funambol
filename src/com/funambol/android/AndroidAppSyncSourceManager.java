/*
 * Funambol is a mobile platform developed by Funambol, Inc.
 * Copyright (C) 2009 Funambol, Inc.
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

package com.funambol.android;

import java.util.Vector;
import java.io.IOException;

import android.content.Context;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.MediaStore.Images.Media;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Environment;

import com.funambol.android.providers.FilesContentProvider;
import com.funambol.android.source.AbstractDataManager;
import com.funambol.android.source.pim.AndroidPIMCacheTracker;
import com.funambol.android.source.pim.contact.ContactSyncSource;
import com.funambol.android.source.pim.contact.ContactExternalAppManager;
import com.funambol.android.source.pim.contact.ContactAppSyncSourceConfig;
import com.funambol.android.source.pim.contact.ContactAppSyncSource;
import com.funambol.android.source.pim.contact.ContactSettingsUISyncSource;
import com.funambol.android.source.pim.calendar.CalendarSyncSource;
import com.funambol.android.source.pim.calendar.EventSyncSource;
import com.funambol.android.source.pim.calendar.CalendarManager;
import com.funambol.android.source.pim.calendar.CalendarAppSyncSource;
import com.funambol.android.source.pim.calendar.CalendarChangesTracker;
import com.funambol.android.source.pim.calendar.CalendarChangesTrackerMD5;
import com.funambol.android.source.pim.calendar.CalendarExternalAppManager;
import com.funambol.android.source.pim.calendar.CalendarAppSyncSourceConfig;
import com.funambol.android.source.pim.task.AstridTaskManager;
import com.funambol.android.source.pim.note.NoteSyncSource;
import com.funambol.android.source.pim.note.OINoteManager;
import com.funambol.android.source.media.MediaAppSyncSource;
import com.funambol.android.source.media.picture.PictureAppSyncSourceConfig;
import com.funambol.android.source.media.MediaExternalAppManager;
import com.funambol.android.source.media.file.FileAppSyncSourceConfig;
import com.funambol.android.source.media.file.FileTracker;
import com.funambol.android.source.media.picture.PictureSyncSource;
import com.funambol.android.source.media.picture.PictureTracker;
import com.funambol.android.source.media.video.VideoAppSyncSourceConfig;
import com.funambol.android.source.media.video.VideoSyncSource;
import com.funambol.android.source.pim.contact.ContactManager;
import com.funambol.android.source.pim.contact.DirtyChangesTracker;
import com.funambol.android.source.pim.contact.FunambolContactManager;
import com.funambol.android.source.media.file.AndroidFileSyncSource;
import com.funambol.android.services.AutoSyncServiceHandler;
import com.funambol.client.controller.SynchronizationController;
import com.funambol.client.customization.Customization;
import com.funambol.client.source.AppSyncSource;
import com.funambol.client.source.AppSyncSourceConfig;
import com.funambol.client.source.AppSyncSourceManager;
import com.funambol.client.localization.Localization;
import com.funambol.platform.DeviceInfo;
import com.funambol.platform.DeviceInfoInterface;
import com.funambol.platform.FileAdapter;
import com.funambol.storage.StringKeyValueMemoryStore;
import com.funambol.sync.SyncSource;
import com.funambol.sync.SourceConfig;
import com.funambol.sync.client.ConfigSyncSource;
import com.funambol.sync.client.CacheTracker;
import com.funambol.sync.client.ChangesTracker;
import com.funambol.syncml.spds.SyncMLSourceConfig;
import com.funambol.syncml.spds.SyncMLAnchor;
import com.funambol.sapisync.SapiSyncAnchor;
import com.funambol.sapisync.source.FileSyncSource;
import com.funambol.sapisync.source.util.MediaItemsSorter;
import com.funambol.storage.StringKeyValueSQLiteStore;
import com.funambol.sync.Filter;
import com.funambol.sync.SyncFilter;
import com.funambol.syncml.protocol.CTCap;
import com.funambol.syncml.protocol.CTInfo;
import com.funambol.syncml.protocol.DataStore;
import com.funambol.syncml.protocol.SourceRef;
import com.funambol.syncml.protocol.SyncCap;
import com.funambol.syncml.protocol.SyncType;
import com.funambol.util.Log;

/**
 * A manager for the AndroidAppSyncSource instances in use by the Android client
 * The access to this class can be made invoking the static method getInstance()
 * as the pattern was realized as Singleton. Once the instance is no more used
 * by the caller it is suitable to call the dispose() method in order to release
 * this class' resources as soon as possible.
 */
public class AndroidAppSyncSourceManager extends AppSyncSourceManager {

    private static final String TAG_LOG = "AndroidAppSyncSourceManager";

    private static AndroidAppSyncSourceManager instance = null;

    private Localization localization;
    private Context context;
    private DeviceInfoInterface deviceInfo;

    private AutoSyncServiceHandler autoSyncService;


    /**
     * The private constructor that enforce the Singleton implementation
     * @param customization the Customization object to be referred to
     * @param localization the Localization pattern for this manager
     * @param context the Context object to be related to the manager
     */
    private AndroidAppSyncSourceManager(Customization customization,
                                        Localization localization,
                                        Context context) {
        super(customization);
        this.localization = localization;
        this.context = context;
        this.deviceInfo = new DeviceInfo(context);
        this.autoSyncService = new AutoSyncServiceHandler(context);
    }

    /**
     * Single instance call
     * @param customization the Customization object to be referred to
     * @param localization the Localization pattern for this manager
     * @param context the Context object to be related to the manager
     * @return the single instance of this class, creating it if it has never
     * been referenced
     */
    public static AndroidAppSyncSourceManager getInstance(Customization customization,
                                                          Localization localization,
                                                          Context context) {
        if (instance == null) {
            instance = new AndroidAppSyncSourceManager(customization, localization, context);
        }
        return instance;
    }

    /**
     * Dispose the single instance referencing it with the null object
     */
    public static void dispose() {
        instance = null;
    }

    /**
     * Setup the SyncSource identified by its sourceId index
     * @param sourceId the int that identifies the source
     * @param configuration the AndroidConfiguration object used to setup the
     * source
     * @return AppSyncSource the instance of the setup AppSyncSource
     * @throws Exception
     */
    public AppSyncSource setupSource(int sourceId, AndroidConfiguration configuration) throws Exception {
        if (Log.isLoggable(Log.INFO)) {
            Log.info(TAG_LOG, "Setting up source: " + sourceId);
        }
        AppSyncSource appSource;
        switch(sourceId) {
            case CONTACTS_ID:
            {
                appSource = setupContactsSource(configuration);
                break;
            }
            case EVENTS_ID:
            {
                appSource = setupEventsSource(configuration);
                break;
            }
            case TASKS_ID:
            {
                appSource = setupTasksSource(configuration);
                break;
            }
            case NOTES_ID:
            {
                appSource = setupNotesSource(configuration);
                break;
            }
            case CONFIG_ID:
            {
                appSource = setupConfigSource(configuration);
                break;
            }
            case PICTURES_ID:
            {
                appSource = setupPicturesSource(configuration);
                break;
            }
            case VIDEOS_ID:
            {
                appSource = setupVideosSource(configuration);
                break;
            }
            case FILES_ID:
            {
                appSource = setupFilesSource(configuration);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown source: " + sourceId);
        }
        return appSource;
    }

    /**
     * Setup the source for contacts
     * @param configuration the AndroidConfiguration to be used to setup the
     * source
     * @return AppSyncSource related to contacts
     * @throws Exception
     */
    protected AppSyncSource setupContactsSource(AndroidConfiguration configuration) throws Exception {

        int id = CONTACTS_ID;
        String name = localization.getLanguage("type_contacts");

        ContactAppSyncSource appSyncSource = new ContactAppSyncSource(context, name);
        appSyncSource.setId(id);

        // On the 2.0.x simulator there is no support for native sync, so in
        // this case we revert to sync service method
        appSyncSource.setSyncMethod(AndroidAppSyncSource.SERVICE);
        appSyncSource.setAuthority(AndroidAppSyncSource.AUTHORITY_CONTACTS);
        appSyncSource.setProviderUri(ContactsContract.Contacts.CONTENT_URI);

        // Create the proper settings component for this source
        Class basicSettings = Class.forName("com.funambol.android.source.pim.contact.ContactSettingsUISyncSource");
        appSyncSource.setSettingsUIClass(basicSettings);

        // Create the dev settings for this source
        Class devSettings = Class.forName("com.funambol.android.activities.settings.AndroidDevSettingsUISyncSource");
        appSyncSource.setDevSettingsUIClass(devSettings);

        Class buttonView = Class.forName("com.funambol.android.activities.AndroidButtonUISyncSource");
        appSyncSource.setButtonUIClass(buttonView);

        Class aloneView = Class.forName(((AndroidCustomization)customization)
                .getAloneUISyncSourceClassName());
        appSyncSource.setAloneUIClass(aloneView);
        
        int order = getSourcePosition(id);
        appSyncSource.setUiSourceIndex(order);

        appSyncSource.setHasSetting(AppSyncSource.SYNC_MODE_SETTING,
                customization.isSyncDirectionVisible(),
                customization.getDefaultSourceSyncModes(id, deviceInfo.getDeviceRole()));
        appSyncSource.setHasSetting(
                ContactSettingsUISyncSource.DEFAULT_ADDRESS_BOOK_SETTING,
                true, "");

        appSyncSource.setBandwidthSaverUse(customization.useBandwidthSaverContacts());

        // Create the contact manager
        ContactManager cm = new FunambolContactManager(context);

        SourceConfig sc = null;
        if (SourceConfig.VCARD_TYPE.equals(customization.getContactType())) {
            // This is vcard format
            String defaultUri = customization.getDefaultSourceUri(id);
            sc = new SyncMLSourceConfig("contacts", SourceConfig.VCARD_TYPE, defaultUri, 
                                        createDataStore("contacts", SourceConfig.VCARD_TYPE, "2.1", cm));
            sc.setEncoding(SyncSource.ENCODING_NONE);
            sc.setSyncMode(customization.getDefaultSourceSyncMode(id, deviceInfo.getDeviceRole()));
            // Set this item anchor
            SyncMLAnchor anchor = new SyncMLAnchor();
            sc.setSyncAnchor(anchor);
        }

        if (sc != null) {
            // Load the source config from the configuration
            ContactAppSyncSourceConfig asc = new ContactAppSyncSourceConfig(
                    appSyncSource, customization, configuration);
            asc.load(sc);
            appSyncSource.setConfig(asc);

            if(!asc.getUseDirtyChangesTracker()) {
                // Migrate tracker store only if the user has already synced
                SyncMLAnchor anchor = (SyncMLAnchor)sc.getSyncAnchor();
                if(anchor.getLast() != 0) {
                    try {
                        ContactSyncSource.migrateToDirtyChangesTracker(sc, cm, context);
                    } catch(Throwable t) {
                        Log.error(TAG_LOG, "Failed to migrate changes tracker store", t);
                    }
                }
                asc.setUseDirtyChangesTracker(true);
                asc.save();
            }

            ChangesTracker tracker = new DirtyChangesTracker(context, cm);

            ContactSyncSource src = new ContactSyncSource(sc, tracker, context,
                    configuration, appSyncSource, cm);
            appSyncSource.setSyncSource(src);

            // Setup the external app manager
            ContactExternalAppManager appManager =
                    new ContactExternalAppManager(context, appSyncSource);
            appSyncSource.setAppManager(appManager);

            // Inform the auto sync service that we shall monitor contacts for
            // changes
            autoSyncService.startMonitoringUri(appSyncSource.getProviderUri().toString(), appSyncSource.getId());
        } else {
            Log.error(TAG_LOG, "The contact sync source does not support the type: " +
                    customization.getContactType());
            Log.error(TAG_LOG, "Contact source will be disabled as not working");
        }

        
        return appSyncSource;
    }

    /**
     * Setup the source for tasks
     * @param configuration the AndroidConfiguration to be used to setup the
     * source
     * @return AppSyncSource related to events
     * @throws Exception
     */
    protected AppSyncSource setupTasksSource(AndroidConfiguration configuration) throws Exception {

        int id = TASKS_ID;
        String name = localization.getLanguage("type_tasks");

        AndroidAppSyncSource appSyncSource = new AndroidAppSyncSource(name);
        appSyncSource.setId(id);

        appSyncSource.setSyncMethod(AndroidAppSyncSource.SERVICE);
        appSyncSource.setAuthority(AstridTaskManager.AUTHORITY);
        appSyncSource.setProviderUri(AstridTaskManager.Tasks.CONTENT_URI);

        // Create the proper settings component for this source
        Class calendarSettings = Class.forName("com.funambol.android.source.pim.calendar.CalendarSettingsUISyncSource");
        appSyncSource.setSettingsUIClass(calendarSettings);

        // Create the dev settings for this source
        Class devSettings = Class.forName("com.funambol.android.activities.settings.AndroidDevSettingsUISyncSource");
        appSyncSource.setDevSettingsUIClass(devSettings);

        Class buttonView = Class.forName("com.funambol.android.activities.AndroidButtonUISyncSource");
        appSyncSource.setButtonUIClass(buttonView);

        Class aloneView = Class.forName("com.funambol.android.activities.AndroidAloneUISyncSource");
        appSyncSource.setAloneUIClass(aloneView);

        int order = getSourcePosition(id);
        appSyncSource.setUiSourceIndex(order);

        appSyncSource.setHasSetting(AppSyncSource.SYNC_MODE_SETTING,
                customization.isSyncDirectionVisible(),
                customization.getDefaultSourceSyncModes(id, deviceInfo.getDeviceRole()));

        AstridTaskManager dm = new AstridTaskManager(context, appSyncSource);
        SourceConfig sc = null;
        String defaultUri = customization.getDefaultSourceUri(id);
        sc = new SyncMLSourceConfig("task", customization.getCalendarType(), defaultUri,
                                    createDataStore("task", customization.getCalendarType(), "1.0", dm));
        sc.setEncoding(SyncSource.ENCODING_NONE);
        sc.setSyncMode(customization.getDefaultSourceSyncMode(id, deviceInfo.getDeviceRole()));

        // Set this item anchor
        SyncMLAnchor anchor = new SyncMLAnchor();
        sc.setSyncAnchor(anchor);

        AppSyncSourceConfig asc = new AppSyncSourceConfig(appSyncSource, customization, configuration);
        asc.load(sc);

        appSyncSource.setConfig(asc);

        // We need a third party app to sync task. At the moment we only support
        // Astrid, so we check for its availability and version
        PackageManager pm = context.getPackageManager();
        try {
            if (Log.isLoggable(Log.DEBUG)) {
                Log.debug(TAG_LOG, "Checking if Astrid is available");
            }
            ProviderInfo info = pm.resolveContentProvider("com.todoroo.astrid", 0);
            if (info != null) {
                if (Log.isLoggable(Log.INFO)) {
                    Log.info(TAG_LOG, "Astrid provider found, enable task source");
                }
            } else {
                if (Log.isLoggable(Log.INFO)) {
                    Log.info(TAG_LOG, "Astrid provider not found, disable task source");
                }
                asc.setActive(false);
                return appSyncSource;
            }
        } catch (Exception e) {
            Log.error(TAG_LOG, "Error detecting Astrid", e);
            asc.setActive(false);
            return appSyncSource;
        }

        // Create the sync source
        IntKeyValueSQLiteStore trackerStore =
            new IntKeyValueSQLiteStore(context, 
            ((AndroidCustomization)customization).getFunambolSQLiteDbName(),
            sc.getName());

        // Since we sync calendars that our app does not owe, we cannot use sync
        // fields and calendars do not have revisions, so we are forced to use a
        // CacheTracker here (based on MD5)
        AndroidPIMCacheTracker tracker = new AndroidPIMCacheTracker(context, trackerStore);

        CalendarSyncSource src = new CalendarSyncSource(sc, tracker, context, configuration, appSyncSource, dm);
        appSyncSource.setSyncSource(src);

        // Setup the external app manager
        //CalendarExternalAppManager appManager = new CalendarExternalAppManager(context, appSyncSource);
        //appSyncSource.setAppManager(appManager);

        // Inform the auto sync service that we shall monitor contacts for
        // changes
        autoSyncService.startMonitoringUri(appSyncSource.getProviderUri().toString(), appSyncSource.getId());

        return appSyncSource;
    }

    /**
     * Setup the source for notes
     * @param configuration the AndroidConfiguration to be used to setup the
     * source
     * @return AppSyncSource related to events
     * @throws Exception
     */
    protected AppSyncSource setupNotesSource(AndroidConfiguration configuration) throws Exception {

        int id = NOTES_ID;
        String name = localization.getLanguage("type_notes");

        AndroidAppSyncSource appSyncSource = new AndroidAppSyncSource(name);
        appSyncSource.setId(id);

        appSyncSource.setSyncMethod(AndroidAppSyncSource.SERVICE);
        appSyncSource.setAuthority(OINoteManager.AUTHORITY);
        appSyncSource.setProviderUri(OINoteManager.Notes.CONTENT_URI);

        // Create the proper settings component for this source
        Class calendarSettings = Class.forName("com.funambol.android.activities.settings.AndroidSettingsUISyncSource");
        appSyncSource.setSettingsUIClass(calendarSettings);

        // Create the dev settings for this source
        Class devSettings = Class.forName("com.funambol.android.activities.settings.AndroidDevSettingsUISyncSource");
        appSyncSource.setDevSettingsUIClass(devSettings);

        Class buttonView = Class.forName("com.funambol.android.activities.AndroidButtonUISyncSource");
        appSyncSource.setButtonUIClass(buttonView);

        Class aloneView = Class.forName("com.funambol.android.activities.AndroidAloneUISyncSource");
        appSyncSource.setAloneUIClass(aloneView);

        int order = getSourcePosition(id);
        appSyncSource.setUiSourceIndex(order);

        appSyncSource.setHasSetting(AppSyncSource.SYNC_MODE_SETTING,
                customization.isSyncDirectionVisible(),
                customization.getDefaultSourceSyncModes(id, deviceInfo.getDeviceRole()));

        OINoteManager dm = new OINoteManager(context, appSyncSource);
        SourceConfig sc = null;
        String defaultUri = customization.getDefaultSourceUri(id);
        sc = new SyncMLSourceConfig("note", customization.getNoteType(), defaultUri,
                createDataStore("note", customization.getNoteType(), "1.0", dm));
        sc.setEncoding(SyncSource.ENCODING_NONE);
        sc.setSyncMode(customization.getDefaultSourceSyncMode(id, deviceInfo.getDeviceRole()));

        SyncMLAnchor anchor = new SyncMLAnchor();
        sc.setSyncAnchor(anchor);

        AppSyncSourceConfig asc = new AppSyncSourceConfig(appSyncSource, customization, configuration);
        asc.load(sc);

        appSyncSource.setConfig(asc);

        // We need a third party app to sync task. At the moment we only support
        // Astrid, so we check for its availability and version
        PackageManager pm = context.getPackageManager();
        try {
            if (Log.isLoggable(Log.DEBUG)) {
                Log.debug(TAG_LOG, "Checking if Open Intent Notepad is available");
            }
            ProviderInfo info = pm.resolveContentProvider("org.openintents.notepad", 0);
            if (info != null) {
                if (Log.isLoggable(Log.INFO)) {
                    Log.info(TAG_LOG, "Open Intent Notepad provider found, enable note source");
                }
            } else {
                if (Log.isLoggable(Log.INFO)) {
                    Log.info(TAG_LOG, "Open Intent Notepad provider not found, disable note source");
                }
                asc.setActive(false);
                return appSyncSource;
            }
        } catch (Exception e) {
            Log.error(TAG_LOG, "Error detecting Open Intent Notepad", e);
            asc.setActive(false);
            return appSyncSource;
        }

        // Create the sync source
        IntKeyValueSQLiteStore trackerStore =
            new IntKeyValueSQLiteStore(context, 
            ((AndroidCustomization)customization).getFunambolSQLiteDbName(),
            sc.getName());

        AndroidPIMCacheTracker tracker = new AndroidPIMCacheTracker(context, trackerStore);

        NoteSyncSource src = new NoteSyncSource(sc, tracker, context, configuration, appSyncSource, dm);
        appSyncSource.setSyncSource(src);

        // Inform the auto sync service that we shall monitor contacts for
        // changes
        autoSyncService.startMonitoringUri(appSyncSource.getProviderUri().toString(), appSyncSource.getId());

        // Setup the external app manager
        //CalendarExternalAppManager appManager = new CalendarExternalAppManager(context, appSyncSource);
        //appSyncSource.setAppManager(appManager);

        return appSyncSource;
    }

    /**
     * Setup the source for events
     * @param configuration the AndroidConfiguration to be used to setup the
     * source
     * @return AppSyncSource related to events
     * @throws Exception
     */
    protected AppSyncSource setupEventsSource(AndroidConfiguration configuration) throws Exception {

        int id = EVENTS_ID;
        String name = localization.getLanguage("type_calendar");

        CalendarAppSyncSource appSyncSource = new CalendarAppSyncSource(context, name);
        appSyncSource.setId(id);

        appSyncSource.setSyncMethod(AndroidAppSyncSource.SERVICE);
        appSyncSource.setAuthority(CalendarManager.getCalendarAuthority());
        appSyncSource.setProviderUri(CalendarManager.Events.CONTENT_URI);

        // Create the proper settings component for this source
        Class calendarSettings = Class.forName("com.funambol.android.source.pim.calendar.CalendarSettingsUISyncSource");
        appSyncSource.setSettingsUIClass(calendarSettings);

        // Create the dev settings for this source
        Class devSettings = Class.forName("com.funambol.android.activities.settings.AndroidDevSettingsUISyncSource");
        appSyncSource.setDevSettingsUIClass(devSettings);

        Class buttonView = Class.forName("com.funambol.android.activities.AndroidButtonUISyncSource");
        appSyncSource.setButtonUIClass(buttonView);

        Class aloneView = Class.forName(((AndroidCustomization)customization)
                .getAloneUISyncSourceClassName());
        appSyncSource.setAloneUIClass(aloneView);

        int order = getSourcePosition(id);
        appSyncSource.setUiSourceIndex(order);

        appSyncSource.setHasSetting(AppSyncSource.SYNC_MODE_SETTING,
                customization.isSyncDirectionVisible(),
                customization.getDefaultSourceSyncModes(id, deviceInfo.getDeviceRole()));

        CalendarManager dm = new CalendarManager(context, appSyncSource);
        SourceConfig sc = null;
        String defaultUri = customization.getDefaultSourceUri(id);
        sc = new SyncMLSourceConfig("calendar", customization.getCalendarType(), defaultUri,
                                    createDataStore("calendar", customization.getCalendarType(), "1.0", dm));
        sc.setEncoding(SyncSource.ENCODING_NONE);
        sc.setSyncMode(customization.getDefaultSourceSyncMode(id, deviceInfo.getDeviceRole()));

        // Set this item anchor
        SyncMLAnchor anchor = new SyncMLAnchor();
        sc.setSyncAnchor(anchor);

        CalendarAppSyncSourceConfig asc = new CalendarAppSyncSourceConfig(
                appSyncSource, customization, configuration);
        asc.load(sc);

        appSyncSource.setConfig(asc);

        appSyncSource.setBandwidthSaverUse(customization.useBandwidthSaverEvents());

        // Create the sync source
        IntKeyValueSQLiteStore trackerStore =
            new IntKeyValueSQLiteStore(context, 
            ((AndroidCustomization)customization).getFunambolSQLiteDbName(),
            sc.getName());

        // There are two different trackers depending on the OS version, because
        // OS < 2.2 does not allow the sync_dirty to be handled properly
        CalendarChangesTracker tracker;
        if (AndroidUtils.isSimulator(context) || Build.VERSION.SDK_INT >= 8) {
            tracker = new CalendarChangesTracker(context, trackerStore, asc);
        } else {
            tracker = new CalendarChangesTrackerMD5(context, trackerStore, asc);
        }

        EventSyncSource src = new EventSyncSource(sc, tracker, context, configuration, appSyncSource, dm);
        appSyncSource.setSyncSource(src);

        // Setup the external app manager
        CalendarExternalAppManager appManager = new CalendarExternalAppManager(context, appSyncSource);
        appSyncSource.setAppManager(appManager);

        // If the user is already logged in and there is no valid calendar to
        // sync, then we better recreate it here
        if (!configuration.getCredentialsCheckPending()) {
            boolean recreateCalendar = false;
            // If the calendar id is undefined, or no longer exists, then we
            // recreate the funambol calendar
            long calendarId = asc.getCalendarId();
            if (calendarId == -1) {
                recreateCalendar = true;
            } else {
                if (!dm.exists(""+calendarId)) {
                    recreateCalendar = true;
                }
            }

            if (recreateCalendar) {
                Log.info(TAG_LOG, "Creating account calendar because it does not exist");
                try {
                    calendarId = appSyncSource.createCalendar();
                    Log.info(TAG_LOG, "Calendar created with id " + calendarId);
                    asc.setCalendarId(calendarId);
                    asc.save();
                } catch (Exception e) {
                    Log.error(TAG_LOG, "Cannot create account calendar", e);
                }
            }
        }

        // Inform the auto sync service that we shall monitor contacts for
        // changes
        autoSyncService.startMonitoringUri(CalendarManager.Events.CONTENT_URI.toString(), appSyncSource.getId());
        // Add the reminder table URI to the list of monitored uris
        autoSyncService.startMonitoringUri(CalendarManager.Reminders.CONTENT_URI.toString(), appSyncSource.getId());

        return appSyncSource;
    }

    /**
     * Setup the source for pictures
     * @param configuration the AndroidConfiguration to be used to setup the
     * source
     * @return AppSyncSource related to pictures
     * @throws Exception
     */
    protected AppSyncSource setupPicturesSource(AndroidConfiguration configuration) throws Exception {

        int id = PICTURES_ID;
        String name = localization.getLanguage("type_photos");
        // These are the supported extensions
        String extensions[] = { "jpg", "jpeg", "gif", "png" };

        MediaAppSyncSource appSyncSource = new MediaAppSyncSource(name);
        appSyncSource.setId(id);
        appSyncSource.setSyncMethod(AndroidAppSyncSource.SYNC_ADAPTER);
        appSyncSource.setAuthority(AndroidAppSyncSource.AUTHORITY_MEDIA);
        appSyncSource.setAuthorityType(AndroidAppSyncSource.AUTHORITY_MEDIA_TYPE_PICTURES);
        
        appSyncSource.setIsRefreshSupported(SynchronizationController.REFRESH_FROM_SERVER, false);
        appSyncSource.setIsRefreshSupported(SynchronizationController.REFRESH_TO_SERVER, false);
        appSyncSource.setUiSourceIndex(getSourcePosition(id));
        appSyncSource.setProviderUri(Media.EXTERNAL_CONTENT_URI);

        appSyncSource.setBandwidthSaverUse(customization.useBandwidthSaverMedia());

        // Create the proper settings component for this source
        Class pictureSettings = Class.forName("com.funambol.android.source.media.picture.PictureSettingsUISyncSource");
        appSyncSource.setSettingsUIClass(pictureSettings);

        // No dev settings for this source
        appSyncSource.setDevSettingsUIClass(null);

        Class buttonView = Class.forName("com.funambol.android.activities.AndroidButtonUISyncSource");
        appSyncSource.setButtonUIClass(buttonView);

        Class aloneView = Class.forName(((AndroidCustomization)customization)
                .getAloneUISyncSourceClassName());
        appSyncSource.setAloneUIClass(aloneView);

        appSyncSource.setHasSetting(AppSyncSource.SYNC_MODE_SETTING,
                customization.isSyncDirectionVisible(),
                customization.getDefaultSourceSyncModes(id, deviceInfo.getDeviceRole()));
        appSyncSource.setHasSetting(AppSyncSource.SYNC_FOLDER_SETTING, true,
                localization.getLanguage("sync_pictures_folder_label"));

        SourceConfig sc = null;
        String defaultUri = customization.getDefaultSourceUri(id);
        sc = new SourceConfig("pictures", SourceConfig.FILE_OBJECT_TYPE, defaultUri);
        sc.setEncoding(SyncSource.ENCODING_NONE);
        sc.setSyncMode(customization.getDefaultSourceSyncMode(id, deviceInfo.getDeviceRole()));

        // Set the sync anchor to sync via media engine
        SapiSyncAnchor anchor = new SapiSyncAnchor();
        sc.setSyncAnchor(anchor);

        PictureAppSyncSourceConfig asc = new PictureAppSyncSourceConfig(
                context, appSyncSource, customization, configuration);
        asc.load(sc);
        asc.setMaxItemSize(FileSyncSource.NO_LIMIT_ON_ITEM_SIZE);
        appSyncSource.setConfig(asc);

        // Create the sync source
        StringKeyValueSQLiteStore trackerStore = PictureTracker
                .createTrackerStore(context, sc, asc, customization);
        FileTracker tracker = new FileTracker(sc.getName(), trackerStore);
        tracker.setItemsSorter(new MediaItemsSorter(true));

        String sdCardDirectory = Environment.getExternalStorageDirectory().toString();
        StringBuffer dataDirectory = new StringBuffer();
        dataDirectory.append(sdCardDirectory);
        dataDirectory.append("/DCIM");
        dataDirectory.append("/").append(asc.getBucketId());

        PictureSyncSource syncSource = new PictureSyncSource(sc, tracker,
                dataDirectory.toString(), sdCardDirectory, appSyncSource,
                configuration, context);
        syncSource.setSupportedExtensions(extensions);

        DeviceInfoInterface.DeviceRole deviceRole = deviceInfo.getDeviceRole();

        // Set up Sync Filter
        SyncFilter filter = new SyncFilter();
        Filter fullUploadFilter = new Filter(Filter.ITEMS_COUNT_TYPE);
        fullUploadFilter.setCount(customization.getFirstSyncMediaUploadLimit(PICTURES_ID, deviceRole));
        
        // Disable the upload filter if the "Upload Older Media" flag is set
        boolean includeOlder = asc.getIncludeOlderMedia();
        fullUploadFilter.setEnabled(!includeOlder);
        
        Filter fullDownloadFilter;
        if(deviceInfo.isTablet()) {
            if(Log.isLoggable(Log.DEBUG)) {
                Log.debug(TAG_LOG, "This is a tablet device. Setting items count "
                        + "filter for first pictures download");
            }
            fullDownloadFilter = new Filter(Filter.ITEMS_COUNT_TYPE);
            int dowloadFilter = customization.getFirstSyncMediaDownloadLimit(PICTURES_ID, deviceRole);
            fullDownloadFilter.setCount(dowloadFilter);
        } else {
            fullDownloadFilter = new Filter(Filter.DATE_RECENT_TYPE);
            fullDownloadFilter.setDate(configuration.getFirstRunTimestamp());
        }
        filter.setFullDownloadFilter(fullDownloadFilter);
        filter.setFullUploadFilter(fullUploadFilter);
        syncSource.setFilter(filter);
        syncSource.setDataTag("pictures");

        syncSource.setAllItemsSorter(new MediaItemsSorter(true));
        
        appSyncSource.setSyncSource(syncSource);

        // Setup the external app manager
        MediaExternalAppManager appManager = new MediaExternalAppManager(context, appSyncSource);
        appSyncSource.setAppManager(appManager);

        // Start monitoring directory
        autoSyncService.startMonitoringDirectory(dataDirectory.toString(), appSyncSource.getId(), extensions);

        return appSyncSource;
    }

    /**
     * Setup the source for videos
     * @param configuration the AndroidConfiguration to be used to setup the
     * source
     * @return AppSyncSource related to pictures
     * @throws Exception
     */
    protected AppSyncSource setupVideosSource(AndroidConfiguration configuration) throws Exception {

        int id = VIDEOS_ID;
        String name = localization.getLanguage("type_videos");
        // These are the extensions supported by this source
        String extensions[] = { "3gp", "mp4", "mpeg", "flv" };

        MediaAppSyncSource appSyncSource = new MediaAppSyncSource(name);
        appSyncSource.setId(id);
        appSyncSource.setSyncMethod(AndroidAppSyncSource.SYNC_ADAPTER);
        appSyncSource.setAuthority(AndroidAppSyncSource.AUTHORITY_MEDIA);
        appSyncSource.setAuthorityType(AndroidAppSyncSource.AUTHORITY_MEDIA_TYPE_VIDEOS);

        appSyncSource.setIsRefreshSupported(SynchronizationController.REFRESH_FROM_SERVER, false);
        appSyncSource.setIsRefreshSupported(SynchronizationController.REFRESH_TO_SERVER, false);
        appSyncSource.setUiSourceIndex(getSourcePosition(id));
        appSyncSource.setProviderUri(Media.EXTERNAL_CONTENT_URI);

        appSyncSource.setBandwidthSaverUse(customization.useBandwidthSaverMedia());

        // Create the proper settings component for this source
        Class videoSettings = Class.forName("com.funambol.android.source.media.video.VideoSettingsUISyncSource");
        appSyncSource.setSettingsUIClass(videoSettings);

        // No dev settings for this source
        appSyncSource.setDevSettingsUIClass(null);

        Class buttonView = Class.forName("com.funambol.android.activities.AndroidButtonUISyncSource");
        appSyncSource.setButtonUIClass(buttonView);

        Class aloneView = Class.forName("com.funambol.android.activities.AndroidAloneUISyncSource");
        appSyncSource.setAloneUIClass(aloneView);

        appSyncSource.setHasSetting(AppSyncSource.SYNC_MODE_SETTING,
                customization.isSyncDirectionVisible(),
                customization.getDefaultSourceSyncModes(id, deviceInfo.getDeviceRole()));
        appSyncSource.setHasSetting(AppSyncSource.SYNC_FOLDER_SETTING, true,
                localization.getLanguage("sync_videos_folder_label"));

        SourceConfig sc = null;
        String defaultUri = customization.getDefaultSourceUri(id);
        sc = new SourceConfig("videos", SourceConfig.FILE_OBJECT_TYPE, defaultUri);
        sc.setEncoding(SyncSource.ENCODING_NONE);
        sc.setSyncMode(customization.getDefaultSourceSyncMode(id, deviceInfo.getDeviceRole()));

        // Set the sync anchor to sync via media engine
        SapiSyncAnchor anchor = new SapiSyncAnchor();
        sc.setSyncAnchor(anchor);

        VideoAppSyncSourceConfig asc = new VideoAppSyncSourceConfig(context,
                appSyncSource, customization, configuration);
        asc.load(sc);
        asc.setMaxItemSize(customization.getMaxAllowedFileSizeForVideos());
        appSyncSource.setConfig(asc);

        // Create the sync source
        StringKeyValueSQLiteStore trackerStore =
                new StringKeyValueSQLiteStore(context,
                ((AndroidCustomization)customization).getFunambolSQLiteDbName(),
                sc.getName());

        FileTracker tracker = new FileTracker(sc.getName(), trackerStore);
        tracker.setItemsSorter(new MediaItemsSorter(true));

        String sdCardDirectory = Environment.getExternalStorageDirectory().toString();
        StringBuffer dataDirectory = new StringBuffer();
        dataDirectory.append(sdCardDirectory);
        dataDirectory.append("/DCIM");
        dataDirectory.append("/").append(asc.getBucketId());

        VideoSyncSource syncSource = new VideoSyncSource(sc, tracker,
                dataDirectory.toString(), sdCardDirectory, appSyncSource,
                configuration, context);
        syncSource.setSupportedExtensions(extensions);

        DeviceInfoInterface.DeviceRole deviceRole = deviceInfo.getDeviceRole();

        // Set up Sync Filter
        SyncFilter filter = new SyncFilter();
        Filter fullUploadFilter = new Filter(Filter.ITEMS_COUNT_TYPE);
        fullUploadFilter.setCount(customization.getFirstSyncMediaUploadLimit(VIDEOS_ID, deviceRole));

        // Disable the upload filter if the "Upload Older Media" flag is set
        boolean includeOlder = asc.getIncludeOlderMedia();
        fullUploadFilter.setEnabled(!includeOlder);
        
        Filter fullDownloadFilter;
        if(deviceInfo.isTablet()) {
            if(Log.isLoggable(Log.DEBUG)) {
                Log.debug(TAG_LOG, "This is a tablet device. Setting items count "
                        + "filter for first videos download");
            }
            fullDownloadFilter = new Filter(Filter.ITEMS_COUNT_TYPE);
            int downloadLimit = customization.getFirstSyncMediaDownloadLimit(VIDEOS_ID, deviceRole);
            fullDownloadFilter.setCount(downloadLimit);
        } else {
            fullDownloadFilter = new Filter(Filter.DATE_RECENT_TYPE);
            fullDownloadFilter.setDate(configuration.getFirstRunTimestamp());
        }
        filter.setFullDownloadFilter(fullDownloadFilter);
        filter.setFullUploadFilter(fullUploadFilter);
        syncSource.setFilter(filter);
        syncSource.setDataTag("videos");

        syncSource.setAllItemsSorter(new MediaItemsSorter(true));
        
        appSyncSource.setSyncSource(syncSource);

        // Setup the external app manager
        MediaExternalAppManager appManager = new MediaExternalAppManager(context, appSyncSource);
        appSyncSource.setAppManager(appManager);

        // Start monitoring directory
        autoSyncService.startMonitoringDirectory(dataDirectory.toString(), appSyncSource.getId(), extensions);

        return appSyncSource;
    }

    /**
     * Setup the source for files
     * @param configuration the AndroidConfiguration to be used to setup the
     * source
     * @return AppSyncSource related to pictures
     * @throws Exception
     */
    protected AppSyncSource setupFilesSource(AndroidConfiguration configuration) throws Exception {

        int id = FILES_ID;
        String name = localization.getLanguage("type_files");

        MediaAppSyncSource mediaAppSyncSource = new MediaAppSyncSource(name);
        mediaAppSyncSource.setId(id);
        mediaAppSyncSource.setSyncMethod(AndroidAppSyncSource.SYNC_ADAPTER);
        mediaAppSyncSource.setAuthority(AndroidAppSyncSource.AUTHORITY_FILES);
        mediaAppSyncSource.setIsRefreshSupported(SynchronizationController.REFRESH_FROM_SERVER, false);
        mediaAppSyncSource.setIsRefreshSupported(SynchronizationController.REFRESH_TO_SERVER, false);
        mediaAppSyncSource.setUiSourceIndex(getSourcePosition(id));
        mediaAppSyncSource.setProviderUri(FilesContentProvider.CONTENT_URI);

        mediaAppSyncSource.setBandwidthSaverUse(customization.useBandwidthSaverMedia());

        // Create the proper settings component for this source
        Class fileSettings = Class.forName("com.funambol.android.source.media.file.FileSettingsUISyncSource");
        mediaAppSyncSource.setSettingsUIClass(fileSettings);

        // No dev settings for this source
        mediaAppSyncSource.setDevSettingsUIClass(null);

        Class buttonView = Class.forName("com.funambol.android.activities.AndroidButtonUISyncSource");
        mediaAppSyncSource.setButtonUIClass(buttonView);

        Class aloneView = Class.forName("com.funambol.android.activities.AndroidAloneUISyncSource");
        mediaAppSyncSource.setAloneUIClass(aloneView);

        mediaAppSyncSource.setHasSetting(AppSyncSource.SYNC_MODE_SETTING,
                customization.isSyncDirectionVisible(),
                customization.getDefaultSourceSyncModes(id, deviceInfo.getDeviceRole()));
        mediaAppSyncSource.setHasSetting(AppSyncSource.SYNC_FOLDER_SETTING, true,
                localization.getLanguage("sync_files_folder_label"));

        SourceConfig sc = null;
        String defaultUri = customization.getDefaultSourceUri(id);
        sc = new SourceConfig("files", SourceConfig.BRIEFCASE_TYPE, defaultUri);
        sc.setEncoding(SyncSource.ENCODING_NONE);
        sc.setSyncMode(customization.getDefaultSourceSyncMode(id, deviceInfo.getDeviceRole()));

        // Set the sync anchor to sync via media engine
        SapiSyncAnchor anchor = new SapiSyncAnchor();
        sc.setSyncAnchor(anchor);

        FileAppSyncSourceConfig assc = new FileAppSyncSourceConfig(context,
                mediaAppSyncSource, customization, configuration);
        assc.load(sc);
        assc.setMaxItemSize(customization.getMaxAllowedFileSizeForFiles());
        assc.setIncludeOlderMedia(true);
        mediaAppSyncSource.setConfig(assc);

        StringBuffer defaultDir = new StringBuffer();
        String sdCardRoot = Environment.getExternalStorageDirectory().toString();
        String directoryName = ((AndroidCustomization)customization)
                    .getDefaultFilesSDCardDir();
        defaultDir = new StringBuffer();
        defaultDir.append(sdCardRoot);
        defaultDir.append("/");
        defaultDir.append(directoryName);

        if(Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG_LOG, "Path for files sync is " + defaultDir);
        }
        assc.setBaseDirectory(defaultDir.toString());

        try {
            // Create the default folder if it doesn't exist
            FileAdapter d = new FileAdapter(defaultDir.toString());
            if(!d.exists()) {
                d.mkdir();
            }
            d.close();
        } catch(IOException ex) {
            Log.error(TAG_LOG, "Cannot create directory: " + defaultDir.toString(), ex);
        }

        // Create the sync source
        StringKeyValueSQLiteStore trackerStore =
                new StringKeyValueSQLiteStore(context,
                ((AndroidCustomization)customization).getFunambolSQLiteDbName(),
                sc.getName());

        FileTracker tracker = new FileTracker(sc.getName(), trackerStore);

        // Set the current file tracker to be notified of rename operations
        autoSyncService.startMonitoringDirectory(assc.getBaseDirectory(), mediaAppSyncSource.getId(), null);

        AndroidFileSyncSource syncSource = new AndroidFileSyncSource(sc, tracker,
                defaultDir.toString(), sdCardRoot, assc.getMaxItemSize(),
                customization, context);
        syncSource.setDataTag("files");
        
        mediaAppSyncSource.setSyncSource(syncSource);

        return mediaAppSyncSource;
    }


    /**
     * Setup the source for config
     * @param configuration the AndroidConfiguration to be used to setup the
     * source
     * @return AppSyncSource related to config
     * @throws Exception
     */
    private AppSyncSource setupConfigSource(AndroidConfiguration configuration) throws Exception {
        int id = CONFIG_ID;
        // This source is invisible, don't care about the name and its
        // localization
        String name = "config";
        AndroidAppSyncSource configSource = new AndroidAppSyncSource(name);
        configSource.setId(id);
        configSource.setEnabledLabel(null);
        configSource.setDisabledLabel(null);
        configSource.setIconName(null);
        configSource.setDisabledIconName(null);
        configSource.setUiSourceIndex(0);
        configSource.setIsRefreshSupported(false);
        configSource.setIsVisible(false);
        configSource.setSyncMethod(AndroidAppSyncSource.DIRECT);

        SourceConfig sc = new SourceConfig("config", SourceConfig.BRIEFCASE_TYPE,
                                           customization.getDefaultSourceUri(id));
        sc.setEncoding(SyncSource.ENCODING_NONE);
        sc.setSyncMode(customization.getDefaultSourceSyncMode(id, deviceInfo.getDeviceRole()));

        // Set the sync anchor
        SyncMLAnchor anchor = new SyncMLAnchor();
        sc.setSyncAnchor(anchor);

        AppSyncSourceConfig asc = new AppSyncSourceConfig(configSource, customization, configuration);
        asc.load(sc);
        configSource.setConfig(asc);

        StringKeyValueMemoryStore configStore = new StringKeyValueMemoryStore();
        StringKeyValueMemoryStore cacheStore  = new StringKeyValueMemoryStore();
        CacheTracker tracker = new CacheTracker(cacheStore);
 
        SyncSource syncSource = new ConfigSyncSource(sc, tracker, configStore);

        // Reset the underlying sync source
        configSource.setSyncSource(syncSource);

        return configSource;
    }

    private DataStore createDataStore(String name, String type, String version,
            AbstractDataManager dm) {

        DataStore ds = new DataStore();
        SourceRef sr = new SourceRef();
        sr.setValue(name);
        ds.setSourceRef(sr);

        CTInfo rxPref = new CTInfo();
        rxPref.setCTType(type);
        rxPref.setVerCT(version);
        ds.setRxPref(rxPref);

        CTInfo txPref = new CTInfo();
        txPref.setCTType(type);
        txPref.setVerCT(version);
        ds.setTxPref(txPref);

        SyncCap syncCap = new SyncCap();
        Vector types = new Vector();
        types.addElement(SyncType.TWO_WAY);
        types.addElement(SyncType.SLOW);
        types.addElement(SyncType.SERVER_ALERTED);
        syncCap.setSyncType(types);
        ds.setSyncCap(syncCap);

        // Max GUID size set to 2 bytes as default
        ds.setMaxGUIDSize(2);

        Vector properties = dm.getSupportedProperties();
        if(properties != null) {
            Vector ctCaps = new Vector();
            CTCap ctCap = new CTCap();
            ctCap.setCTInfo(new CTInfo(type, version));
            ctCap.setProperties(properties);
            ctCaps.add(ctCap);
            ds.setCTCaps(ctCaps);
        }
        return ds;
    }
}
