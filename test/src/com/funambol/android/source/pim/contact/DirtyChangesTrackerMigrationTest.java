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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import com.funambol.util.Log;
import com.funambol.util.AndroidLogAppender;
import com.funambol.android.AndroidBaseTest;
import com.funambol.android.AndroidCustomization;
import com.funambol.android.App;
import com.funambol.android.AppInitializer;
import com.funambol.android.IntKeyValueSQLiteStore;
import com.funambol.android.UnitTestActivity;
import com.funambol.android.controller.AndroidController;
import com.funambol.android.source.AndroidChangesTracker;
import com.funambol.androidsync.R;
import com.funambol.sync.ItemStatus;
import com.funambol.sync.SourceConfig;
import com.funambol.sync.SyncSource;
import com.funambol.sync.client.ChangesTracker;
import java.util.Enumeration;
import java.util.Vector;


public class DirtyChangesTrackerMigrationTest extends AndroidBaseTest {

    private static final String TAG_LOG = "DirtyChangesTrackerMigrationTest";
     
    private ContentResolver resolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Log.initLog(new AndroidLogAppender("DirtyChangesTrackerMigrationTest"), Log.INFO);

        AppInitializer initializer = App.i().getAppInitializer();

        if(AndroidController.getNativeAccount() == null) {
            initializer.init(UnitTestActivity.getInstance());
            try {
                Thread.sleep(5000);
            } catch (Exception e) {}
        }

        resolver = getContext().getContentResolver();

        deleteAllContactsData();
    }

    @Override
    public void tearDown() {
        deleteAllContactsData();
    }

    private void deleteAllContactsData() {
        Uri.Builder b = RawContacts.CONTENT_URI.buildUpon();
        b.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
        resolver.delete(b.build(), null, null);

        b = ContactsContract.Data.CONTENT_URI.buildUpon();
        b.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
        resolver.delete(b.build(), null, null);
    }

    public void testMigrateToDirtyChangesTracker() throws Exception {

        AccountManager am = AccountManager.get(getContext());
        Account[] accounts = am.getAccountsByType(getContext().getString(
                R.string.account_type));
        
        assertEquals(accounts.length, 1);

        Account account = accounts[0];

        ContentValues cv = new ContentValues();
        cv.put(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type);
        cv.put(ContactsContract.RawContacts.ACCOUNT_NAME, account.name);

        Vector itemsToBeUpdated = new Vector();
        for(int i=0; i<10; i++) {
            Uri uri = resolver.insert(ContactsContract.RawContacts.CONTENT_URI, cv);
            itemsToBeUpdated.addElement(uri.getLastPathSegment());
        }
        Vector itemsToBeDeleted = new Vector();
        for(int i=0; i<10; i++) {
            Uri uri = resolver.insert(ContactsContract.RawContacts.CONTENT_URI, cv);
            itemsToBeDeleted.addElement(uri.getLastPathSegment());
        }

        ContactManager cm = new FunambolContactManager(getContext());
        IntKeyValueSQLiteStore trackerStore = new IntKeyValueSQLiteStore(getContext(),
            AndroidCustomization.getInstance().getFunambolSQLiteDbName(),
            "fake");

        AndroidChangesTracker oldTracker = new VersionCacheTracker(
                trackerStore, getContext(), cm);
        oldTracker.begin(SyncSource.INCREMENTAL_SYNC, true);
        Enumeration news = oldTracker.getNewItems();
        Vector<ItemStatus> status = new Vector<ItemStatus>();
        while(news.hasMoreElements()) {
            String key = (String)news.nextElement();
            status.addElement(new ItemStatus(key, SyncSource.SUCCESS_STATUS));
        }
        oldTracker.setItemsStatus(status);
        oldTracker.end();
        
        // Add 10 contacts
        for(int i=0; i<10; i++) {
            resolver.insert(ContactsContract.RawContacts.CONTENT_URI, cv);
        }
        // Update 10 contacts
        ContentValues newCV = new ContentValues();
        newCV.put(ContactsContract.RawContacts.VERSION, 8);
        for(int i=0; i<itemsToBeUpdated.size(); i++) {
            resolver.update(ContactsContract.RawContacts.CONTENT_URI, newCV,
                    ContactsContract.RawContacts._ID+"="+itemsToBeUpdated.elementAt(i), null);
        }
        // Delete 10 contacts
        for(int i=0; i<itemsToBeDeleted.size(); i++) {
            resolver.delete(ContactsContract.RawContacts.CONTENT_URI,
                    ContactsContract.RawContacts._ID+"="+itemsToBeDeleted.elementAt(i), null);
        }

        long time = System.currentTimeMillis();
        ContactSyncSource.migrateToDirtyChangesTracker(
                new SourceConfig("fake", "fake", "fake"), cm, getContext());
        long elapsed = System.currentTimeMillis() - time;
        
        ChangesTracker newTracker = new DirtyChangesTracker(getContext(), cm);
        newTracker.begin(SyncSource.INCREMENTAL_SYNC, true);
        Enumeration newItems = newTracker.getNewItems();
        Enumeration updItems = newTracker.getUpdatedItems();
        Enumeration delItems = newTracker.getDeletedItems();
        newTracker.end();

        int newItemsCount = 0;
        while(newItems.hasMoreElements()) {
            newItems.nextElement();
            newItemsCount++;
        }
        int updItemsCount = 0;
        while(updItems.hasMoreElements()) {
            updItems.nextElement();
            updItemsCount++;
        }
        int delItemsCount = 0;
        while(delItems.hasMoreElements()) {
            delItems.nextElement();
            delItemsCount++;
        }
        assertEquals(newItemsCount, 10);
        assertEquals(updItemsCount, 10);
        assertEquals(delItemsCount, 10);

        Log.info(TAG_LOG, "Tracker migration took " + elapsed + " milliseconds");
    }
}
