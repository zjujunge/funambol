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
package com.funambol.android;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import com.funambol.android.controller.AndroidHomeScreenController;

import com.funambol.client.configuration.Configuration;
import com.funambol.client.customization.Customization;
import com.funambol.client.controller.SynchronizationController;
import com.funambol.client.source.AppSyncSource;
import com.funambol.client.source.AppSyncSourceManager;
import com.funambol.client.push.MessageParserException;
import com.funambol.client.push.SANMessageParser;
import com.funambol.client.push.SyncInfo;
import com.funambol.util.Log;

/**
 * Used to be notified when SMS messages are received.
 * 
 * This receiver is registered thru the application manifest, so there isn't a way
 * to unregister it if the SMS or WAP push is disabled in the settings.
 * 
 * Check also the WAP_PUSH_MIME_TYPE of the message both in the manifest and in
 * the code to be sure to listen for the right WAP message
 */
public class SmsReceiver extends BroadcastReceiver {
    
    //---------- Private fields
    private static final String TAG_LOG = "SmsReceiver";
    

    //---------- Constructor

    
    //---------- Public properties
    /** Intent actions */
    public static final String ACTION_DATA_SMS_RECEIVED = "android.intent.action.DATA_SMS_RECEIVED";
    public static final String ACTION_SMS_RECEIVED      = "android.provider.Telephony.SMS_RECEIVED";
    public static final String ACTION_WAP_PUSH_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";
    public static final String WAP_PUSH_MIME_TYPE       = "application/vnd.syncml.ds.notification";

    //---------- Events
    @Override
    public void onReceive(Context context, Intent intent) {

        //check if the SMS must be managed
        Log.info(TAG_LOG, "Check if the SMS recevier must process the incoming SMS");
        AppInitializer appInitializer = App.i().getAppInitializer();

        Configuration configuration = appInitializer.getConfiguration();

        boolean ignorePush = false;

        // If the App has not been initialized yet we ignore this push message
        if (configuration == null) {
            Log.info(TAG_LOG, "Ignoring sms push message because the application has not started yet");
            ignorePush = true;
        }

        // If the user is currently logged out, we ignore the message
        if (configuration.getCredentialsCheckPending()) {
            Log.info(TAG_LOG, "Ignoring sms push message because the user is not logged in");
            ignorePush = true;
        }

        // Check if the client is configured in manual/scheduled mode
        if (configuration.getSyncMode() != Configuration.SYNC_MODE_PUSH) {
            Log.info(TAG_LOG, "Ignoring sms push message because the client is not in push mode");
            ignorePush = true;
        }

        // Check if the message is received on the right port (if we cannot
        // extract the port, we simply ignore it)
        if (intent.getData() != null) {
            int    port   = intent.getData().getPort();
            Customization customization = appInitializer.getCustomization();
            if (port != customization.getS2CPushSmsPort()) {
                Log.info(TAG_LOG, "Ignoring sms push message because it was directed to an unknown port");
                ignorePush = true;
            }
        }

        if (ignorePush) {
            abortBroadcast();
            return;
        }
        
        //kind of message arrived
        String action = intent.getAction();
        String type   = intent.getType();
        
        // Processes WAP PUSH
        if (ACTION_WAP_PUSH_RECEIVED.equals(action) &&
                   WAP_PUSH_MIME_TYPE.equals(type)) {
            handleWapPushReceived(context, intent);
            
        // Processes data SMS
        } else if (ACTION_DATA_SMS_RECEIVED.equals(action)) {
            handleWapPushReceived(context, intent);
            
        // Processes what?
        } else {
            Log.debug(TAG_LOG, "Found unhandled action: " + action);
        }
        abortBroadcast();
    }

    protected void handleWapPushReceived(Context context, Intent intent) {
        Log.info(TAG_LOG, "handleWapPushReceived");

        byte[] msg = getPushMessageFromIntent(intent);
        SANMessageParser parser = new SANMessageParser();
        try {
            parser.parseMessage(msg, false);
            if (checkIfWapPushCanTriggerASync(parser)) {
                Vector<AppSyncSource> sources = getSourcesFromWapPush(parser);
                startSync(context, sources);
            } else {
                Log.debug(TAG_LOG, "No good WAP push message for launching a sync");
            }
        } catch (MessageParserException e) {
            parser = null;
            Log.error(TAG_LOG, "Cannot parse wap push message", e);
        }
    }
     
    /**
     * Retrieves the sync source to sync based on WAP push message content
     * 
     * @param parser
     * @return a Vector with the sync source to sync
     */
    @SuppressWarnings("unchecked")
    private Vector<AppSyncSource> getSourcesFromWapPush(SANMessageParser parser) {
        Vector<AppSyncSource> sources = new Vector<AppSyncSource>();
        AppInitializer appInitializer = App.i().getAppInitializer();
        AppSyncSourceManager sourceManager = appInitializer.getAppSyncSourceManager();
        Enumeration rawSources = sourceManager.getEnabledAndWorkingSources();
        
        while(rawSources.hasMoreElements()) {
            AppSyncSource source = (AppSyncSource) rawSources.nextElement();
            
            //compare the enabled and working source with the ones requested in the WAP push
            for (SyncInfo syncInfo : parser.getSyncInfoArray()) {
                if (source.getSyncSource().getSourceUri().equals(syncInfo.getServerUri())) {
                    sources.add(source);
                }
            }
        }
        return sources;
    }

    protected final SmsMessage[] getMessagesFromIntent(Intent intent) {
        Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
        if (messages == null || messages.length == 0) {
            return null;
        }
        byte[][] pduObjs = new byte[messages.length][];
        for (int i = 0; i < messages.length; i++) {
            pduObjs[i] = (byte[]) messages[i];
        }
        byte[][] pdus = new byte[pduObjs.length][];
        int pduCount = pdus.length;
        SmsMessage[] msgs = new SmsMessage[pduCount];
        for (int i = 0; i < pduCount; i++) {
            pdus[i] = pduObjs[i];
            msgs[i] = SmsMessage.createFromPdu(pdus[i]);
        }
        return msgs;
    }
    
    /*
     * Extract push data from raw PDUs as if this were a normal WAP push.
     * If Android didn't filter out DS notification WAP push messages, all this
     * low-level computing would be done by Android internal contraptions.
     */
    protected final byte[] getDataFromPdus(Object[] pdus) {
        
        // This is refactored from com.android.internal.telephony.WapPushOverSms    
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (Object pdu : pdus) {
            SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
            byte[] data = msg.getUserData();
            output.write(data, 0, data.length);
        }
        byte[] pdu = output.toByteArray();
        int index = 0;
        int transactionId = pdu[index++] & 0xFF; // not used
        int pduType = pdu[index++] & 0xFF;
        int headerLength = 0;
        if ((pduType != 0x06 && // WspTypeDecoder.PDU_TYPE_PUSH
            (pduType != 0x07))) { // WspTypeDecoder.PDU_TYPE_CONFIRMED_PUSH
            Log.info(TAG_LOG, "Received non-PUSH WAP PDU. Type = " + pduType);
            return null;
        }
        
        // This is refactored from com.android.internal.telephony.WspTypeDecoder
        int decodeIndex = index;
        long unsigned32bit = 0;
        while ((pdu[index] & 0x80) != 0) {
            if ((decodeIndex - index) >= 4) {
                Log.info(TAG_LOG, "Received PDU. Header Length error.");
                return null;
            }
            unsigned32bit = (unsigned32bit << 7) | (pdu[index] & 0x7f);
            index++;
        }
        unsigned32bit = (unsigned32bit << 7) | (pdu[index] & 0x7f);
        int decodedDataLength = decodeIndex - index + 1;        
        
        // Another bit from com.android.internal.telephony.WapPushOverSms    
        headerLength = (int) unsigned32bit;
        int headerStartIndex = index + decodedDataLength;
        int dataIndex = headerStartIndex + headerLength;
        byte[] data = new byte[pdu.length - dataIndex];
        System.arraycopy(pdu, dataIndex, data, 0, data.length);
        return data;
    }
    
    protected final byte[] getPushMessageFromIntent(Intent intent) {
        Bundle b = intent.getExtras();        
        byte[] data = b.getByteArray("data");

        if (data == null) { // data SMS, needs to dig into it
            data = getDataFromPdus((Object[])b.get("pdus"));
            
        } else { // WAP push already processed by Android OS
            // Checks PDU Type (0x06 = PUSH)
            int type = b.getInt("pduType");            
            if (type != 0x06) {
                Log.error(TAG_LOG, "Invalid PDU type: " + type);
                return null;
            }
        }
        return data;
    }

    /**
     * Verifies if the body of received message is the one than can trigger a sync
     * @return
     */
    protected boolean checkIfWapPushCanTriggerASync(SANMessageParser parser) {
        if (null == parser) {
            return false;
        }

        String serverId = parser.getServerId();
        Log.info(TAG_LOG, "Server id " + serverId);
        if (TextUtils.isEmpty(serverId)) {
            return false;
        }
        
        return true;
    }

    /**
     * Starts a sync of selected sync sources
     * 
     * @param context
     * @param sources List of source to sync, if null sync all sync sources
     */
    protected void startSync(Context context, Vector<AppSyncSource> sources) {
        AppInitializer appInitializer = App.i().getAppInitializer();
        AndroidHomeScreenController homeController = (AndroidHomeScreenController)
            appInitializer.getController().getHomeScreenController();
        if (null == sources) {
            Log.info(TAG_LOG, "Start a complete sync invoked via WAP Push");

            if (homeController.isSynchronizing()) {
                Log.info(TAG_LOG, "A sync is already running, enqueu the request");
                homeController.enquePushSyncRequest();
            } else {
                homeController.syncAllSources(SynchronizationController.PUSH);
            }
        } else if (sources.size() > 0) {
            Log.info(TAG_LOG, "Start a selective sync invoked via WAP Push");
            if (homeController.isSynchronizing()) {
                homeController.enquePushSyncRequest(sources);
            } else {
                homeController.synchronize(SynchronizationController.PUSH, sources);
            }
        } else {
            Log.debug(TAG_LOG, "No enabled and working sync sources to sync");
        }
    }
}
