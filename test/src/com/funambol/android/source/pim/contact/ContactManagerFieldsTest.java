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

import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.funambol.util.Log;
import com.funambol.util.StringUtil;

import com.funambol.common.pim.model.contact.Note;

public class ContactManagerFieldsTest extends ContactManagerBaseTest {

    public void testAddLoad() throws Exception {

        Contact c = new Contact();

        Vector suppFields = cm.getSupportedFields();

        byte[] vcard = getSampleVCard(suppFields, suppFields, true);
        
        assertEquals(getVCardFieldsCount(vcard), suppFields.size()+4);

        c.setVCard(vcard);

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        // Load the same contact
        c = cm.load(id);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());
        
        byte[] result = os.toByteArray();

        assertVCardEquals(result, vcard);
    }

    public void testUpdate() throws Exception {

        Contact c = new Contact();
        
        Vector suppFields = cm.getSupportedFields();

        byte[] vcard = getSampleVCard(suppFields, suppFields, true);
        byte[] updvcard = getSampleUpdatedVCard(suppFields, suppFields, true);

        c.setVCard(vcard);

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        c = new Contact();
        c.setVCard(updvcard);

        cm.beginTransaction();
        cm.update(id, c);
        cm.commit();

        // Load the same contact
        c = cm.load(id);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        byte[] result = os.toByteArray();

        assertVCardEquals(result, updvcard);
    }

    public void testUpdate_EmptyFields() throws Exception {

        Contact c = new Contact();

        Vector suppFields = cm.getSupportedFields();

        int suppFieldsSize = suppFields.size();
        assertTrue(suppFieldsSize > 0);

        Vector subFields = (Vector)suppFields.clone();

        byte[] vcard = getSampleVCard(suppFields, suppFields, true);

        c.setVCard(vcard);

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        byte[] updvcard;
        for(int i=0; i<suppFields.size()+1; i++) {

            assertEquals(suppFieldsSize, suppFields.size());

            updvcard = getSampleUpdatedVCard(subFields, suppFields, true);

            Log.trace("updated vcard: " + new String(updvcard));

            Contact upd = new Contact();
            upd.setVCard(updvcard);

            cm.beginTransaction();
            cm.update(id, upd);
            cm.commit();

            // Load the same contact
            c = cm.load(id);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            c.toVCard(os, cm.getSupportedFields());

            byte[] result = os.toByteArray();

            assertVCardEquals(result, updvcard);

            // Remove the last field
            if(!subFields.isEmpty()) subFields.remove(subFields.size()-1);
        }
    }

    public void testUpdate_NullFields() throws Exception {

        Vector suppFields = cm.getSupportedFields();

        int suppFieldsSize = suppFields.size();
        assertTrue(suppFieldsSize > 0);

        Vector subFields = (Vector)suppFields.clone();

        for(int i=0; i<suppFields.size()+1; i++) {

            assertEquals(suppFieldsSize, suppFields.size());
            
            byte[] vcard       = getSampleVCard(suppFields, suppFields, true);
            byte[] updvcard    = getSampleUpdatedVCard(subFields, suppFields, false);
            byte[] mergedvcard = getSampleMergedVCard(subFields, suppFields);

            Log.trace("vcard: " + new String(vcard));
            Log.trace("updated vcard: " + new String(updvcard));
            Log.trace("merged vcard: "  + new String(mergedvcard));

            Contact c = new Contact();
            c.setVCard(vcard);

            // Add the new contact
            cm.beginTransaction();
            cm.add(c);
            Vector keys = cm.commit();
            String id = (String)keys.elementAt(0);

            Contact upd = new Contact();
            upd.setVCard(updvcard);

            cm.beginTransaction();
            cm.update(id, upd);
            cm.commit();

            // Load the same contact
            c = cm.load(id);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            c.toVCard(os, cm.getSupportedFields());

            byte[] result = os.toByteArray();

            assertVCardEquals(result, mergedvcard);

            // Remove the last field
            if(!subFields.isEmpty()) subFields.remove(subFields.size()-1);
        }
    }

    public void testDelete() throws Exception {

        Contact c = new Contact();

        Vector suppFields = cm.getSupportedFields();

        byte[] vcard = getSampleVCard(suppFields, suppFields, true);
        assertEquals(getVCardFieldsCount(vcard), suppFields.size()+4);

        c.setVCard(vcard);

        String[] ids = new String[5];

        // Add some contacts
        cm.beginTransaction();
        for(int i=0; i<ids.length; i++) {
            cm.add(c);
        }
        Vector keys = cm.commit();
        for(int i=0;i<ids.length;++i) {
            ids[i] = (String)keys.elementAt(i);
        }

        // Check if they exist
        for(int i=0; i<ids.length; i++) {
            assertTrue(cm.exists(ids[i]));
        }

        // Delete all
        cm.beginTransaction();
        for(int i=0; i<ids.length; i++) {
            cm.delete(ids[i]);
        }
        cm.commit();

        // Check if they exist
        for(int i=0; i<ids.length; i++) {
            assertTrue(!cm.exists(ids[i]));

            //Ensure there's no more rows in RawContacts table
            Cursor cur = resolver.query(ContactsContract.RawContacts.CONTENT_URI, null,
                    ContactsContract.RawContacts._ID+"="+ids[i],
                    null, null);

            assertTrue(cur.getCount() == 0);
            
            //Ensure there's no more rows in Data table
            cur = resolver.query(ContactsContract.Data.CONTENT_URI, null,
                    ContactsContract.Data.RAW_CONTACT_ID+"="+ids[i],
                    null, null);

            assertTrue(cur.getCount() == 0);
        }
    }

    public void testGetAllKeys() throws Exception {

        Contact c = new Contact();

        Vector suppFields = cm.getSupportedFields();

        byte[] vcard = getSampleVCard(suppFields, suppFields, true);
        assertEquals(getVCardFieldsCount(vcard), suppFields.size()+4);

        c.setVCard(vcard);

        String[] ids = new String[5];
        // Add some contacts
        cm.beginTransaction();
        for(int i=0; i<ids.length; i++) {
            cm.add(c);
        }
        Vector keys = cm.commit();
        for(int i=0;i<ids.length;++i) {
            ids[i] = (String)keys.elementAt(i);
        }

        // Check if they exist
        for(int i=0; i<ids.length; i++) {
            assertTrue(cm.exists(ids[i]));
        }

        boolean[] found = new boolean[ids.length];
        for(int i=0; i<found.length; i++) {
            found[i] = false;
        }
        Enumeration en = cm.getAllKeys();
        int count = 0;
        while(en.hasMoreElements()) {
            count++;
            String key = (String)en.nextElement();
            for(int i=0; i<ids.length; i++) {
                if(key.equals(ids[i])) {
                    found[i] = true;
                    break;
                }
            }
        }
        assertEquals(count, ids.length);
        for(int i=0; i<found.length; i++) {
            assertTrue(found[i]);
        }
    }

    public void testExists() throws Exception {

        Contact c = new Contact();

        Vector suppFields = cm.getSupportedFields();

        byte[] vcard = getSampleVCard(suppFields, suppFields, true);
        c.setVCard(vcard);

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);
        assertTrue(cm.exists(id));

        cm.beginTransaction();
        cm.delete(id);
        cm.commit();
        assertTrue(!cm.exists(id));
    }
    
    public void testNullFields() throws Exception {
        Log.info("testNullFields start");

        ContentValues cv = new ContentValues();
        cv.put(ContactsContract.RawContacts.ACCOUNT_TYPE, "");
        cv.put(ContactsContract.RawContacts.ACCOUNT_NAME, "");
        Uri newItem = resolver.insert(ContactsContract.RawContacts.CONTENT_URI, cv);

        long id = Long.parseLong(newItem.getLastPathSegment());

        cv = new ContentValues();
        cv.put(ContactsContract.Data.RAW_CONTACT_ID, id);
        cv.put(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        cv.put(ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Email.TYPE_WORK);
        cv.put(ContactsContract.CommonDataKinds.Email.DATA, (String)null);
        resolver.insert(ContactsContract.Data.CONTENT_URI, cv);

        cv = new ContentValues();
        cv.put(ContactsContract.Data.RAW_CONTACT_ID, id);
        cv.put(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);
        cv.put(ContactsContract.CommonDataKinds.Event.TYPE,
                ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY);
        cv.put(ContactsContract.CommonDataKinds.Event.DATA, (String)null);
        resolver.insert(ContactsContract.Data.CONTENT_URI, cv);

        cv = new ContentValues();
        cv.put(ContactsContract.Data.RAW_CONTACT_ID, id);
        cv.put(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);
        cv.put(ContactsContract.CommonDataKinds.Im.TYPE,
                ContactsContract.CommonDataKinds.Im.TYPE_HOME);
        cv.put(ContactsContract.CommonDataKinds.Im.PROTOCOL,
                ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM);
        cv.put(ContactsContract.CommonDataKinds.Im.DATA, (String)null);
        resolver.insert(ContactsContract.Data.CONTENT_URI, cv);

        cv = new ContentValues();
        cv.put(ContactsContract.Data.RAW_CONTACT_ID, id);
        cv.put(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
        cv.put(ContactsContract.CommonDataKinds.Nickname.TYPE,
                ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT);
        cv.put(ContactsContract.CommonDataKinds.Nickname.NAME, (String)null);
        resolver.insert(ContactsContract.Data.CONTENT_URI, cv);

        cv = new ContentValues();
        cv.put(ContactsContract.Data.RAW_CONTACT_ID, id);
        cv.put(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        cv.put(ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        cv.put(ContactsContract.CommonDataKinds.Phone.NUMBER, (String)null);
        resolver.insert(ContactsContract.Data.CONTENT_URI, cv);

        cv = new ContentValues();
        cv.put(ContactsContract.Data.RAW_CONTACT_ID, id);
        cv.put(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
        cv.put(ContactsContract.CommonDataKinds.Website.TYPE,
                ContactsContract.CommonDataKinds.Website.TYPE_WORK);
        cv.put(ContactsContract.CommonDataKinds.Website.URL, (String)null);
        resolver.insert(ContactsContract.Data.CONTENT_URI, cv);

        cv = new ContentValues();
        cv.put(ContactsContract.Data.RAW_CONTACT_ID, id);
        cv.put(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
        cv.put(ContactsContract.CommonDataKinds.Organization.TYPE,
                ContactsContract.CommonDataKinds.Organization.TYPE_WORK);
        cv.put(ContactsContract.CommonDataKinds.Organization.COMPANY, (String)null);
        cv.put(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, (String)null);
        cv.put(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION, (String)null);
        cv.put(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION, (String)null);
        cv.put(ContactsContract.CommonDataKinds.Organization.TITLE, (String)null);
        resolver.insert(ContactsContract.Data.CONTENT_URI, cv);

        cv = new ContentValues();
        cv.put(ContactsContract.Data.RAW_CONTACT_ID, id);
        cv.put(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE);
        cv.put(ContactsContract.CommonDataKinds.Relation.TYPE,
                ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE);
        cv.put(ContactsContract.CommonDataKinds.Relation.NAME, (String)null);
        resolver.insert(ContactsContract.Data.CONTENT_URI, cv);

        cv = new ContentValues();
        cv.put(ContactsContract.Data.RAW_CONTACT_ID, id);
        cv.put(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        cv.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, (String)null);
        cv.put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, (String)null);
        cv.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, (String)null);
        cv.put(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, (String)null);
        cv.put(ContactsContract.CommonDataKinds.StructuredName.PREFIX, (String)null);
        cv.put(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, (String)null);
        resolver.insert(ContactsContract.Data.CONTENT_URI, cv);

        cv = new ContentValues();
        cv.put(ContactsContract.Data.RAW_CONTACT_ID, id);
        cv.put(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);
        cv.put(ContactsContract.CommonDataKinds.StructuredPostal.CITY, (String)null);
        cv.put(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, (String)null);
        cv.put(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD, (String)null);
        cv.put(ContactsContract.CommonDataKinds.StructuredPostal.POBOX, (String)null);
        cv.put(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, (String)null);
        cv.put(ContactsContract.CommonDataKinds.StructuredPostal.STREET, (String)null);
        resolver.insert(ContactsContract.Data.CONTENT_URI, cv);

        cv = new ContentValues();
        cv.put(ContactsContract.Data.RAW_CONTACT_ID, id);
        cv.put(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE);
        cv.put(ContactsContract.CommonDataKinds.Note.NOTE, (String)null);
        resolver.insert(ContactsContract.Data.CONTENT_URI, cv);

        Contact c = cm.load(Long.toString(id));
        List notes = c.getNotes();
        assertEquals(notes.size(), 1);

        for(Note note : (List<Note>)notes) {
            assertTrue(note.getPropertyValueAsString() == null);
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        byte[] local = os.toByteArray();
        // get a vcard with all fields empty
        byte[] expected = getSampleVCard(new Vector(), 
                cm.getSupportedFields(), true, true);

        assertVCardEquals(local, expected);

        Log.info("testNullFields start");
    }

}
