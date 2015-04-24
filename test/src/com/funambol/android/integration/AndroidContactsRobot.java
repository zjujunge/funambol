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
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import android.accounts.Account;
import android.app.Instrumentation;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.provider.ContactsContract;
import com.funambol.android.AndroidAccountManager;

import com.funambol.common.pim.model.common.Property;
import com.funambol.common.pim.model.contact.Photo;
import com.funambol.android.AndroidCustomization;
import com.funambol.android.App;
import com.funambol.android.AppInitializer;
import com.funambol.android.source.pim.contact.Contact;
import com.funambol.android.source.pim.contact.ContactManager;
import com.funambol.android.source.pim.contact.FunambolContactManager;
import com.funambol.sync.SyncItem;
import com.funambol.platform.FileAdapter;

import com.funambol.client.source.AppSyncSourceManager;
import com.funambol.client.test.ClientTestException;
import com.funambol.client.test.basic.BasicRobot;
import com.funambol.client.test.contact.ContactsCommandRunner;
import com.funambol.client.test.contact.ContactsRobot;
import com.funambol.client.configuration.Configuration;
import com.funambol.util.StringUtil;


public class AndroidContactsRobot extends ContactsRobot {
   
    private static final String TAG_LOG = "AndroidContactsRobot";

    private Instrumentation instrumentation = null;

    private ContactManager cm = null;
    private Contact      currentContact = null;
    private StringBuffer currentContactVCard = null;

    ArrayList<ContentProviderOperation> rawContactOperations;
    private long lastRawContactId = -1;

    // Associates script field names to vCard fields
    private Hashtable<String,String> vCardFields = new Hashtable<String,String>();

    public AndroidContactsRobot(Instrumentation instrumentation, BasicRobot basicRobot) {

        // The app source manager is not available yet, it will be set later
        super(null);

        this.basicRobot = basicRobot;

        this.instrumentation = instrumentation;

        // Init vCard fields
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_DISPLAY_NAME,  "FN");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_NICK_NAME,     "NICKNAME");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_TEL_HOME,      "TEL;VOICE;HOME");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_TEL_WORK,      "TEL;VOICE;WORK");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_TEL_OTHER,     "TEL;VOICE");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_TEL_OTHER2,    "TEL;PREF;VOICE");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_TEL_CELL,      "TEL;CELL");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_TEL_PAGER,     "TEL;PAGER");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_TEL_FAX_HOME,  "TEL;FAX;HOME");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_TEL_FAX_WORK,  "TEL;FAX;WORK");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_TEL_COMPANY,   "TEL;WORK;PREF");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_TEL_OTHER_FAX, "TEL;FAX");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_EMAIL_HOME,    "EMAIL;INTERNET;HOME");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_EMAIL_WORK,    "EMAIL;INTERNET;WORK");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_EMAIL_OTHER,   "EMAIL;INTERNET");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_EMAIL_IM,      "EMAIL;INTERNET;HOME;X-FUNAMBOL-INSTANTMESSENGER");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_ADR_OTHER,     "ADR");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_ADR_HOME,      "ADR;HOME");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_ADR_WORK,      "ADR;WORK");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_WEB,           "URL");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_WEB_HOME,      "URL;HOME");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_WEB_WORK,      "URL;WORK");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_BDAY,          "BDAY");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_ANNIVERSARY,   "X-ANNIVERSARY");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_CHILDREN,      "X-FUNAMBOL-CHILDREN");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_SPOUSE,        "X-SPOUSE");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_TITLE,         "TITLE");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_ORGANIZATION,  "ORG");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_NOTE,          "NOTE");
        vCardFields.put(ContactsCommandRunner.CONTACT_FIELD_PHOTO,         "PHOTO");
    }

    public ContactManager getContactManager() {
        if(cm == null) {
            // Init contact manager
            cm = new FunambolContactManager(instrumentation.getContext(), false);
        }
        return cm;
    }

    @Override
    public void createEmptyContact() throws Throwable {
        currentContact = new Contact();
        initEmptyContactVCard();
    }

    private void initEmptyContactVCard() {
        currentContactVCard = new StringBuffer();
        currentContactVCard.append("BEGIN:VCARD").append("\r\n");
        currentContactVCard.append("VERSION:2.1").append("\r\n");
    }

    @Override
    public void setContactField(String field, String value) throws Throwable {
        if(currentContact == null) {
            throw new ClientTestException("You have to inizialize the contact before edotong it");
        }
        if(ContactsCommandRunner.CONTACT_FIELD_FIRST_NAME.equals(field)) {
            currentContact.getName().setFirstName(new Property(value));
        } else if(ContactsCommandRunner.CONTACT_FIELD_LAST_NAME.equals(field)) {
            currentContact.getName().setLastName(new Property(value));
        } else if(ContactsCommandRunner.CONTACT_FIELD_MIDDLE_NAME.equals(field)) {
            currentContact.getName().setMiddleName(new Property(value));
        } else if(ContactsCommandRunner.CONTACT_FIELD_PREFIX_NAME.equals(field)) {
            currentContact.getName().setSalutation(new Property(value));
        } else if(ContactsCommandRunner.CONTACT_FIELD_SUFFIX_NAME.equals(field)) {
            currentContact.getName().setSuffix(new Property(value));
        } else if(ContactsCommandRunner.CONTACT_FIELD_NICK_NAME.equals(field)) {
            currentContact.getName().setNickname(new Property(value));
        } else if(ContactsCommandRunner.CONTACT_FIELD_DISPLAY_NAME.equals(field)) {
            if(AndroidCustomization.getInstance().isDisplayNameSupported()) {
                currentContact.getName().setDisplayName(new Property(value));
            }
        } else if(ContactsCommandRunner.CONTACT_FIELD_PHOTO.equals(field)
                && value.length()>0) {
            
            Photo photo = new Photo();

            FileAdapter fa = new FileAdapter(value, true);
            InputStream is = fa.openInputStream();
            
            int bytesCount = 0;
            while(is.read() != -1) {
                bytesCount++;
            }

            byte[] bytes = new byte[bytesCount];
            is = fa.openInputStream();
            is.read(bytes);
            
            photo.setImage(bytes);
            currentContact.getPersonalDetail().addPhotoObject(photo);
        } else {
            String vCardField = vCardFields.get(field);
            if(vCardField == null) {
                throw new IllegalArgumentException("Unknown field: " + field);
            } else {
                currentContactVCard.append(vCardField).append(":")
                        .append(value).append("\r\n");
            }
        }
    }

    @Override
    public void saveContact() throws Throwable {

        Contact contact = new Contact();

        // Finish contact formatting
        currentContactVCard.append("END:VCARD").append("\r\n");
        contact.setVCard(currentContactVCard.toString().getBytes());

        contact.setName(currentContact.getName());

        contact.getPersonalDetail().setPhotos(
                currentContact.getPersonalDetail().getPhotos());

        // Check if firstname and lastname are set
        if(contact.getName().getFirstName().getPropertyValue() == null ||
           contact.getName().getLastName().getPropertyValue() == null) {
            throw new ClientTestException("You must set firstname and lastname before saving the contact");
        }

        getContactManager().beginTransaction();
        if(currentContactId != -1) {
            getContactManager().update(Long.toString(currentContactId), contact);
        } else {
            getContactManager().add(contact);
        }
        getContactManager().commit();
        
        // Reset current contact
        currentContact = null;
        currentContactVCard = null;
        currentContactId = -1;
    }

    @Override
    public void deleteContact(String firstname, String lastname) throws Throwable {
        instrumentation.getTargetContext().getContentResolver().delete(
                ContactsContract.RawContacts.CONTENT_URI,
                ContactsContract.RawContacts._ID+"="+
                findContactKey(firstname, lastname), null);
    }

    @Override
    public void deleteAllContacts() throws Throwable {
        instrumentation.getTargetContext().getContentResolver().delete(
                ContactsContract.RawContacts.CONTENT_URI, null, null);
    }

    @Override
    protected void checkContactAsVCard(String vcard) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadContact(String firstName, String lastName) throws Throwable {
        currentContactId = findContactKey(firstName, lastName);
        currentContact = getContactManager().load(Long.toString(currentContactId));
        initEmptyContactVCard();
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
    protected String getCurrentContactVCard() throws Throwable {
        if (currentContactVCard != null) {
            return currentContactVCard.toString();
        } else if (currentContact != null) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            currentContact.toVCard(os, getContactManager().getSupportedFields());
            return os.toString();
        } else {
            throw new ClientTestException("No current contact");
        }
    }

    private long findContactKey(String firstName, String lastName) throws Throwable {
        Enumeration allkeys = getContactManager().getAllKeys();
        while(allkeys.hasMoreElements()) {
            long key = Long.parseLong((String)allkeys.nextElement());
            Contact contact = getContactManager().load(Long.toString(key));
            if(contact.getName().getFirstName().getPropertyValueAsString().equals(firstName) &&
               contact.getName().getLastName().getPropertyValueAsString().equals(lastName)) {
               return key;
            }
        }
        throw new ClientTestException("Can't find contact: " + firstName + " " + lastName);
    }

    @Override
    public void createEmptyRawContact() throws Throwable {
        Account account = AndroidAccountManager.getNativeAccount(
                instrumentation.getContext());
        rawContactOperations = new ArrayList<ContentProviderOperation>();
        rawContactOperations.add(ContentProviderOperation.newInsert(
                ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
                .build());
    }

    @Override
    public void setRawContactData(String mimeType, Vector dataValues) throws Throwable {

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                ContactsContract.Data.CONTENT_URI);
        builder = builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);

        builder = builder.withValue(ContactsContract.Data.MIMETYPE, mimeType);
        for(int i=0; i<dataValues.size(); i++) {
            builder = builder.withValue("data" + (i+1), (String)dataValues.elementAt(i));
        }
        rawContactOperations.add(builder.build());
    }

    @Override
    public void saveRawContact() throws Throwable {
        ContentResolver resolver = instrumentation.getContext().getContentResolver();
        ContentProviderResult[] res = resolver.applyBatch(
                ContactsContract.AUTHORITY, rawContactOperations);
        lastRawContactId = ContentUris.parseId(res[0].uri);
    }

    @Override
    public void checkRawContactAsVCard(String vcard) throws Throwable {

        // \r\n must be explicit, so we remove them
        vcard = StringUtil.replaceAll(vcard, "\r", "");
        vcard = StringUtil.replaceAll(vcard, "\n", "");
        // Now transform the encoded \r \n into the actual chars
        vcard = StringUtil.replaceAll(vcard, "\\r\\n", "\r\n");

        Contact contact = getContactManager().load(Long.toString(lastRawContactId));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        contact.toVCard(os, getContactManager().getSupportedFields());

        assertTrue(vcard, os.toString(), "vCard mismatch");
    }

    @Override
    public void checkRawContactData(String mimeType, Vector dataValues) throws Throwable {

        ContentResolver cr = instrumentation.getTargetContext().getContentResolver();

        StringBuffer whereClause = new StringBuffer();

        whereClause.append(ContactsContract.Data.MIMETYPE).append("='")
                .append(mimeType).append("'");

        for(int i=0; i<dataValues.size(); i++) {
            String value = (String)dataValues.elementAt(i);

            // Unescape commas
            value = StringUtil.replaceAll(value, "?-?", ",");
            // Unescape CRLF
            value = StringUtil.replaceAll(value, "\\r", "\r");
            value = StringUtil.replaceAll(value, "\\n", "\n");

            if(!StringUtil.isNullOrEmpty(value)) {
                whereClause.append(" AND ").append("data").append((i+1)).append("='")
                    .append(value).append("'");
            }
        }

        Cursor cursor = cr.query(ContactsContract.Data.CONTENT_URI, null,
                whereClause.toString(), null, null);
        assertTrue(cursor.getCount(), 1, "Data fields count mismatches");

        cursor.close();
    }

    @Override
    protected Configuration getConfiguration() {
        return App.i().getAppInitializer().getConfiguration();
    }

}
