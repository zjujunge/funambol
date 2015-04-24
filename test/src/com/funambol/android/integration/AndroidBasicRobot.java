/*
 * Funambol is a mobile platform developed by Funambol, Inc. 
 * Copyright (C) 2003 - 2009 Funambol, Inc.
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

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Vector;

import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.accounts.AccountManager;
import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import com.funambol.android.ExternalAccountManager;
import com.funambol.android.AndroidAccountManager;
import com.funambol.android.AndroidAppSyncSource;
import com.funambol.android.AndroidAppSyncSourceManager;
import com.funambol.android.AndroidConfiguration;
import com.funambol.android.App;
import com.funambol.android.AppInitializer;
import com.funambol.android.activities.AndroidActivitiesFactory;
import com.funambol.androidsync.R;
import com.funambol.android.activities.AndroidDisplayManager;
import com.funambol.android.activities.AndroidHomeScreen;
import com.funambol.android.controller.AndroidController;
import com.funambol.android.controller.AndroidHomeScreenController;
import com.funambol.client.configuration.Configuration;
import com.funambol.client.controller.Controller;
import com.funambol.client.controller.SynchronizationController;
import com.funambol.client.source.AppSyncSource;
import com.funambol.client.source.AppSyncSourceManager;
import com.funambol.client.test.ClientTestException;
import com.funambol.client.test.Robot;
import com.funambol.client.test.basic.BasicCommandRunner;
import com.funambol.client.test.basic.BasicRobot;
import com.funambol.client.test.util.SyncMonitor;
import com.funambol.client.test.util.SyncMonitorListener;
import com.funambol.client.test.util.TestFileManager;
import com.funambol.sync.SyncListener;
import com.funambol.sync.SyncSource;
import com.funambol.util.Log;
import com.funambol.platform.DeviceInfo;

public class AndroidBasicRobot extends BasicRobot {
   
    private static final String TAG_LOG = "AndroidBasicRobot";

    private Instrumentation instrumentation = null;

    private AndroidAppSyncSourceManager appSyncSourceManager = null;
    private Configuration configuration = null;

    private ActivityMonitor activityMonitor = null;
    private ArrayList<Activity> openedActivities = new ArrayList<Activity>();


    public AndroidBasicRobot(Instrumentation instrumentation,
                             TestFileManager fileManager,
                             Hashtable vars)
    {
        super(fileManager, vars);
        this.instrumentation = instrumentation;
        // Setup activity monitor
        activityMonitor = instrumentation.addMonitor((IntentFilter)null, null, false);
    }
    
    @Override
    public void initialize() {
        AppInitializer initializer = App.i().getAppInitializer();
        appSyncSourceManager = initializer.getAppSyncSourceManager();
        configuration = initializer.getConfiguration();
    }

    public void waitForActivity(String name, int timeout) throws Throwable {
        while(!getCurrentActivityName().equals(name)) {
            Thread.sleep(WAIT_DELAY);
            timeout -= WAIT_DELAY;
            if (timeout < 0) {
                throw new ClientTestException("Timeout waiting activity: " + name);
            }
        }
    }

    private String getCurrentActivityName() {
        instrumentation.waitForIdleSync();
        Activity lastActivity = null;
        if (activityMonitor != null) {
            if (activityMonitor.getLastActivity() != null) {
                lastActivity = activityMonitor.getLastActivity();
            }
        }
        boolean found = false;
        for(int i = 0; i < openedActivities.size(); i++){
            Activity aActivity = openedActivities.get(i);
            if (aActivity.getClass().getName().equals(
                lastActivity.getClass().getName())) {
                found = true;
            }
        }
        if (found) {
            return lastActivity.getClass().getSimpleName();
        } else {
            openedActivities.add(lastActivity);
            return lastActivity.getClass().getSimpleName();
        }
    }

    public void keyPress(String keyName, int count) throws Throwable {
        int keyCode = 0;
        if (BasicCommandRunner.DOWN_KEY_NAME.equals(keyName)) {
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN;
        } else if (BasicCommandRunner.UP_KEY_NAME.equals(keyName)) {
            keyCode = KeyEvent.KEYCODE_DPAD_UP;
        } else if (BasicCommandRunner.LEFT_KEY_NAME.equals(keyName)) {
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT; 
        } else if (BasicCommandRunner.RIGHT_KEY_NAME.equals(keyName)) {
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
        } else if (BasicCommandRunner.FIRE_KEY_NAME.equals(keyName)) {
            keyCode = KeyEvent.KEYCODE_DPAD_CENTER;
        } else if (BasicCommandRunner.MENU_KEY_NAME.equals(keyName)) {
            keyCode = KeyEvent.KEYCODE_MENU;
        } else if (BasicCommandRunner.BACK_KEY_NAME.equals(keyName)) {
            keyCode = KeyEvent.KEYCODE_BACK;
        } else if (BasicCommandRunner.DEL_KEY_NAME.equals(keyName)) {
            keyCode = KeyEvent.KEYCODE_DEL;
        } else {
            Log.error(TAG_LOG, "Unknown keyName: " + keyName);
            throw new IllegalArgumentException("Unknown keyName: " + keyName);
        }

        DeviceInfo devInf = new DeviceInfo(instrumentation.getTargetContext());
        boolean done = false;
        // There is a bug on the Samsung Galaxy S, so that the instrumentation
        // throws a NullPointerException when generating a DPAD_CENTER on
        // buttons. For this reason on this device we invoke the performClick
        // directly
        if ("GT-I9000".equals(devInf.getDeviceModel())) {
            Activity activity = activityMonitor.getLastActivity();
            if (activity.hasWindowFocus()) {
                View currentView = activity.getCurrentFocus();
                if (currentView != null) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                        if (currentView instanceof Button) {
                            final Button btn = (Button)currentView;
                            for(int i=0; i<count; i++) {
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        btn.performClick();
                                        delay(1000);
                                    }
                                });
                            }
                            done = true;
                        } else if (currentView instanceof Spinner) {
                            final Spinner spn = (Spinner)currentView;
                            for(int i=0; i<count; i++) {
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        spn.performClick();
                                        delay(1000);
                                    }
                                });
                            }
                            done = true;
                        }
                    } 
                }
            }
        }

        if (!done) {
            for(int i=0; i<count; i++) {
                instrumentation.waitForIdleSync();
                instrumentation.sendCharacterSync(keyCode);
                instrumentation.waitForIdleSync();
            }
        }
    }

    public void writeString(String text) throws Throwable {
        for(int i=0;i<text.length();++i) {
            char ch = text.charAt(i);
            instrumentation.waitForIdleSync();
            instrumentation.sendStringSync("" + ch);
            instrumentation.waitForIdleSync();
        }
        delay(500);
    }

    public static void waitDelay(int d) {
        delay(d * 1000);
    }

    public static void removeAccount(Context context) throws Throwable {
        AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(context.getString(R.string.account_type));
        for(int i=0; i<accounts.length; i++) {
            am.removeAccount(accounts[i], null, null);
        }
        SharedPreferences settings = context.getSharedPreferences(
                AndroidConfiguration.KEY_FUNAMBOL_PREFERENCES, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();
    }

    public void waitForSyncToComplete(String sourceName, int minStart, int max,
            AndroidSyncMonitor syncMonitor) throws Throwable {

        Log.debug(TAG_LOG, "waiting for sync to complete for source: " + sourceName);
        
        String authority = ((AndroidAppSyncSource)getAppSyncSource(sourceName))
                .getAuthority();

        // We wait no more than minStart for sync client to start
        while(!syncMonitor.isSyncing()) {
            Thread.sleep(WAIT_DELAY);
            minStart -= WAIT_DELAY;
            if (minStart < 0) {
                throw new ClientTestException("Sync did not start within time limit");
            }
        }

        // Now wait until the busy is in progress for a max amount of time
        while(syncMonitor.isSyncing()) {
            Thread.sleep(WAIT_DELAY);
            max -= WAIT_DELAY;
            if (max < 0) {
                throw new ClientTestException("Sync did not complete before timeout");
            }
        }
    }

    public void waitForAuthToComplete(int minStart, int max,
            SyncMonitor syncMonitor) throws Throwable {
        AndroidController ac = AndroidController.getInstance();
        // Get the AccountScreenController from te global controller
        super.waitForSyncToComplete(minStart, max,
                new AndroidSyncMonitor(ac.getLoginScreenController()));
    }

    public void checkSyncPending(String sourceName, boolean checkPending) throws Throwable {
        AndroidAppSyncSource source = (AndroidAppSyncSource)getAppSyncSource(sourceName);
        String authority = source.getAuthority();

        boolean pending = ContentResolver.isSyncPending(AndroidController.getNativeAccount(), authority);
        boolean active = ContentResolver.isSyncActive(AndroidController.getNativeAccount(), authority);

        Log.debug(TAG_LOG, "Checking pending sync for authority: " + authority);
        Log.debug(TAG_LOG, "Pending sync: " + pending);
        Log.debug(TAG_LOG, "Active sync: " + active);

        boolean isSyncPending = pending;
        if(isSyncPending != checkPending) {
            if(checkPending) {
                throw new ClientTestException("Cannot find pending sync for source: " + sourceName +
                    " with authority: " + authority);
            } else {
                throw new ClientTestException("Found pending sync for source: " + sourceName +
                    " with authority: " + authority);
            }
         }
    }

    public void cancelSync() throws Throwable {
        AndroidHomeScreenController c = (AndroidHomeScreenController)
                AndroidController.getInstance().getHomeScreenController();
        c.cancelMenuSelected();
    }

    public void setAutoSyncEnabled(boolean enabled) throws Throwable {
        ContentResolver.setMasterSyncAutomatically(enabled); 
    }

    public void setSourceAutoSyncEnabled(String sourceName, boolean enabled) throws Throwable {
        AndroidAppSyncSource source = (AndroidAppSyncSource)getAppSyncSource(sourceName);
        String authority = source.getAuthority();
        ContentResolver.setSyncAutomatically(AndroidAccountManager.getNativeAccount(
                instrumentation.getTargetContext()), authority, enabled);
    }

    public void checkSourceAutoSyncEnabled(String sourceName, boolean enabled) throws Throwable {
        AndroidAppSyncSource source = (AndroidAppSyncSource)getAppSyncSource(sourceName);
        String authority = source.getAuthority();
        boolean autoSync = ContentResolver.getSyncAutomatically(AndroidAccountManager.getNativeAccount(
                instrumentation.getTargetContext()), authority);
        if(autoSync != enabled) {
            if(autoSync) {
                throw new ClientTestException("Auto sync is enabled for source: " + sourceName +
                    " with authority: " + authority);
            } else {
                throw new ClientTestException("Auto sync is disabled for source: " + sourceName +
                    " with authority: " + authority);
            }
        }
    }

    public void cancelSyncAfterPhase(String phaseName, int num, int progress,
            SyncMonitor syncMonitor) throws Throwable {
        Log.debug(TAG_LOG, "Preparing to interrupt sync after phase " +
                phaseName + "," + num + "," + progress);
        // Register the listeners to monitor the sync execution
        Enumeration workingSources = appSyncSourceManager.getWorkingSources();
        syncMonitor.cleanListeners();
        while(workingSources.hasMoreElements()) {
            AppSyncSource appSource = (AppSyncSource)workingSources.nextElement();
            SyncSource    source    = appSource.getSyncSource();
            SyncListener  lis       = source.getListener();
            Log.info(TAG_LOG, "Registering monitoring listener for source " + source.getName());
            if (lis != null) {
                // Replace the listener with a monitoring one
                AndroidSyncMonitorListener monLis = new AndroidSyncMonitorListener(lis,
                        AndroidController.getInstance().getHomeScreenController());
                source.setListener(monLis);
                syncMonitor.addListener(monLis);
                Log.info(TAG_LOG, "Monitoring listener registered");
            }
        }
        syncMonitor.interruptSyncAfterPhase(phaseName, num, progress, "Cancel sync");
    }

    public void waitForSyncPhase(String phaseName, int num, int progress,
            int timeout, SyncMonitor syncMonitor) throws Throwable {
        Log.debug(TAG_LOG, "Waiting for sync phase " + phaseName + "," + 
                num + "," + progress);
        final Object syncInterruptedMonitor = new Object();
        synchronized(syncInterruptedMonitor) {
            // Register the listeners to monitor the sync execution
            Enumeration workingSources = appSyncSourceManager.getWorkingSources();
            syncMonitor.cleanListeners();
            while(workingSources.hasMoreElements()) {
                AppSyncSource appSource = (AppSyncSource)workingSources.nextElement();
                SyncSource    source    = appSource.getSyncSource();
                SyncListener  lis       = source.getListener();
                Log.info(TAG_LOG, "Registering monitoring listener for source " + source.getName());
                if (lis != null) {
                    // Replace the listener with a monitoring one
                    SyncMonitorListener monLis = new SyncMonitorListener(lis) {
                        @Override
                        protected void interruptSync() {
                            synchronized(syncInterruptedMonitor) {
                                syncInterruptedMonitor.notifyAll();
                            }
                        }
                    };
                    source.setListener(monLis);
                    syncMonitor.addListener(monLis);
                    Log.info(TAG_LOG, "Monitoring listener registered");
                }
            }
            syncMonitor.interruptSyncAfterPhase(phaseName, num, progress, null);

            // Wait for sync phase to be reached
            syncInterruptedMonitor.wait(timeout);
        }
    }

    public void checkLastAlertMessage(String message) throws Throwable {
        AndroidDisplayManager dm = (AndroidDisplayManager)
                AndroidController.getInstance().getDisplayManager();
        String lastMessage = dm.readAndResetLastMessage();
        assertTrue(lastMessage, message, "Last alert message mismatch");
    }

    private static void delay(int msec) {
        try {
            Thread.sleep(msec);
        } catch (Exception e) {
            Log.error(TAG_LOG, "Exception Occurred while sleeping", e);
        }
    }

    protected Configuration getConfiguration() {
        return AndroidController.getInstance().getConfiguration();
    }

    protected Controller getController() {
        return AndroidController.getInstance();
    }

    protected AppSyncSourceManager getAppSyncSourceManager() {
        return getController().getAppSyncSourceManager();
    }

    protected void startMainApp() throws Throwable {

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(instrumentation.getTargetContext(), AndroidHomeScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Activity activity = instrumentation.startActivitySync(intent);

        android.util.Log.i("getComponentName", activity.getComponentName().getShortClassName());

        if(activity.getComponentName().getShortClassName().equals(
                "com.funambol.android.activities.AndroidHomeScreen")) {

            // Now initialize everything
            AppInitializer initializer = App.i().getAppInitializer();
            AndroidController cont = AndroidController.getInstance(
                    App.i(),
                    new AndroidActivitiesFactory(),
                    initializer.getConfiguration(),
                    initializer.getCustomization(),
                    initializer.getLocalization(),
                    initializer.getAppSyncSourceManager());

            ((AndroidScriptRunner)instrumentation).getBasicScriptRunner().setSyncMonitor(
                    new AndroidSyncMonitor(cont.getHomeScreenController()));

            Robot.waitDelay(3);
            ((AndroidScriptRunner)instrumentation).getBasicScriptRunner().setAuthSyncMonitor(
                    new AndroidSyncMonitor(cont.getLoginScreenController()));

            initialize();

            // At this point we can set the variable that indicates if a contact
            // import shall be performed or not
            setContactsToImportVariable();
        }
    }

    protected void closeMainApp() throws Throwable {
        AndroidAccountManager.hideAllScreensAndDisposeSingletons();
        App.i().disposeAppInitializer();
    }

    private void setContactsToImportVariable() {

        if (vars == null) {
            return;
        }

        final ExternalAccountManager aManager = ExternalAccountManager.getInstance(instrumentation.getTargetContext());

        // This is the first call of contacts import if there aren't any accounts
        // imported and the caller requested a reset
        final boolean isFirstImport = !aManager.accountsImported();
        Vector<Account> accounts = aManager.listContactAccounts(true, true);

        // Filter accounts which don't include any item
        for(int i=0; i<accounts.size(); i++) {
            Account account = accounts.elementAt(i);
            if(aManager.getAccountItemsCount(account) <= 0) {
                accounts.remove(account);
                i--;
            }
        }

        int choicesCount = accounts.size();

        // Include Phone only contacts
        boolean showPhoneOnlyOption = aManager.hasPhoneOnlyItems();
        int phoneOnlyOptionIndex = -1;
        if(showPhoneOnlyOption) {
            phoneOnlyOptionIndex = accounts.size();
            choicesCount++;
        }
        // Include SIM contacts
        boolean showSimOption = aManager.hasSimItems() &&
            !aManager.simImported();
        int simOptionIndex = -1;
        if(showSimOption) {
            choicesCount++;
            if(!showPhoneOnlyOption) {
                simOptionIndex = accounts.size();
            } else {
                simOptionIndex = accounts.size()+1;
            }
        }

        if (choicesCount == 0) {
            // No accounts to be imported
            vars.put("contactsimportrequired", "false");
        } else {
            vars.put("contactsimportrequired", "true");
        }
    }



    private class AndroidSyncMonitorListener extends SyncMonitorListener {

        private SynchronizationController scontroller;
        
        public AndroidSyncMonitorListener(SyncListener lis, SynchronizationController scontroller) {
            super(lis);
            this.scontroller = scontroller;
        }

        /**
         * Override default implementation in order to cancel the synchronization
         * instead of throw an Exception.
         * 
         * @param reason
         */
        @Override
        protected void interruptSync() {
            interruptOnPhase = null;
            receivingPhaseCounter = 0;
            sendingPhaseCounter = 0;
            interruptOnPhaseNumber = -1;
            interruptOnPhaseProgress = -1;
            interruptReason = null;
            scontroller.cancelSync();
        }
    }
}
