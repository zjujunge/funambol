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

package com.funambol.android.source.pim.calendar;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Build;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.funambol.androidsync.R;
import com.funambol.android.AndroidUtils;
import com.funambol.android.AndroidAppSyncSource;
import com.funambol.android.controller.AndroidController;

import com.funambol.sync.SyncSource;
import com.funambol.client.source.AppSyncSource;
import com.funambol.util.Log;

public class CalendarAppSyncSource extends AndroidAppSyncSource {

    private static final String TAG_LOG = "CalendarAppSyncSource";
    private static final int FUNAMBOL_CALENDAR_COLOR = 0xFF33bdf0;
    private Context context = null;

    public CalendarAppSyncSource(Context context, String name, SyncSource source) {
        super(name, source);
        this.context = context;
    }

    public CalendarAppSyncSource(Context context, String name) {
        this(context, name, null);
    }

    @Override
    public boolean isWorking() {
        CalendarAppSyncSourceConfig config = (CalendarAppSyncSourceConfig)getConfig();
        long calendarId = config.getCalendarId();
        if (calendarId == -1) {
            return false;
        } else {
            return super.isWorking();
        }
    }

    @Override
    public void accountCreated(String accountName, String accountType) {
        // The account was created, we can now create our own calendar (in case
        // this is needed)
        Log.trace(TAG_LOG, "Setting calendar to sync");

        CalendarDescriptor res = createCalendar(accountName, accountType);

        if (res != null) {
            CalendarAppSyncSourceConfig config = (CalendarAppSyncSourceConfig)getConfig();
            config.setCalendarId(res.getId());
            config.save();
        }
    }

    /**
     * This method can be used to create the default calendar associated to this
     * account. If the calendar already exists, the method simply returns a
     * reference to it.
     *
     * @return the calendar id
     */
    public long createCalendar() {
        Account account = AndroidController.getNativeAccount();
        if (account == null) {
            return -1;
        }
        CalendarDescriptor res = createCalendar(account.name, account.type);
        if (res == null) {
            return -1;
        } else {
            return res.getId();
        }
    }

    private CalendarDescriptor createCalendar(String accountName, String accountType) {
        // On Android 2.2 and onward we always create the Funambol calendar to
        // sync. On older versions, because of a bug in Android, we try to use a
        // calendar that gets not removed once a google sync kicks in
        CalendarDescriptor res = null;
        if (Build.VERSION.SDK_INT >= 8 || AndroidUtils.isSimulator(context)) {
            Log.info(TAG_LOG, "On this version of Android, we create a separate calendar");
            res = createFunambolCalendar(accountName, accountType);
        }
        return res;
    }



    /**
     * This class represent a calendar, with all its relevant properties
     */
    private class CalendarDescriptor {
        private long id;
        private String name;

        public CalendarDescriptor(long id, String name) {
            this.id = id;
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Find the default native calendar if any.
     *
     * @return a calendar descriptor if the calendar was found
     */
    private CalendarDescriptor findNativeCalendar() {

        if (Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG_LOG, "Searching for native calendars");
        }
        ContentResolver resolver = context.getContentResolver();
        Cursor dcals = resolver.query(CalendarManager.Calendars.CONTENT_URI, null, null, null, null);

        if (dcals == null) {
            return null;
        }

        CalendarDescriptor res = null;
        try {

            if (dcals.moveToFirst()) {
                do {
                    long calendarId = dcals.getLong(dcals.getColumnIndexOrThrow(CalendarManager.Calendars._ID));
                    String calendarName = dcals.getString(dcals.getColumnIndexOrThrow(CalendarManager.Calendars.DISPLAY_NAME));
                    String syncAccountType = dcals.getString(dcals.getColumnIndexOrThrow(CalendarManager.Calendars._SYNC_ACCOUNT_TYPE));
                    String syncAccount = dcals.getString(dcals.getColumnIndexOrThrow(CalendarManager.Calendars._SYNC_ACCOUNT));
                    String ownerAccount = dcals.getString(dcals.getColumnIndexOrThrow(CalendarManager.Calendars.OWNER_ACCOUNT));

                    // There is some blackmagic here, because each manufacturer
                    // creates local calendars with different properties.
                    if (ownerAccount != null && ownerAccount.indexOf("default@") != -1) {
                        // This the personal calendar on HTC devices
                        res = new CalendarDescriptor(calendarId, calendarName);
                    } else if ("local".equals(syncAccount) && "local".equals(syncAccountType)) {
                        // This is the personal calendar on Samsung devices
                        res = new CalendarDescriptor(calendarId, calendarName);
                    } else {
                        if (Log.isLoggable(Log.DEBUG)) {
                            Log.debug(TAG_LOG, "Calendar found with id: " + calendarId + " name: " + calendarName);
                        }
                        if (Log.isLoggable(Log.DEBUG)) {
                            Log.debug(TAG_LOG, "Calendar found with sync account: " + syncAccount);
                        }
                        if (Log.isLoggable(Log.DEBUG)) {
                            Log.debug(TAG_LOG, "Calendar found with sync account type: " + syncAccountType);
                        }
                        if (Log.isLoggable(Log.DEBUG)) {
                            Log.debug(TAG_LOG, "Calendar found with owner account: " + ownerAccount);
                        }
                    }

                    if (res != null) {
                        break;
                    }
                } while(dcals.moveToNext());
            }
        } finally {
            dcals.close();
        }
        return res;
    }

    /**
     * Create a Funambol calendar
     */
    private CalendarDescriptor createFunambolCalendar(String accountName, String accountType) {

        if (Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG_LOG, "Creating Funambol Calendar");
        }
        ContentResolver resolver = context.getContentResolver();

        // Query all the calendars belonging to a funambol account
        Cursor cals = resolver.query(CalendarManager.Calendars.CONTENT_URI,
                                     CalendarManager.Calendars.PROJECTION,
                                     CalendarManager.Calendars._SYNC_ACCOUNT_TYPE+"='" +accountType+"'", null, null);

        if (cals == null) {
            // No provider available for calendar. Do not even try to create a
            // calendar
            if (Log.isLoggable(Log.INFO)) {
                Log.info(TAG_LOG, "No provider found for calendar");
            }
            return null;
        }

        CalendarDescriptor res = null;

        try {
            if(!cals.moveToFirst()) {
                // The funambol calendar doesn't exist -> create it if the account
                // information is correctly loaded
                if (Log.isLoggable(Log.DEBUG)) {
                    Log.debug(TAG_LOG, "No Funambol calendar defined, create one (" + accountName + "," + accountType);
                }
                if(accountName != null && accountType != null) {

                    String calDisplayName = context.getString(R.string.account_label);

                    ContentValues cv = new ContentValues();
                    cv.put(CalendarManager.Calendars._SYNC_ACCOUNT,      accountName);
                    cv.put(CalendarManager.Calendars._SYNC_ACCOUNT_TYPE, accountType);
                    cv.put(CalendarManager.Calendars.DISPLAY_NAME,       calDisplayName);
                    cv.put(CalendarManager.Calendars.OWNER_ACCOUNT,      accountName);
                    cv.put(CalendarManager.Calendars.SYNC_EVENTS,        1);
                    cv.put(CalendarManager.Calendars.ACCESS_LEVEL,       700);
                    cv.put(CalendarManager.Calendars.COLOR,              FUNAMBOL_CALENDAR_COLOR);

                    Uri calUri = resolver.insert(CalendarManager.Calendars.CONTENT_URI, cv);
                    long calendarId = Long.parseLong(calUri.getLastPathSegment());
                    String calendarName = calDisplayName;
                    res = new CalendarDescriptor(calendarId, calendarName);
                } else {
                    Log.error(TAG_LOG, "Cannot initialize calendar since there is " +
                            "no accounts defined.");
                }
            } else {
                long calendarId = cals.getLong(cals.getColumnIndexOrThrow(CalendarManager.Calendars._ID));
                String calendarName = cals.getString(cals.getColumnIndexOrThrow(CalendarManager.Calendars.DISPLAY_NAME));
                res = new CalendarDescriptor(calendarId, calendarName);
                if (Log.isLoggable(Log.DEBUG)) {
                    Log.debug(TAG_LOG, "Calendar found with id: " + calendarId);
                }
            }
        } finally {
            if (cals != null) {
                cals.close();
            }
        }
        return res;
    }
}
