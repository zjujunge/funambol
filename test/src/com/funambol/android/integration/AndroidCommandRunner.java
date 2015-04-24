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

import java.util.Vector;

import com.funambol.client.test.basic.BasicCommandRunner;
import com.funambol.client.test.BasicScriptRunner;
import com.funambol.util.Log;

/**
 * Implementation of the client's API BasicCommandRunner object.
 */
public class AndroidCommandRunner extends BasicCommandRunner
        implements AndroidUserCommands {

    private static final String TAG_LOG = "AndroidCommandRunner";

    private AndroidScriptRunner scriptRunner;

    /**
     * Constructor
     * @param scriptRunner the AndroidScriptRunner object that represents the
     * Instrumentation to run this test suite
     * @param robot AndroidBasicRobot to interprete all of the Android specific
     * test suite commands
     */
    public AndroidCommandRunner(AndroidScriptRunner scriptRunner,
                                AndroidBasicRobot robot,
                                String filterByName, String filterBySourceType,
                                String filterByDirection, String filterByLocality)
    {
        super(robot, filterByName, filterBySourceType, filterByDirection, filterByLocality);
        this.scriptRunner = scriptRunner;
    }

    /**
     * Run the command given in the input script by the tester. Relies upon the
     * low level BasicCommandRunner implementation for the main commands
     * definition while his extension just manages the case of waiting for a
     * specific ativity or removing an addressed account (they are architecture
     * specific implementations)
     * @param command the String representation of the input command
     * @param args the String representation of the command arguments
     * @return boolean true if the command is valid, false otherwise
     * @throws Throwable if a command thrown an exception when it was run
     */
    @Override
    public boolean runCommand(String command, Vector args) throws Throwable {

        if(!super.runCommand(command, args)) {
            if (WAIT_FOR_ACTIVITY_COMMAND.equals(command)) {
                waitForActivity(command, args);
            } else if (REMOVE_ACCOUNT_COMMAND.equals(command)) {
                removeAccount(command, args);
            } else if (CANCEL_SYNC_COMMAND.equals(command)) {
                cancelSync(command, args);
            } else if (CHECK_SYNC_PENDING_COMMAND.equals(command)) {
                checkSyncPending(command, args);
            } else if (SET_AUTO_SYNC_COMMAND.equals(command)) {
                setAutoSyncEnabled(command, args);
            } else if (SET_SOURCE_AUTO_SYNC_COMMAND.equals(command)) {
                setSourceAutoSyncEnabled(command, args);
            } else if (CHECK_SOURCE_AUTO_SYNC_COMMAND.equals(command)) {
                checkSourceAutoSyncEnabled(command, args);
            } else if (CHECK_LAST_ALERT_MESSAGE.equals(command)) {
                checkLastAlertMessage(command, args);
            } else if (CANCEL_SYNC_AFTER_PHASE.equals(command)) {
                cancelSyncAfterPhase(command, args);
            } else if (WAIT_FOR_SYNC_PHASE.equals(command)) {
                waitForSyncPhase(command, args);
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void waitForSyncToComplete(String command, Vector args) throws Throwable {

        int paramsCount = args.size();
        if(paramsCount == 2) {
            super.waitForSyncToComplete(command, args);
        } else {
            String sourceName = getParameter(args, 0);
            String minStart   = getParameter(args, 1);
            String maxWait    = getParameter(args, 2);

            checkArgument(sourceName, "Missing sourceName in " + command);
            checkArgument(minStart, "Missing min start in " + command);
            checkArgument(maxWait, "Missing max wait in " + command);

            checkObject(syncMonitor, "Run StartMainApp before command: " + command);

            int min = Integer.parseInt(minStart)*1000;
            int max = Integer.parseInt(maxWait)*1000;

            ((AndroidBasicRobot)getBasicRobot()).waitForSyncToComplete(
                    sourceName, min, max, (AndroidSyncMonitor)syncMonitor);
        }
    }

    /**
     * End test declaration command.
     * @param command the end test related Stirng formatted representation
     * @param args the command's related String arguments. Not required for this
     * command
     * @throws Throwable if an error occurred
     */
    protected void endTest(String command, Vector args) throws Throwable {
        int i = 0;
        String scriptName = getParameter(args, 0);
        while (scriptName != null) {
            if (Log.isLoggable(Log.TRACE)) {
                Log.trace(TAG_LOG, "Running end test cleanup script " + scriptName);
            }
            BasicScriptRunner bsr = scriptRunner.getBasicScriptRunner();
            Vector newArg = new Vector();
            newArg.addElement(scriptName);
            bsr.includeScript(command, newArg);
            scriptName = getParameter(args, ++i);
        }
        currentTestName = null;
    }



    /**
     * Wait for a specified amount of time that a particular activity is
     * displayed on the screen. The parameters are set into the script command
     * arguments.
     * @param command the String representation that match the "WaitForActivity"
     * command
     * @param args the "WaitForActivity" command arguments: the name of the
     * expected activity and the time interval to wait until is it displayed to
     * the tester on the screen.
     * @throws Throwable if anything went wrong with the command execution
     */
    private void waitForActivity(String command, Vector args) throws Throwable {

        String activityName = getParameter(args, 0);
        String timeout      = getParameter(args, 1);

        checkArgument(activityName, "Missing activity name in " + command);
        checkArgument(timeout, "Missing timeout in " + command);

        int t = Integer.parseInt(timeout)*1000;
        ((AndroidBasicRobot)robot).waitForActivity(activityName, t);
    }

    /**
     * Android specific account removal command execution
     * @param command the String representation of the action to "Remove" an
     * account
     * @param args the String arguments to be passed to the AndroidBasicRobot
     * in order to have a particular account deleted
     * @throws Throwable is something went wrong while executing the command
     */
    private void removeAccount(String command, Vector args) throws Throwable {
        AndroidBasicRobot.removeAccount(scriptRunner.getTargetContext());
    }

    private void cancelSync(String command, Vector args) throws Throwable {
        ((AndroidBasicRobot)getBasicRobot()).cancelSync();
    }

    private void checkSyncPending(String command, Vector args) throws Throwable {

        String sourceName = getParameter(args, 0);

        checkArgument(sourceName, "Missing source name in " + command);

        String pending = getParameter(args, 1);
        boolean checkPending = true;
        if(pending != null) {
            checkPending = Boolean.parseBoolean(pending);
        }
        ((AndroidBasicRobot)robot).checkSyncPending(sourceName, checkPending);
    }

    private void setAutoSyncEnabled(String command, Vector args) throws Throwable {

        String enabled = getParameter(args, 0);

        checkArgument(enabled, "Missing enabled param in " + command);

        ((AndroidBasicRobot)robot).setAutoSyncEnabled(Boolean.parseBoolean(enabled));
    }

    private void setSourceAutoSyncEnabled(String command, Vector args) throws Throwable {

        String sourceName = getParameter(args, 0);
        String enabled = getParameter(args, 1);

        checkArgument(enabled, "Missing source name param in " + command);
        checkArgument(enabled, "Missing enabled param in " + command);

        ((AndroidBasicRobot)robot).setSourceAutoSyncEnabled(sourceName,
                Boolean.parseBoolean(enabled));
    }

    private void checkSourceAutoSyncEnabled(String command, Vector args) throws Throwable {

        String sourceName = getParameter(args, 0);
        String enabled = getParameter(args, 1);

        checkArgument(enabled, "Missing source name param in " + command);
        checkArgument(enabled, "Missing enabled param in " + command);

        ((AndroidBasicRobot)robot).checkSourceAutoSyncEnabled(sourceName,
                Boolean.parseBoolean(enabled));
    }

    private void checkLastAlertMessage(String command, Vector args) throws Throwable {

        String message = getParameter(args, 0);

        checkArgument(message, "Missing message param in " + command);

        ((AndroidBasicRobot)robot).checkLastAlertMessage(message);
    }

    private void cancelSyncAfterPhase(String command, Vector args) throws Throwable {

        String phase  = getParameter(args, 0);
        String num    = getParameter(args, 1);
        String progress = getParameter(args, 2);

        checkArgument(phase, "Missing phase name param in " + command);
        checkArgument(num, "Missing num param in " + command);

        checkObject(syncMonitor, "Run StartMainApp before command: " + command);

        int numParam = Integer.parseInt(num);
        int progressParam = progress != null ? Integer.parseInt(progress) : -1;
        ((AndroidBasicRobot)robot).cancelSyncAfterPhase(phase, numParam, progressParam, syncMonitor);
    }
    
    private void waitForSyncPhase(String command, Vector args) throws Throwable {

        String phase = getParameter(args, 0);
        String num   = getParameter(args, 1);
        String par2  = getParameter(args, 2);
        String par3  = getParameter(args, 3);

        String timeout = null;
        String progress = null;
        if(par3 == null) {
            timeout = par2;
        } else {
            progress = par2;
            timeout = par3;
        }

        checkArgument(phase, "Missing phase name param in " + command);
        checkArgument(num, "Missing num param in " + command);
        checkArgument(timeout, "Missing timeout param in " + command);

        checkObject(syncMonitor, "Run StartMainApp before command: " + command);

        int numParam = Integer.parseInt(num);
        int progressParam = progress != null ? Integer.parseInt(progress) : -1;
        int timeoutParam = Integer.parseInt(timeout);
        ((AndroidBasicRobot)robot).waitForSyncPhase(phase, numParam,
                progressParam, timeoutParam*1000, syncMonitor);
    }
}
 
