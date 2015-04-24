/*
 * Funambol is a mobile platform developed by Funambol, Inc. 
 * Copyright (C) 2003 - 2010 Funambol, Inc.
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


package com.funambol.android.integration;

import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;

import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import com.funambol.android.App;
import com.funambol.android.AppInitializer;
import com.funambol.android.source.pim.PimTestRecorder;

import com.funambol.android.source.pim.calendar.Calendar;
import com.funambol.android.source.pim.calendar.CalendarAppSyncSourceConfig;
import com.funambol.android.source.pim.calendar.CalendarManager;
import com.funambol.android.source.pim.calendar.CalendarManager.Events;

import com.funambol.common.pim.model.calendar.Attendee;
import com.funambol.common.pim.model.calendar.Event;
import com.funambol.common.pim.model.calendar.Reminder;
import com.funambol.common.pim.model.common.Property;

import com.funambol.client.source.AppSyncSource;
import com.funambol.client.source.AppSyncSourceManager;
import com.funambol.client.test.ClientTestException;
import com.funambol.client.test.basic.BasicRobot;
import com.funambol.client.test.calendar.CalendarCommandRunner;
import com.funambol.client.test.calendar.CalendarRobot;
import com.funambol.client.configuration.Configuration;

import com.funambol.common.pim.model.common.PropertyWithTimeZone;
import com.funambol.common.pim.model.utility.TimeUtils;
import com.funambol.sync.SyncItem;
import com.funambol.sync.SyncSource;
import com.funambol.sync.SyncException;
import com.funambol.util.Log;
import com.funambol.util.StringUtil;

/**
 * This is a robot implementation that owns the logic of the commands to write 
 * vcalendar integration tests
 */
public class AndroidCalendarsRobot extends CalendarRobot {

    private static final String TAG_LOG = "AndroidCalendarsRobot";

    private Instrumentation instrumentation = null;

    private CalendarManager cm = null;
    private Event currentEvent = null;

    private ContentValues rawEventValues;
    private ContentValues rawReminderValues;
    private long lastRawEventId = -1;
    private String lastCheckedEventId = null;

    /**
     * Default constructor
     * @param instrumentation the Instrumentation object useful to retrieve both the 
     * context and the resolver for calendars
     */
    public AndroidCalendarsRobot(Instrumentation instrumentation, BasicRobot basicRobot) {
        this.basicRobot = basicRobot;
        this.instrumentation = instrumentation;
    }

    /**
     * Getter to retrieve the CalendarManager instance
     * @return CalendarManager the Singleton instance of the Calendar manager 
     */
    private CalendarManager getCalendarManager() throws SyncException {
        if(cm == null) {
            cm = new CalendarManager(instrumentation.getContext(), getAppSyncSource(), false);
            // At the moment we need to invoke the beginSync method at least once to make sure
            // the config is properly initialized. At the beginning of a sync we
            // check if the calendar is properly set and reset it if necessary
            SyncSource calSource = getAppSyncSource().getSyncSource();
            calSource.beginSync(SyncSource.INCREMENTAL_SYNC, false);
        }
        return cm;
    }
    
    protected AppSyncSource getAppSyncSource() {
        return getAppSyncSourceManager().getSource(AppSyncSourceManager.EVENTS_ID);
    }

    @Override
    protected AppSyncSourceManager getAppSyncSourceManager() {
        if (appSourceManager == null) {
            AppInitializer appInitializer = App.i().getAppInitializer();
            appSourceManager = appInitializer.getAppSyncSourceManager();
        }
        return appSourceManager;
    }

    @Override
    public void createEmptyEvent() throws Throwable {
        currentEvent = new Event();
    }
    
    @Override
    public void setEventField(String field, String value) throws Throwable {
        if(currentEvent == null) {
            throw new ClientTestException("You have to inizialize the event before editing it");
        }
        if(CalendarCommandRunner.EVENT_FIELD_START.equals(field)) {
            currentEvent.setDtStart(new Property(value));
        } else if(CalendarCommandRunner.EVENT_FIELD_END.equals(field)) {
            currentEvent.setDtEnd(new Property(value));
        } else if(CalendarCommandRunner.EVENT_FIELD_SUMMARY.equals(field)) {
            currentEvent.setSummary(new Property(value));
        } else if(CalendarCommandRunner.EVENT_FIELD_DESCRIPTION.equals(field)) {
            currentEvent.setDescription(new Property(value));
        } else if(CalendarCommandRunner.EVENT_FIELD_LOCATION.equals(field)) {
            currentEvent.setLocation(new Property(value));
        } else if(CalendarCommandRunner.EVENT_FIELD_ALLDAY.equals(field)) {
            boolean isAllDay = value.equals("1");
            currentEvent.setAllDay(isAllDay);
        } else if(CalendarCommandRunner.EVENT_FIELD_ATTENDEES.equals(field)) {
            Attendee a = new Attendee();
            a.setEmail(value);
            currentEvent.getAttendees().add(a);
        } else if(CalendarCommandRunner.EVENT_FIELD_REMINDER.equals(field)) {
            Reminder r = new Reminder();
            r.setMinutes(Integer.parseInt(value));
            currentEvent.setReminder(r);
        } else if (CalendarCommandRunner.EVENT_FIELD_DURATION.equals(field)) {
            currentEvent.setDuration(new Property(value));
        } else if(CalendarCommandRunner.EVENT_FIELD_TIMEZONE.equals(field)) {
            throw new ClientTestException(CalendarCommandRunner.EVENT_FIELD_TIMEZONE
                    + " field not yet implemented");
        } else {
            throw new ClientTestException("Unknown field: " + field);
        }
    }

    @Override
    public void saveEvent() throws Throwable {

        Calendar calendar = new Calendar();
        
        // If the event has been set directly with the entire vCal string
        // we simply replace it
        if(eventAsVcal != null) {
            calendar.setVCalendar(eventAsVcal.toString().getBytes());
            currentEvent = calendar.getEvent();
        } else {
            // If the dtstart/dtend are not set to 0, the provider complains
            // so we set them here
            if (currentEvent.isAllDay()) {
                PropertyWithTimeZone start = currentEvent.getDtStart();
                PropertyWithTimeZone end   = currentEvent.getDtEnd();

                start = toMidnight(start);
                end   = toMidnight(end);

                currentEvent.setDtStart(start);
                currentEvent.setDtEnd(end);
            }

            calendar.setEvent(currentEvent);
        }

        // Check if summary is set
        if(currentEvent.getSummary() == null) {
            throw new ClientTestException("You must set summary before saving the event");
        }

        getCalendarManager().beginTransaction();
        if(currentEventId != -1) {
            getCalendarManager().update(Long.toString(currentEventId), calendar);
        } else {
            getCalendarManager().add(calendar);
        }
        getCalendarManager().commit();

        // Reset current event
        currentEvent = null;
        eventAsVcal = null;
        currentEventId = -1;
    }

    private PropertyWithTimeZone toMidnight(PropertyWithTimeZone dateTime) {
        String value = dateTime.getPropertyValueAsString();
        if(value != null) {
            int tPos = value.indexOf("T");
            if (tPos != -1) {
                String newValue = value.substring(0, tPos);
                Log.trace(TAG_LOG, "toMidnight returning: " + newValue);
                PropertyWithTimeZone res = new PropertyWithTimeZone(newValue, dateTime.getTimeZone());
                return res;
            }
        }
        return dateTime;
    }

    @Override
    public void deleteAllEvents() throws Throwable {
        // Phisically delete all the items from the store
        ContentResolver cr = instrumentation.getTargetContext().getContentResolver();
        Enumeration keys = getCalendarManager().getAllKeys();
        while(keys.hasMoreElements()) {
            long itemId = Long.parseLong((String) keys.nextElement());
            Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, itemId);
            cr.delete(uri, null, null);
        }
    }

    @Override
    public void deleteEvent(String summary) throws Throwable {
        ContentResolver cr = instrumentation.getTargetContext().getContentResolver();
        long itemId = findEventKey(summary);
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, itemId);
        cr.delete(uri, null, null);
    }

    @Override
    public void loadEvent(String summary) throws Throwable {
        currentEventId = findEventKey(summary);
        currentEvent = getCalendarManager().load(
                Long.toString(currentEventId)).getEvent();
    }

    @Override
    protected String getCurrentEventVCal() throws Throwable {
        if (eventAsVcal != null) {
            return eventAsVcal;
        } else {
            Calendar c = new Calendar();
            c.setEvent(currentEvent);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            c.toVCalendar(baos, true);
            return new String(baos.toByteArray());
        }
    }

    private long findEventKey(String summary) throws Throwable {
        Enumeration allkeys = getCalendarManager().getAllKeys();
        while(allkeys.hasMoreElements()) {
            long key = Long.parseLong((String)allkeys.nextElement());
            Calendar calendar = getCalendarManager().load(Long.toString(key));
            if(calendar.getEvent().getSummary().getPropertyValueAsString().equals(summary)) {
                return key;
            }
        }
        throw new ClientTestException("Can't find event: " + summary);
    }

    @Override
    public void createEmptyRawEvent() throws Throwable {
        rawEventValues = new ContentValues();
        rawReminderValues = new ContentValues();
    }

    @Override
    public void setRawEventField(String fieldName, String fieldValue) throws Throwable {
        boolean skip = false;
        // There are some fields that some providers do not allow to write. We
        // shall skip them
        if (!PimTestRecorder.isFieldAllowed(fieldName))  {
            Log.trace(TAG_LOG, "Skipping raw field " + fieldName);
            skip = true;
        }
        if (!skip) {
            rawEventValues.put(fieldName, fieldValue);
        }
    }

    @Override
    public void setRawReminderField(String fieldName, String fieldValue) throws Throwable {
        boolean skip = false;
        // There are some fields that the provider does not allow to write. We
        // shall skip them
        if (fieldName.equals(CalendarManager.Reminders._ID) ||
            fieldName.equals(CalendarManager.Reminders.EVENT_ID))
        {
            skip = true;
            Log.trace(TAG_LOG, "Skipping raw field " + fieldName);
        }

        if (!skip) {
            rawReminderValues.put(fieldName, fieldValue);
        }
    }

    @Override
    public void saveRawEvent() throws Throwable {
        ContentResolver resolver = instrumentation.getContext().getContentResolver();

        // Last think to do before saving the event is to set the related calendar_id field
        AppSyncSource appSource = getAppSyncSourceManager().getSource(AppSyncSourceManager.EVENTS_ID);
        CalendarAppSyncSourceConfig config = (CalendarAppSyncSourceConfig)appSource.getConfig();
        rawEventValues.put("calendar_id", config.getCalendarId());

        Uri eventUri = resolver.insert(CalendarManager.Events.CONTENT_URI, rawEventValues);
        lastRawEventId = Long.parseLong(eventUri.getLastPathSegment());

        // Set the event id
        rawReminderValues.put(CalendarManager.Reminders.EVENT_ID, "" + lastRawEventId);

        // Now save the reminder
        resolver.insert(CalendarManager.Reminders.CONTENT_URI, rawReminderValues);
    }

    @Override
    public void checkRawEventAsVCal(String vcal1) throws Throwable {

        // \r\n must be explicit, so we remove them
        vcal1 = StringUtil.replaceAll(vcal1, "\r", "");
        vcal1 = StringUtil.replaceAll(vcal1, "\n", "");

        vcal1 = StringUtil.replaceAll(vcal1, "\\r\\n", "\r\n");

        Calendar cal = getCalendarManager().load(Long.toString(lastRawEventId));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        cal.toVCalendar(os, true);

        String vcal2 = os.toString();

        // Daylight information depends on the device current date, so it can
        // change over time. For this reason we shall perform an intelligent
        // comparison
        Vector dayLight1 = new Vector();
        vcal1 = stripDaylightInfo(vcal1, dayLight1);

        Vector dayLight2 = new Vector();
        vcal2 = stripDaylightInfo(vcal2, dayLight2);

        assertTrue(vcal2, vcal1, "VCalendar mismatch");
        checkDaylight(dayLight1, dayLight2);
    }


    @Override
    public void checkRawEventField(String fieldName, String fieldValue) throws Throwable {
        // We must load the values in the DB and compare them to the given
        // representation
        ContentResolver cr = instrumentation.getTargetContext().getContentResolver();
        AppSyncSource appSource = getAppSyncSourceManager().getSource(AppSyncSourceManager.EVENTS_ID);
        CalendarAppSyncSourceConfig config = (CalendarAppSyncSourceConfig)appSource.getConfig();

        Cursor cursor = cr.query(CalendarManager.Events.CONTENT_URI, null,
                CalendarManager.Events.CALENDAR_ID + "='" + config.getCalendarId() + "'", null, null);
        try {
            if(cursor != null && cursor.moveToFirst()) {
                if(PimTestRecorder.isFieldAllowed(fieldName)) {
                    String localValue = cursor.getString(
                            cursor.getColumnIndexOrThrow(fieldName));
                    if (fieldName.equals("_id")) {
                        lastCheckedEventId = localValue;
                    }
                    // Unescape commas
                    fieldValue = StringUtil.replaceAll(fieldValue, "?-?", ",");
                    // Unescape CRLF
                    fieldValue = StringUtil.replaceAll(fieldValue, "\\r", "\r");
                    fieldValue = StringUtil.replaceAll(fieldValue, "\\n", "\n");

                    // Reserve a special behaviour for some fields
                    if(fieldName.equals("eventTimezone")) {
                        TimeZone timezone = TimeZone.getTimeZone(fieldValue);
                        TimeZone localTimezone = TimeZone.getTimeZone(
                                localValue);
                        assertTrue(localTimezone.getRawOffset(),
                                timezone.getRawOffset(),
                                "Raw field mismatch: " + fieldName);
                    } else if(fieldName.equals("duration") &&
                            Integer.parseInt(Build.VERSION.SDK) < 8 &&
                            isAllDay(cursor)) {
                        // In Android 2.2 the duration for allday events is 
                        // converted in P<days>D format. Other versions keeps
                        // the duration in seconds.
                        long minutes = TimeUtils.getAlarmInterval(localValue);
                        minutes++;
                        int days = (int)((minutes / 60) / 24);
                        if(days == 0) {
                            days = 1;
                        }
                        StringBuffer duration = new StringBuffer(10);
                        duration.append("P");
                        duration.append(days);
                        duration.append("D");
                        assertTrue(fieldValue, duration.toString(),
                            "Raw field mismatch: " + fieldName);
                    } else {
                        assertTrue(fieldValue, localValue,
                            "Raw field mismatch: " + fieldName);
                    }
                } else {
                    Log.debug(TAG_LOG, "Skipping unsupported field: " + fieldName);
                }
            } else {
                // Item not found
                throw new IllegalStateException("No events in the db");
            }
        } finally {
            cursor.close();
        }
    }

    private boolean isAllDay(Cursor cursor) {
        String allday = cursor.getString(cursor.getColumnIndexOrThrow("allDay"));
        return "1".equals(allday);
    }

    @Override
    public void checkRawReminderField(String fieldName, String fieldValue) throws Throwable {
        // We must load the values in the DB and compare them to the given
        // representation
        ContentResolver cr = instrumentation.getTargetContext().getContentResolver();

        Cursor cursor = cr.query(CalendarManager.Reminders.CONTENT_URI, null,
                CalendarManager.Reminders.EVENT_ID + "='" + lastCheckedEventId + "'", null, null);
        try {
            if(cursor != null && cursor.moveToFirst()) {
                if(PimTestRecorder.isFieldAllowed(fieldName)) {
                    String localValue = cursor.getString(
                            cursor.getColumnIndexOrThrow(fieldName));
                    if (fieldName.equals("_id")) {
                        lastCheckedEventId = localValue;
                    }
                    // Unescape commas
                    fieldValue = StringUtil.replaceAll(fieldValue, "?-?", ",");
                    // Unescape CRLF
                    fieldValue = StringUtil.replaceAll(fieldValue, "\\r", "\r");
                    fieldValue = StringUtil.replaceAll(fieldValue, "\\n", "\n");
                    assertTrue(fieldValue, localValue.toString(), 
                            "Reminder raw field mismatch: " + fieldName);
                } else {
                    Log.debug(TAG_LOG, "Skipping unsupported field: " + fieldName);
                }
            } else {
                // Item not found
                throw new IllegalStateException("No reminders in the db");
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    protected Configuration getConfiguration() {
        return App.i().getAppInitializer().getConfiguration();
    }

    private String stripDaylightInfo(String vcal, Vector daylight) {

        String seps[] = new String[1];
        seps[0] = "\r\n";
        String rows[] = StringUtil.split(vcal, seps);

        StringBuffer res = new StringBuffer();
        for(int i=0;i<rows.length;++i) {
            String row = rows[i];
            if (row.startsWith("DAYLIGHT:")) {
                daylight.addElement(row);
            } else {
                res.append(row);
            }
        }
        return res.toString();
    }

    private void checkDaylight(Vector expected, Vector actual) throws ClientTestException {
        // We check that expected is a subset of actual
        for(int i=0;i<expected.size();++i) {
            String e = (String)expected.elementAt(i);
            // We must search the same in the actual
            boolean found = false;
            for(int j=0;j<actual.size();++j) {
                String a = (String)actual.elementAt(j);
                if (e.equals(a)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Log.error(TAG_LOG, "expected " + e + " not found");
            }
            assertTrue(found, "DAYLIGHT section mismatch");
        }
    }
}
