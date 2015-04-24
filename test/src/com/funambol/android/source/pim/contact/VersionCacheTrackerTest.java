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

package com.funambol.android.source.pim.contact;

import java.util.Vector;

import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.content.ContentResolver;
import android.net.Uri;

import com.funambol.storage.StringKeyValueSQLiteStore;
import com.funambol.sync.SyncItem;
import com.funambol.sync.ItemStatus;
import com.funambol.sync.SourceConfig;
import com.funambol.sync.SyncSource;

import com.funambol.util.Log;
import com.funambol.util.AndroidLogAppender;

import com.funambol.android.AndroidAppSyncSourceManager;
import com.funambol.android.AndroidBaseTest;
import com.funambol.android.App;
import com.funambol.android.AppInitializer;
import com.funambol.android.UnitTestActivity;
import com.funambol.android.controller.AndroidController;
import com.funambol.client.configuration.Configuration;
import com.funambol.client.source.AppSyncSourceManager;

public class VersionCacheTrackerTest extends AndroidBaseTest {

    private ContentResolver resolver;

    protected ContactSyncSource source;

    private ContactManager cm;

    private Configuration configuration;
    private AppSyncSourceManager sManager;

    protected int id = 0;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        Log.initLog(new AndroidLogAppender("VersionCacheTrackerTest"), Log.TRACE);

        AppInitializer initializer = App.i().getAppInitializer();

        if(AndroidController.getNativeAccount() == null) {
            initializer.init(UnitTestActivity.getInstance());
            try {
                Thread.sleep(5000);
            } catch (Exception e) {}
        }
        
        configuration = initializer.getConfiguration();
        sManager = initializer.getAppSyncSourceManager();
        
        resolver = getContext().getContentResolver();

        cm = new FunambolContactManager(getContext(), false);

        // Init the SyncSource and the tracker
        initSyncSourceTracker();
    }

    @Override
    public void tearDown() {
        Uri.Builder b = RawContacts.CONTENT_URI.buildUpon();
        b.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
        resolver.delete(b.build(), null, null);

        b = ContactsContract.Data.CONTENT_URI.buildUpon();
        b.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
        resolver.delete(b.build(), null, null);
    }
    
    private void initSyncSourceTracker() {

        try {
            SourceConfig config = new SourceConfig("contact", "text/x-vcard", "card");

            StringKeyValueSQLiteStore trackerStore =
                    new StringKeyValueSQLiteStore(getContext(), "funambol.test.db", config.getName());

            trackerStore.load();
            trackerStore.reset();
            trackerStore.save();

            resolver.delete(RawContacts.CONTENT_URI, null, null);

            VersionCacheTracker tracker = new VersionCacheTracker(trackerStore, getContext(), null);

            source = new ContactSyncSource(config, tracker, getContext(),
                    configuration, sManager.getSource(AndroidAppSyncSourceManager.CONTACTS_ID),
                    new FunambolContactManager(getContext()));

        } catch(Exception ex) {
            ex.toString();
            Log.error("Exception in initSyncSourceTracker: " + ex.toString());
        }
    }

    public void testSimpleFasts() throws Exception {
        testAddedNewItems();
        testUpdatedItems();
        testDeletedItems();
        testAddCommand();
        testReplaceCommand();
        testDeleteCommand();
    }

    public void testMultipleChanges() throws Exception {

        String lastId  = "";
        String firstId = "";

        // Prepopulate the source with six items
        for(int i=0;i<6;++i) {
            lastId = addContactFromOutside("name-" + id++);
            if(i==0) {firstId = lastId; }
        }

        source.beginSync(SyncSource.FULL_SYNC, false);
        Vector itemsStatus = new Vector();
        for(int i=0;i<6;++i) {
            SyncItem nextItem = source.getNextItem();
            assertTrue(nextItem != null);
            itemsStatus.addElement(new ItemStatus(nextItem.getKey(), SyncSource.SUCCESS_STATUS));
        }
        assertTrue(source.getNextItem() == null);
        source.applyItemsStatus(itemsStatus);
        source.endSync();

        String new1 = addContactFromOutside("name-" + id++);
        String new2 = addContactFromOutside("name-" + id++);
        updateContactFromOutside(firstId, "new name");
        deleteContactFromOutside(lastId);

        source.beginSync(SyncSource.INCREMENTAL_SYNC, false);
        SyncItem newItem1 = source.getNextNewItem();
        SyncItem newItem2 = source.getNextNewItem();
        
        assertTrue(newItem1 != null);
        assertTrue(newItem2 != null);

        String key1 = newItem1.getKey();
        String key2 = newItem2.getKey();

        if(key1.equals(new1)) {
            assertEquals(key2, new2);
        } else if(key1.equals(new2)) {
            assertEquals(key2, new1);
        } else {
            fail();
        }

        itemsStatus = new Vector();
        itemsStatus.addElement(new ItemStatus(key1, SyncSource.SUCCESS_STATUS));
        itemsStatus.addElement(new ItemStatus(key2, SyncSource.SUCCESS_STATUS));
        source.applyItemsStatus(itemsStatus);

        SyncItem updated = source.getNextUpdatedItem();
        assertTrue(updated != null);
        assertEquals(updated.getKey(), firstId);

        itemsStatus = new Vector();
        itemsStatus.addElement(new ItemStatus(firstId, SyncSource.SUCCESS_STATUS));
        source.applyItemsStatus(itemsStatus);

        SyncItem deleted = source.getNextDeletedItem();
        assertTrue(deleted != null);
        assertEquals(deleted.getKey(), lastId);

        itemsStatus = new Vector();
        itemsStatus.addElement(new ItemStatus(lastId, SyncSource.SUCCESS_STATUS));
        source.applyItemsStatus(itemsStatus);
        
        assertTrue(source.getNextNewItem() == null);
        assertTrue(source.getNextUpdatedItem() == null);
        assertTrue(source.getNextDeletedItem() == null);
        
        source.endSync();
    }

    private void testAddCommand() throws Exception {
        Log.trace("testAddCommand");

        // Begin a new sync
        source.beginSync(SyncSource.INCREMENTAL_SYNC, false);

        // Now receives an add command
        SyncItem newItem = new SyncItem(""+(id++));
        newItem.setState(SyncItem.STATE_NEW);
        newItem.setContent(getSampleVCard("name-" + id, ""));
        Vector items = new Vector();
        items.addElement(newItem);
        source.applyChanges(items);

        // Now check that there are no changes detected
        assertTrue(source.getNextNewItem() == null);
        assertTrue(source.getNextUpdatedItem() == null);
        assertTrue(source.getNextDeletedItem() == null);
        source.endSync();
    }

    private void testAddedNewItems() throws Exception {
        Log.trace("testAddedNewItems");

        // Add a new item from "outside"
        String key = addContactFromOutside("name-" + id++);

        source.beginSync(SyncSource.INCREMENTAL_SYNC, false);
        // Now check that there is one new item reported
        SyncItem newItem = source.getNextNewItem();
        assertTrue(newItem != null);

        assertEquals(newItem.getKey(), key);

        assertTrue(source.getNextNewItem() == null);
        assertTrue(source.getNextUpdatedItem() == null);
        assertTrue(source.getNextDeletedItem() == null);

        Vector itemsStatus = new Vector();
        itemsStatus.addElement(new ItemStatus(key, SyncSource.SUCCESS_STATUS));
        source.applyItemsStatus(itemsStatus);
        source.endSync();
    }

    private void testUpdatedItems() throws Exception {
        Log.trace("testUpdatedItems");

        String key = addContactFromOutside("name-" + id++);
        source.beginSync(SyncSource.INCREMENTAL_SYNC, false);
        Vector itemsStatus = new Vector();
        itemsStatus.addElement(new ItemStatus(key, SyncSource.SUCCESS_STATUS));
        source.applyItemsStatus(itemsStatus);
        source.endSync();

        // Update item in the store
        updateContactFromOutside(key, "new name");

        // Begin a new sync
        source.beginSync(SyncSource.INCREMENTAL_SYNC, false);

        // Now check that there is only one updated item
        SyncItem updItem = source.getNextUpdatedItem();
        assertTrue(updItem != null);
        assertTrue((key+"").equals(updItem.getKey()));

        assertTrue(source.getNextUpdatedItem() == null);
        assertTrue(source.getNextNewItem() == null);
        assertTrue(source.getNextUpdatedItem() == null);
        assertTrue(source.getNextDeletedItem() == null);
        itemsStatus = new Vector();
        itemsStatus.addElement(new ItemStatus(key, SyncSource.SUCCESS_STATUS));
        source.applyItemsStatus(itemsStatus);
        source.endSync();
    }

    private void testDeletedItems() throws Exception {
        Log.trace("testDeletedItems");

        String key = addContactFromOutside("name-" + id++);
        source.beginSync(SyncSource.INCREMENTAL_SYNC, false);
        Vector itemsStatus = new Vector();
        itemsStatus.addElement(new ItemStatus(key, SyncSource.SUCCESS_STATUS));
        source.applyItemsStatus(itemsStatus);
        source.endSync();
        
        deleteContactFromOutside(key);
        
        // Begin a new sync
        source.beginSync(SyncSource.INCREMENTAL_SYNC, false);
        // Now check that there are is one deleted item
        SyncItem delItem = source.getNextDeletedItem();
        assertTrue(delItem != null);
        assertTrue(key.equals(delItem.getKey()));
        assertTrue(source.getNextNewItem() == null);
        assertTrue(source.getNextUpdatedItem() == null);
        assertTrue(source.getNextDeletedItem() == null);
        itemsStatus = new Vector();
        itemsStatus.addElement(new ItemStatus(key, SyncSource.SUCCESS_STATUS));
        source.applyItemsStatus(itemsStatus);
        source.endSync();
    }

    private void testReplaceCommand() throws Exception {
        Log.trace("testReplaceCommand");

        String key = addContactFromOutside("name-" + id++);
        source.beginSync(SyncSource.INCREMENTAL_SYNC, false);
        Vector itemsStatus = new Vector();
        itemsStatus.addElement(new ItemStatus(key, SyncSource.SUCCESS_STATUS));
        source.applyItemsStatus(itemsStatus);
        source.endSync();
        
        source.beginSync(SyncSource.INCREMENTAL_SYNC, false);
        SyncItem item = new SyncItem(key);
        item.setState(SyncItem.STATE_UPDATED);
        item.setContent(getSampleVCard("updated", "000000"));
        Vector items = new Vector();
        items.addElement(item);
        source.applyChanges(items);
        
        // Now check that there are no changes detected
        assertTrue(source.getNextNewItem() == null);
        assertTrue(source.getNextUpdatedItem() == null);
        assertTrue(source.getNextDeletedItem() == null);
        source.endSync();
    }

    private void testDeleteCommand() throws Exception {
        Log.trace("testDeleteCommand");

        String key = addContactFromOutside("name-" + id++);
        source.beginSync(SyncSource.INCREMENTAL_SYNC, false);
        Vector itemsStatus = new Vector();
        itemsStatus.addElement(new ItemStatus(key, SyncSource.SUCCESS_STATUS));
        source.applyItemsStatus(itemsStatus);
        source.endSync();
        
        // Begin a new sync
        source.beginSync(SyncSource.INCREMENTAL_SYNC, false);
        // Now receives a delete command
        SyncItem item = new SyncItem(key);
        item.setState(SyncItem.STATE_DELETED);
        Vector items = new Vector();
        items.addElement(item);
        source.applyChanges(items);

        // Now check that there are no changes detected
        assertTrue(source.getNextNewItem() == null);
        assertTrue(source.getNextUpdatedItem() == null);
        assertTrue(source.getNextDeletedItem() == null);
        source.endSync();
    }

    public void testChangesDuringSync1() throws Exception {

        Log.debug("Test Changes During Sync #1");

        // Prepopulate the source with six items
        for(int i=0;i<6;++i) {
            Log.trace("Add item " + i);
            addContactFromOutside("name-" + id++);
        }

        Log.trace("Perform slow sync");
        // Begin the sync and exchange all items
        source.beginSync(SyncSource.FULL_SYNC, false);

        Vector itemsStatus = new Vector();
        for(int i=0;i<6;++i) {
            Log.trace("Check item " + i);
            SyncItem nextItem = source.getNextItem();
            assertTrue(nextItem != null);
            itemsStatus.addElement(new ItemStatus(nextItem.getKey(), SyncSource.SUCCESS_STATUS));
        }
        source.applyItemsStatus(itemsStatus);
        Log.trace("Simulate user changes in the middle of the sync");
        // Simulate user changes in the middle of the sync
        String id1 = addContactFromOutside("name-" + id++);
        String id2 = addContactFromOutside("name-" + id++);
        
        // Now check that no items are reported as new items
        assertTrue(source.getNextItem() == null);
        source.endSync();

        // Start a fast sync and check that changed made during the previous
        // sync are detected
        source.beginSync(SyncSource.INCREMENTAL_SYNC, false);

        SyncItem added1 = source.getNextNewItem();
        SyncItem added2 = source.getNextNewItem();

        assertTrue(added1 != null);
        assertTrue(added2 != null);

        String key1 = added1.getKey();
        String key2 = added2.getKey();

        if(key1.equals(id1)) {
            assertEquals(key2, id2);
        } else if(key1.equals(id2)) {
            assertEquals(key2, id1);
        } else {
            fail();
        }
        source.endSync();
    }

    private byte[] getSampleVCard(String name, String phone) {
        return ("BEGIN:VCARD\r\n" +
                "VERSION:2.1\r\n" +
                "N:" + name + ";;;;\r\n" +
                "TEL;CELL:" + phone + "\r\n" +
                "TEL;FAX:123456\r\n" +
                "END:VCARD").getBytes();
    }

    private String addContactFromOutside(String name) {
        try {
            Contact c = new Contact();
            c.setVCard(getSampleVCard(name, ""));
            cm.beginTransaction();
            cm.add(c);
            Vector keys = cm.commit();
            String id = (String)keys.elementAt(0);
            return id;
        } catch(Exception ex) {
            return "";
        }
    }

    private void updateContactFromOutside(String key, String newName) {
        try {
            Contact c = new Contact();
            c.setVCard(getSampleVCard(newName, "000000000"));
            cm.beginTransaction();
            cm.update(key, c);
            cm.commit();
        } catch(Exception ex) { }
    }

    private void deleteContactFromOutside(String key) {
        try {
            cm.beginTransaction();
            cm.delete(key);
            cm.commit();
        } catch(Exception ex) { }
    }
}
