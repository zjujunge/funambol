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

import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;

import com.funambol.android.BuildInfo;
import com.funambol.android.source.pim.calendar.CalendarManager;
import com.funambol.android.source.pim.calendar.CalendarAppSyncSourceConfig;

import com.funambol.sync.SyncItem;
import com.funambol.sync.SyncSource;
import com.funambol.sync.SyncAnchor;
import com.funambol.syncml.spds.SyncMLAnchor;
import com.funambol.client.source.AppSyncSource;
import com.funambol.client.source.AppSyncSourceManager;
import com.funambol.client.controller.Controller;
import com.funambol.client.ui.Screen;
import com.funambol.client.ui.DisplayManager;
import com.funambol.client.controller.DialogOption;
import com.funambol.util.StringUtil;
import com.funambol.util.Log;

public class PimTestRecorder {

    private static final String TAG_LOG = "TestRecoder";

    public static final int TEST_C2S = 0;
    public static final int TEST_S2C = 1;

    private static PimTestRecorder instance = null;
    private int testType;
    private int sourceId;
    private Context context;
    private ContentResolver resolver;
    private Controller controller;
    private SyncItem sentItem;
    private SyncItem receivedItem;

    private PimTestRecorder(Context context, Controller controller) {
        this.context = context;
        this.controller = controller;
        resolver = context.getContentResolver();
    }

    public static PimTestRecorder getInstance(Context context, Controller controller) {
        if (instance == null) {
            instance = new PimTestRecorder(context, controller);
        }
        return instance;
    }

    public static PimTestRecorder getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PimTestRecorder not initialized");
        }
        return instance;
    }

    public void saveOutgoingItem(SyncItem item) {
        sentItem = item;
    }

    public void saveIncomingItem(SyncItem item) {
        receivedItem = item;
    }

    public void beginTest(int sourceId, int testType) {
        this.testType = testType;
        this.sourceId = sourceId;
        if (Log.isLoggable(Log.TRACE)) {
            Log.trace(TAG_LOG, "Beginning test: " + sourceId + "," + testType);
        }
        // Clean up the proper info
        AppSyncSourceManager appSyncSourceManager = controller.getAppSyncSourceManager();
        AppSyncSource appSource = appSyncSourceManager.getSource(sourceId);
        SyncSource source = appSource.getSyncSource();
        // We do a trick here. The refresh clears the anchor, but in this case
        // we don't want to perform a slow sync. We just want to delete all the
        // events and do not report the deletes to the server. So we preserve
        // the anchors
        SyncMLAnchor anchor = (SyncMLAnchor)source.getConfig().getSyncAnchor();
        long lastAnchor = anchor.getLast();
        source.beginSync(SyncSource.FULL_DOWNLOAD, false);
        source.endSync();
        anchor.setLast(lastAnchor);

        sentItem = null;
        receivedItem = null;
    }

    public void endTest() {

        if (sourceId == AppSyncSourceManager.EVENTS_ID) {
            endCalendarTest();
        } else {
            throw new IllegalStateException("Test recording not supported for this source");
        }
    }

    public void startTestPressed(Screen screen) {
        // We shall ask the user the source and the direction

        DisplayManager dm = controller.getDisplayManager();

        DialogOption options[] = new DialogOption[2];
        options[0] = new SourceSelector(screen, "Contacts", AppSyncSourceManager.CONTACTS_ID, dm, 100);
        options[1] = new SourceSelector(screen, "Events", AppSyncSourceManager.EVENTS_ID, dm, 100);
        dm.promptSelection(screen, "Select the source", options, 0, 100);
    }

    private class SourceSelector extends DialogOption {

        private DisplayManager dm;
        private int dialogId;

        public SourceSelector(Screen screen, String text, int value, DisplayManager dm, int id) {
            super(screen, text, value);
            this.dm = dm;
            this.dialogId = id;
        }

        public void run() {
            // Close this dialog
            dm.dismissSelectionDialog(dialogId);

            sourceId = getValue();

            DialogOption options[] = new DialogOption[2];
            options[0] = new DirectionSelector(screen, "C2S", TEST_C2S, dm, 101);
            options[1] = new DirectionSelector(screen, "S2C", TEST_S2C, dm, 101);
            dm.promptSelection(screen, "Select the direction", options, 0, 101);
        }
    }

    private class DirectionSelector extends DialogOption {

        private DisplayManager dm;
        private int dialogId;

        public DirectionSelector(Screen screen, String text, int value, DisplayManager dm, int id) {
            super(screen, text, value);
            this.dm = dm;
            this.dialogId = id;
        }

        public void run() {
            // Close this dialog
            dm.dismissSelectionDialog(dialogId);
            beginTest(sourceId, getValue());
        }
    }

    public void endTestPressed() {
        if (sourceId == AppSyncSourceManager.CONTACTS_ID) {
            endContactsTest();
        } else if (sourceId == AppSyncSourceManager.EVENTS_ID) {
            endCalendarTest();
        }
    }

    private void endContactsTest() {

        if (Log.isLoggable(Log.INFO)) {
            Log.info(TAG_LOG, "Generating script for contacts test");
        }

        // Generate the script in the Log
        StringBuffer script = new StringBuffer();
        printScriptHeaders(script);

        script.append("<Script>\n");
        script.append("  <BeginTest>\n")
              .append("    <Arg>test_name</Arg>\n")
              .append("    <Arg>test_name</Arg>\n")
              .append("    <Arg>contact</Arg>\n")
              .append("    <Arg>").append(testType == TEST_S2C ? "s2c" : "c2s").append("</Arg>\n")
              .append("    <Arg>local</Arg>")
              .append("  </BeginTest>\n");
        script.append("  <DeleteAllContacts/>\n");

        if (testType == TEST_C2S) {

            if (sentItem == null) {
                throw new IllegalStateException("No events were synchronized to server");
            }

            // Read the DB and generate a script that generates the same values
            script.append("  <CreateEmptyRawContact/>\n");

            saveContactFields(script, sentItem.getKey());
            
            script.append("  <SaveRawContact/>\n");

            // We expect only one item in this table
            try {
                String vcard = new String(sentItem.getContent(), "UTF-8");
                // Generate a command to compare the vcal
                vcard = StringUtil.replaceAll(vcard, "\r\n", "\\r\\n\n");
                script.append("  <CheckRawContactAsVCard>\n")
                      .append("    <Arg>").append(vcard).append("</Arg>\n");
                script.append("  </CheckRawContactAsVCard>\n");
            } catch (Exception e) {
                throw new IllegalStateException("Cannot generate test script");
            }
        } else if (testType == TEST_S2C) {
            // We must have received an event that we pass down to the source
            // and we except the DB to contain the right values
            if (receivedItem == null) {
                throw new IllegalStateException("No item was received");
            }
            try {
                String vcard = new String(receivedItem.getContent(), "UTF-8");
                // We must store this item via the sync source
                vcard = StringUtil.replaceAll(vcard, "\r\n", "\\r\\n\n");
                script.append("  <SetContactFromServer>\n")
                      .append("    <Arg>").append(vcard).append("</Arg>");
                script.append("  </SetContactFromServer>\n");

                createRawContact(script, receivedItem.getKey());

            } catch (Exception e) {
                throw new IllegalStateException("Cannot generate test script");
            }
        }

        script.append("  <EndTest/>\n");
        script.append("</Script>");
        android.util.Log.i(TAG_LOG, "Generated script\n" + script.toString());
        if (Log.isLoggable(Log.INFO)) {
            Log.info(TAG_LOG, "Generated script\n" + script.toString());
        }
    }

    private void createRawContact(StringBuffer script, String id) {
        generateContactCommands(script, "CheckRawContactData", id);
    }
    
    private void saveContactFields(StringBuffer script, String id) {
        generateContactCommands(script, "SetRawContactData", id);
    }

    private void generateContactCommands(StringBuffer script, String command, String id) {

        Cursor cursor = resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[] {ContactsContract.RawContacts._ID},
                ContactsContract.RawContacts._ID+"="+id, null, null);
        try {

            if(cursor != null && cursor.moveToFirst()) {

                long contactId = cursor.getLong(0);

                Cursor dataCursor = resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[] { ContactsContract.Data.MIMETYPE,
                                   ContactsContract.Data.DATA1,
                                   ContactsContract.Data.DATA2,
                                   ContactsContract.Data.DATA3,
                                   ContactsContract.Data.DATA4,
                                   ContactsContract.Data.DATA5,
                                   ContactsContract.Data.DATA6,
                                   ContactsContract.Data.DATA7,
                                   ContactsContract.Data.DATA8,
                                   ContactsContract.Data.DATA9,
                                   ContactsContract.Data.DATA10,
                                   ContactsContract.Data.DATA11,
                                   ContactsContract.Data.DATA12,
                                   ContactsContract.Data.DATA13,
                                   ContactsContract.Data.DATA14,
                }, ContactsContract.Data.RAW_CONTACT_ID+"="+contactId,
                   null, null);

                if(dataCursor != null && dataCursor.moveToFirst()) {
                    do {
                        String mimetype = dataCursor.getString(0);

                        StringBuffer dataField = new StringBuffer();
                        dataField.append("\"").append(mimetype).append("\"");
                        
                        for(int i=1; i<15; i++) {
                            String value = dataCursor.getString(i);
                            if(value == null) {
                                value = "";
                            } else {
                                // Escape CLRF
                                value = StringUtil.replaceAll(value, "\r", "\\r");
                                value = StringUtil.replaceAll(value, "\n", "\\n");
                            }
                            dataField.append(",");
                            dataField.append("\"").append(value).append("\"");
                        }
                        script.append(command).append("(")
                                .append(dataField.toString()).append(");\n");

                    } while(dataCursor.moveToNext());
                }
            } else {
                // Item not found
                throw new IllegalStateException("No events in the db");
            }
        } finally {
            cursor.close();
        }
    }

    private void endCalendarTest() {

        if (Log.isLoggable(Log.INFO)) {
            Log.info(TAG_LOG, "Generating script for calendar test");
        }

        // Generate the script in the Log
        StringBuffer script = new StringBuffer();
        printScriptHeaders(script);
        script.append("<Script>\n");
        script.append("  <BeginTest>\n")
              .append("    <Arg>test_name</Arg>\n")
              .append("    <Arg>test_name</Arg>\n")
              .append("    <Arg>calendar</Arg>\n")
              .append("    <Arg>").append(testType == TEST_S2C ? "s2c" : "c2s").append("</Arg>\n")
              .append("    <Arg>local</Arg>")
              .append("  </BeginTest>\n");
        script.append("  <DeleteAllEvents/>\n");

        AppSyncSourceManager appSyncSourceManager = controller.getAppSyncSourceManager();
        AppSyncSource appSource = appSyncSourceManager.getSource(sourceId);
        CalendarAppSyncSourceConfig config = (CalendarAppSyncSourceConfig)appSource.getConfig();
        if (config.getCalendarId() == -1) {
            throw new IllegalStateException("Cannot access undefined calendar");
        }

        if (testType == TEST_C2S) {

            if (sentItem == null) {
                throw new IllegalStateException("No events were synchronized to server");
            }

            // Read the DB and generate a script that generates the same values
            script.append("  <CreateEmptyRawEvent/>\n");
            String eventId = saveEventFields(script, config);
            saveReminderField(script, config, eventId);
            script.append("  <SaveRawEvent/>\n");

            // We expect only one item in this table
            try {
                String vcal = new String(sentItem.getContent(), "UTF-8");
                // Generate a command to compare the vcal
                vcal = StringUtil.replaceAll(vcal, "\r\n", "\\r\\n\n");
                script.append("  <CheckRawEventAsVCal>\n")
                      .append("    <Arg>").append(vcal).append("</Arg>\n");
                script.append("  </CheckRawEventAsVCal>\n");
            } catch (Exception e) {
                throw new IllegalStateException("Cannot generate test script");
            }
        } else if (testType == TEST_S2C) {
            // We must have received an event that we pass down to the source
            // and we except the DB to contain the right values
            if (receivedItem == null) {
                throw new IllegalStateException("No item was received");
            }
            try {
                String vcal = new String(receivedItem.getContent(), "UTF-8");
                // We must store this item via the sync source
                vcal = StringUtil.replaceAll(vcal, "\r\n", "\\r\\n\n");
                script.append("  <SetEventFromServer>\n")
                      .append("    <Arg>").append(vcal).append("</Arg>n");
                script.append("  </SetEventFromServer>\n");

                String eventId = createRawEvent(script, config);
                createRawReminder(script, config, eventId);

            } catch (Exception e) {
                throw new IllegalStateException("Cannot generate test script");
            }
        }

        script.append("  <EndTest/>\n");
        script.append("</Script>");

        android.util.Log.i(TAG_LOG, "Generated script\n" + script.toString());
        if (Log.isLoggable(Log.INFO)) {
            Log.info(TAG_LOG, "Generated script\n" + script.toString());
        }
    }

    private String createRawEvent(StringBuffer script, CalendarAppSyncSourceConfig config) {

        Cursor cursor = resolver.query(CalendarManager.Events.CONTENT_URI, null,
                                       CalendarManager.Events.CALENDAR_ID + "='"
                                       + config.getCalendarId() + "'",
                                       null, null);
        String eventId = null;
        try {
            if(cursor != null && cursor.moveToFirst()) {
                int numCols = cursor.getColumnCount();

                for(int i=0;i<numCols;++i) {
                    String fieldName  = cursor.getColumnName(i);
                    String fieldValue = cursor.getString(i);

                    if (fieldValue != null) {
                        if (fieldName.equals(CalendarManager.Events._ID)) {
                            eventId = fieldValue;
                        }
                        if (Log.isLoggable(Log.INFO)) {
                            Log.info(TAG_LOG, "Found col=" + fieldName + " with value=" + fieldValue);
                        }
                        // Escape CLRF
                        fieldValue = StringUtil.replaceAll(fieldValue, "\r", "\\r");
                        fieldValue = StringUtil.replaceAll(fieldValue, "\n", "\\n");
                        script.append("  <CheckRawEventField>\n")
                              .append("    <Arg>").append(fieldName).append("</Arg>\n")
                              .append("    <Arg>").append(fieldValue).append("</Arg>\n")
                              .append("  </CheckRawEventField>\n");
                    }
                }
            }
        } finally {
            cursor.close();
        }
        return eventId;
    }

    private void createRawReminder(StringBuffer script, CalendarAppSyncSourceConfig config, String eventId) {

        Cursor cursor = resolver.query(CalendarManager.Reminders.CONTENT_URI, null,
                                       CalendarManager.Reminders.EVENT_ID + "='" + eventId + "'",
                                       null, null);
        try {
            if(cursor != null && cursor.moveToFirst()) {
                int numCols = cursor.getColumnCount();

                for(int i=0;i<numCols;++i) {
                    String fieldName  = cursor.getColumnName(i);
                    String fieldValue = cursor.getString(i);

                    if (fieldValue != null) {
                        if (fieldName.equals(CalendarManager.Events._ID)) {
                            eventId = fieldValue;
                        }
                        if (Log.isLoggable(Log.INFO)) {
                            Log.info(TAG_LOG, "Found col=" + fieldName + " with value=" + fieldValue);
                        }
                        // Escape commas
                        fieldValue = StringUtil.replaceAll(fieldValue, ",", "?-?");
                        // Escape CLRF
                        fieldValue = StringUtil.replaceAll(fieldValue, "\r", "\\r");
                        fieldValue = StringUtil.replaceAll(fieldValue, "\n", "\\n");
                        script.append("  <CheckRawReminderField>\n")
                              .append("    <Arg>").append(fieldName).append("</Arg>\n")
                              .append("    <Arg>").append(fieldValue).append("</Arg>\n")
                              .append("  </CheckRawReminderField>\n");
                    }
                }
            }
        } finally {
            cursor.close();
        }
    }

    private String saveEventFields(StringBuffer script, CalendarAppSyncSourceConfig config) {

        String eventId = null;

        Cursor cursor = resolver.query(CalendarManager.Events.CONTENT_URI, null,
                CalendarManager.Events.CALENDAR_ID + "='" + config.getCalendarId() + "'", null, null);
        try {
            if(cursor != null && cursor.moveToFirst()) {
                int numCols = cursor.getColumnCount();
                for(int i=0;i<numCols;++i) {
                    String fieldName  = cursor.getColumnName(i);
                    String fieldValue = cursor.getString(i);

                    if ("_id".equals(fieldName)) {
                        eventId = fieldValue;
                    }

                    if (fieldName != null && fieldValue != null) {
                        if (Log.isLoggable(Log.INFO)) {
                            Log.info(TAG_LOG, "Found col=" + fieldName + " with value=" + fieldValue);
                        }
                        // Escape commas
                        fieldValue = StringUtil.replaceAll(fieldValue, ",", "?-?");
                        // Escape CLRF
                        fieldValue = StringUtil.replaceAll(fieldValue, "\r", "\\r");
                        fieldValue = StringUtil.replaceAll(fieldValue, "\n", "\\n");
                        script.append("  <SetRawEventField>\n")
                              .append("    <Arg>").append(fieldName).append("</Arg>\n")
                              .append("    <Arg>").append(fieldValue).append("</Arg>\n")
                              .append("  </SetRawEventField>\n");
                    }
                }
            } else {
                // Item not found
                throw new IllegalStateException("No events in the db");
            }
        } finally {
            cursor.close();
        }
        return eventId;
    }

    private void saveReminderField(StringBuffer script, CalendarAppSyncSourceConfig config, String eventId) {

        Cursor cursor = resolver.query(CalendarManager.Reminders.CONTENT_URI, null,
                CalendarManager.Reminders.EVENT_ID + "='" + eventId + "'", null, null);
        try {
            if(cursor != null && cursor.moveToFirst()) {
                int numCols = cursor.getColumnCount();
                for(int i=0;i<numCols;++i) {
                    String fieldName  = cursor.getColumnName(i);
                    String fieldValue = cursor.getString(i);

                    if (fieldName != null && fieldValue != null) {
                        if (Log.isLoggable(Log.INFO)) {
                            Log.info(TAG_LOG, "Found col=" + fieldName + " with value=" + fieldValue);
                        }
                        // Escape commas
                        fieldValue = StringUtil.replaceAll(fieldValue, ",", "?-?");
                        // Escape CLRF
                        fieldValue = StringUtil.replaceAll(fieldValue, "\r", "\\r");
                        fieldValue = StringUtil.replaceAll(fieldValue, "\n", "\\n");
                        script.append("  <SetRawReminderField>\n")
                              .append("    <Arg>").append(fieldName).append("</Arg>\n")
                              .append("    <Arg>").append(fieldValue).append("</Arg>\n")
                              .append("  </SetRawReminderField>\n");
                    }
                }
            }
        } finally {
            cursor.close();
        }
    }

    private void printScriptHeaders(StringBuffer script) {
        script.append("<!--\n");
        script.append("  DO NOT EDIT THIS FILE\n");
        script.append("  This test was automatically generated using the following device:\n");
        script.append("  - Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        script.append("  - Model: ").append(Build.MODEL).append("\n");
        script.append("  - Android OS Version: ").append(Build.VERSION.RELEASE).append("\n");
        script.append("  - Funambol Client Version: ").append(BuildInfo.VERSION).append("\n");
        script.append("-->\n");
    }

    public static boolean isFieldAllowed(String fieldName) {
        return (fieldName.equals("visibility") ||
                fieldName.equals("rrule") ||
                fieldName.equals("hasAlarm") ||
                fieldName.equals("rdate") ||
                fieldName.equals("transparency") ||
                fieldName.equals("dtstart") ||
                fieldName.equals("hasAttendeeData") ||
                fieldName.equals("description") ||
                fieldName.equals("hasExtendedProperties") ||
                fieldName.equals("eventLocation") ||
                fieldName.equals("dtend") ||
                fieldName.equals("allDay") ||
                fieldName.equals("eventTimezone") ||
                fieldName.equals("guestsCanModify") ||
                fieldName.equals("guestsCanSeeGuests") ||
                fieldName.equals("title") ||
                fieldName.equals("exdate") ||
                fieldName.equals("duration"));
    }

}

