/*
 * Funambol is a mobile platform developed by Funambol, Inc.
 * Copyright (C) 2008 Funambol, Inc.
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

import java.util.Set;
import java.util.Map;
import java.util.Hashtable;
import java.util.Vector;

import android.app.Instrumentation;
import android.os.Bundle;

import com.funambol.androidsync.R;
import com.funambol.platform.DeviceInfo;
import com.funambol.util.Log;
import com.funambol.util.AndroidLogAppender;
import com.funambol.util.StringUtil;

import com.funambol.client.test.BasicScriptRunner;
import com.funambol.client.test.calendar.CalendarCommandRunner;
import com.funambol.client.test.contact.ContactsCommandRunner;
import com.funambol.client.test.media.MediaCommandRunner;

/**
 * Instrumentation class that runs the test suite. Supports both http and file 
 * script streams. Use a BasicCommandRunner to load the test suite scripts and
 * manage the test activities control. Support additional activities to improve
 * the tester experience: the first is an editable text view that allows the
 * user to change the test suite url after the launch before running the script.
 * After the test suite has been executed a second activity displays the report
 * of the test suite for the failed test cases.
 *
 * To run the Instrumentation with adb:
 *
 *  $ adb shell am instrument -e script url [-e stopOnFailure] [-e test.filters filters]
 *      [-e test.username username] [-e test.password password]
 *      -w com.funambol.androidsync/com.funambol.android.AndroidScriptRunner
 */
public class AndroidScriptRunner extends Instrumentation {

    /** Unique ID for Logging reference */
    private static final String TAG_LOG = "AndroidScriptRunner";

    /** The instrumentation arguments loaded in the command line*/
    private static final String EXTRA_SCRIPT_URL      = "script";
    private static final String EXTRA_STOP_ON_FAILURE = "stopOnFailure";
    private static final String EXTRA_SCRIPT_FILTERS  = "filters";

   /** Default test suite url */
    private static final String SCRIPT_URL_DEFAULT = "file://scripts/android.xml";

    private String mainScriptUrl = null;

    private BasicScriptRunner basicRunner;

    private Hashtable vars = new Hashtable();

    private static AndroidScriptRunner instance;


    /**
     * When the instrumentation is created it performs the basic configuration
     * actions of initializing the log, getting the test suite script url,
     * initializing the required command runners to be used by the
     * BasicCommandRunner in order to run the test suite. After the
     * initialization the instrumentation is started.
     * @param arguments the Bundle object with the Instrumentation arguments.
     * The script url should be defined here
     */
    @Override
    public void onCreate(Bundle arguments) {
        instance = this;
        super.onCreate(arguments);

        // Init log
        Log.initLog(new AndroidLogAppender(TAG_LOG), Log.TRACE);

        boolean stopOnFailure = false;

        // Get the extra params
        String allFilters = null;
        if(arguments != null) {
            Set<String> keys = arguments.keySet();
            for(String key : keys) {
                if (EXTRA_SCRIPT_URL.equals(key)) {
                    mainScriptUrl = arguments.getString(EXTRA_SCRIPT_URL);
                } else if (EXTRA_STOP_ON_FAILURE.equals(key)) {
                    stopOnFailure = arguments.containsKey(EXTRA_STOP_ON_FAILURE);
                } else if (EXTRA_SCRIPT_FILTERS.equals(key)) {
                    allFilters = arguments.getString(EXTRA_SCRIPT_FILTERS);
                } else {
                    vars.put(key, arguments.getString(key));
                }
            }
        }
        if(StringUtil.isNullOrEmpty(mainScriptUrl)) {
            mainScriptUrl = SCRIPT_URL_DEFAULT;
        }

        String filterByName = null;
        String filterBySourceType = null;
        String filterByDirection = null;
        String filterByLocality = null;
        if (allFilters != null) {
            String filters[] = StringUtil.split(allFilters, ",");
            if (filters.length == 4) {
                filterByName = filters[0];
                filterBySourceType = filters[1];
                filterByDirection = filters[2];
                filterByLocality = filters[3];
            } else {
                Log.error(TAG_LOG, "Invalid filter " + allFilters);
            }
        }

        AndroidTestFileManager fileManager = new AndroidTestFileManager(this.getContext());
        DeviceInfo devInfo = new DeviceInfo(this.getContext());

        AndroidBasicRobot basicRobot = new AndroidBasicRobot(this, fileManager, vars);
        AndroidCommandRunner acr = new AndroidCommandRunner(this, basicRobot, filterByName, filterBySourceType,
                                       filterByDirection, filterByLocality);
        basicRunner = new BasicScriptRunner(fileManager, devInfo);
        basicRunner.addCommandRunner(acr);

        // Setup contacts script runner
        AndroidContactsRobot aContactRobot = new AndroidContactsRobot(this, basicRobot);
        aContactRobot.setScriptRunner(basicRunner);
        ContactsCommandRunner contactsCommandRunner = new ContactsCommandRunner(aContactRobot);
        basicRunner.addCommandRunner(contactsCommandRunner);

        // Setup calendar script runner
        AndroidCalendarsRobot aCalendarsRobot = new AndroidCalendarsRobot(this, basicRobot);
        aCalendarsRobot.setScriptRunner(basicRunner);
        CalendarCommandRunner calendarCommandRunner = new CalendarCommandRunner(aCalendarsRobot);
        basicRunner.addCommandRunner(calendarCommandRunner);

        // Setup media script runner
        AndroidMediaRobot aMediaRobot = new AndroidMediaRobot(this, fileManager);
        aMediaRobot.setScriptRunner(basicRunner);
        MediaCommandRunner mediaCommandRunner = new MediaCommandRunner(aMediaRobot);
        basicRunner.addCommandRunner(mediaCommandRunner);
        
        // Other properties
        basicRunner.setStopOnFailure(stopOnFailure);
        start();
    }

    @Override
    public void onStart() {   
        super.onStart();
        try {
            basicRunner.runScriptFile(mainScriptUrl, true, vars);
        } catch(Throwable t) {
            // Notify the error
            Log.error(TAG_LOG, "Error running test: ", t);
            basicRunner.setErrorCode(-1);
        }
        finish(basicRunner.getErrorCode(), new Bundle());
    }

    public BasicScriptRunner getBasicScriptRunner() {
        return basicRunner;
    }

    public static AndroidScriptRunner getInstance() {
        return instance;
    }
}
