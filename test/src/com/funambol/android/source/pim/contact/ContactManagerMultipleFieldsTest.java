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
import java.util.List;
import java.util.Vector;

import com.funambol.common.pim.Utils;
import com.funambol.common.pim.model.contact.Address;
import com.funambol.common.pim.model.contact.Email;
import com.funambol.common.pim.model.contact.Phone;
import com.funambol.common.pim.model.contact.Photo;
import com.funambol.common.pim.model.contact.WebPage;


public class ContactManagerMultipleFieldsTest extends ContactManagerBaseTest {

    public void testMultiplePhone() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setMultipleFieldsSupported(true);

        Contact c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "TEL;CELL:1111111111\r\n" +
                    "TEL;CELL:2222222222\r\n" +
                    "TEL;CELL:3333333333\r\n" +
                    "TEL;CELL:4444444444\r\n" +
                    "TEL;CELL:5555555555\r\n" +
                    "TEL;CELL:6666666666\r\n" +
                    "TEL;CELL:7777777777\r\n" +
                    "TEL;CELL:8888888888\r\n" +
                    "TEL;CELL:9999999999\r\n" +
                    "TEL;CELL:0000000000\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("TEL;CELL:1111111111"));
        assertTrue(vcard.contains("TEL;CELL:2222222222"));
        assertTrue(vcard.contains("TEL;CELL:3333333333"));
        assertTrue(vcard.contains("TEL;CELL:4444444444"));
        assertTrue(vcard.contains("TEL;CELL:5555555555"));
        assertTrue(vcard.contains("TEL;CELL:6666666666"));
        assertTrue(vcard.contains("TEL;CELL:7777777777"));
        assertTrue(vcard.contains("TEL;CELL:8888888888"));
        assertTrue(vcard.contains("TEL;CELL:9999999999"));
        assertTrue(vcard.contains("TEL;CELL:0000000000"));

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("TEL;CELL:1111111111"));
        assertTrue(vcard.contains("TEL;CELL:2222222222"));
        assertTrue(vcard.contains("TEL;CELL:3333333333"));
        assertTrue(vcard.contains("TEL;CELL:4444444444"));
        assertTrue(vcard.contains("TEL;CELL:5555555555"));
        assertTrue(vcard.contains("TEL;CELL:6666666666"));
        assertTrue(vcard.contains("TEL;CELL:7777777777"));
        assertTrue(vcard.contains("TEL;CELL:8888888888"));
        assertTrue(vcard.contains("TEL;CELL:9999999999"));
        assertTrue(vcard.contains("TEL;CELL:0000000000"));

        List<Phone> pPhones = c.getPersonalDetail().getPhones();

        assertEquals(pPhones.size(), 10);

        for(Phone phone : pPhones) {
            assertEquals(phone.getPhoneType(), Phone.MOBILE_PHONE_NUMBER);
        }
    }

    public void testMultiplePhone_Update() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setMultipleFieldsSupported(true);

        Contact c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "TEL;VOICE;WORK:1111111111\r\n" +
                    "TEL;VOICE;WORK:2222222222\r\n" +
                    "TEL;VOICE;WORK:3333333333\r\n" +
                    "TEL;VOICE;HOME:7777777777\r\n" +
                    "TEL;VOICE;HOME:8888888888\r\n" +
                    "TEL;CELL:0000000000\r\n" +
                    "END:VCARD\r\n").getBytes());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("TEL;VOICE;WORK:1111111111"));
        assertTrue(vcard.contains("TEL;VOICE;WORK:2222222222"));
        assertTrue(vcard.contains("TEL;VOICE;WORK:3333333333"));
        assertTrue(vcard.contains("TEL;VOICE;HOME:7777777777"));
        assertTrue(vcard.contains("TEL;VOICE;HOME:8888888888"));
        assertTrue(vcard.contains("TEL;CELL:0000000000"));

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "TEL;VOICE;WORK:1111111111\r\n" +
                    "TEL;CELL:\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Update the contact
        cm.beginTransaction();
        cm.update(id, c);
        cm.commit();

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("TEL;VOICE;WORK:1111111111"));
        assertTrue(!vcard.contains("TEL;VOICE;WORK:2222222222"));
        assertTrue(!vcard.contains("TEL;VOICE;WORK:3333333333"));
        assertTrue(vcard.contains("TEL;VOICE;HOME:7777777777"));
        assertTrue(vcard.contains("TEL;VOICE;HOME:8888888888"));
        assertTrue(!vcard.contains("TEL;CELL:0000000000"));

        List<Phone> pPhones = c.getPersonalDetail().getPhones();
        List<Phone> bPhones = c.getBusinessDetail().getPhones();

        assertEquals(pPhones.size(), 2);
        assertEquals(bPhones.size(), 1);

        for(Phone phone : pPhones) {
            assertEquals(phone.getPhoneType(), Phone.HOME_PHONE_NUMBER);
        }

        for(Phone phone : bPhones) {
            assertEquals(phone.getPropertyValueAsString(), "1111111111");
            assertEquals(phone.getPhoneType(), Phone.BUSINESS_PHONE_NUMBER);
        }
    }

    public void testMultipleEmail() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setMultipleFieldsSupported(true);

        Contact c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "EMAIL;INTERNET;HOME:home@home1.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home2.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home3.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home4.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home5.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home6.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home7.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home8.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home9.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home0.com\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home1.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home2.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home3.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home4.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home5.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home6.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home7.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home8.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home9.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home0.com"));

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home1.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home2.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home3.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home4.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home5.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home6.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home7.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home8.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home9.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home0.com"));

        List<Email> pEmails = c.getPersonalDetail().getEmails();

        assertEquals(pEmails.size(), 10);

        for(Email email : pEmails) {
            assertEquals(email.getEmailType(), Email.HOME_EMAIL);
        }
    }

    public void testMultipleEmail_Update() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setMultipleFieldsSupported(true);

        Contact c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "EMAIL;INTERNET:other@other1.com\r\n" +
                    "EMAIL;INTERNET:other@other2.com\r\n" +
                    "EMAIL;INTERNET:other@other3.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home1.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home2.com\r\n" +
                    "EMAIL;INTERNET;WORK:work@work1.com\r\n" +
                    "END:VCARD\r\n").getBytes());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other1.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other2.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other3.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home1.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home2.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;WORK:work@work1.com"));

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "EMAIL;INTERNET:other@other2.com\r\n" +
                    "EMAIL;INTERNET;WORK:\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Update the contact
        cm.beginTransaction();
        cm.update(id, c);
        cm.commit();

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(!vcard.contains("EMAIL;INTERNET:other@other1.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other2.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET:other@other3.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home1.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home2.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET;WORK:work@work1.com"));

        List<Email> pEmails = c.getPersonalDetail().getEmails();
        List<Email> bEmails = c.getBusinessDetail().getEmails();

        assertEquals(pEmails.size(), 3);
        assertEquals(bEmails.size(), 0);

        for(Email email : pEmails) {
            assertTrue(Email.HOME_EMAIL.equals(email.getEmailType()) ||
                       Email.OTHER_EMAIL.equals(email.getEmailType()));
        }
    }

    public void testMultipleWebsite() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setMultipleFieldsSupported(true);

        Contact c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "URL:www.other1.com\r\n" +
                    "URL:www.other2.com\r\n" +
                    "URL:www.other3.com\r\n" +
                    "URL:www.other4.com\r\n" +
                    "URL:www.other5.com\r\n" +
                    "URL:www.other6.com\r\n" +
                    "URL:www.other7.com\r\n" +
                    "URL:www.other8.com\r\n" +
                    "URL:www.other9.com\r\n" +
                    "URL:www.other0.com\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("URL:www.other1.com"));
        assertTrue(vcard.contains("URL:www.other2.com"));
        assertTrue(vcard.contains("URL:www.other3.com"));
        assertTrue(vcard.contains("URL:www.other4.com"));
        assertTrue(vcard.contains("URL:www.other5.com"));
        assertTrue(vcard.contains("URL:www.other6.com"));
        assertTrue(vcard.contains("URL:www.other7.com"));
        assertTrue(vcard.contains("URL:www.other8.com"));
        assertTrue(vcard.contains("URL:www.other9.com"));
        assertTrue(vcard.contains("URL:www.other0.com"));

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("URL:www.other1.com"));
        assertTrue(vcard.contains("URL:www.other2.com"));
        assertTrue(vcard.contains("URL:www.other3.com"));
        assertTrue(vcard.contains("URL:www.other4.com"));
        assertTrue(vcard.contains("URL:www.other5.com"));
        assertTrue(vcard.contains("URL:www.other6.com"));
        assertTrue(vcard.contains("URL:www.other7.com"));
        assertTrue(vcard.contains("URL:www.other8.com"));
        assertTrue(vcard.contains("URL:www.other9.com"));
        assertTrue(vcard.contains("URL:www.other0.com"));

        List<WebPage> pWebPages = c.getPersonalDetail().getWebPages();

        assertEquals(pWebPages.size(), 10);

        for(WebPage webPage : pWebPages) {
            assertEquals(webPage.getWebPageType(), WebPage.OTHER_WEBPAGE);
        }
    }

    public void testMultipleWebsite_Update() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setMultipleFieldsSupported(true);

        Contact c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "URL;HOME:www.home1.com\r\n" +
                    "URL;HOME:www.home2.com\r\n" +
                    "URL;HOME:www.home3.com\r\n" +
                    "URL:www.other1.com\r\n" +
                    "URL;WORK:www.work1.com\r\n" +
                    "URL;WORK:www.work2.com\r\n" +
                    "END:VCARD\r\n").getBytes());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("URL;HOME:www.home1.com"));
        assertTrue(vcard.contains("URL;HOME:www.home2.com"));
        assertTrue(vcard.contains("URL;HOME:www.home3.com"));
        assertTrue(vcard.contains("URL:www.other1.com"));
        assertTrue(vcard.contains("URL;WORK:www.work1.com"));
        assertTrue(vcard.contains("URL;WORK:www.work2.com"));

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "URL;HOME:www.home1.com\r\n" +
                    "URL;HOME:www.home4.com\r\n" +
                    "URL:\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Update the contact
        cm.beginTransaction();
        cm.update(id, c);
        cm.commit();

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("URL;HOME:www.home1.com"));
        assertTrue(!vcard.contains("URL;HOME:www.home2.com"));
        assertTrue(!vcard.contains("URL;HOME:www.home3.com"));
        assertTrue(vcard.contains("URL;HOME:www.home4.com"));
        assertTrue(!vcard.contains("URL:www.other1.com"));
        assertTrue(vcard.contains("URL;WORK:www.work1.com"));
        assertTrue(vcard.contains("URL;WORK:www.work2.com"));

        List<WebPage> pWebPages = c.getPersonalDetail().getWebPages();
        List<WebPage> bWebPages = c.getBusinessDetail().getWebPages();

        assertEquals(pWebPages.size(), 2);
        assertEquals(bWebPages.size(), 2);

        for(WebPage webPage : pWebPages) {
            assertEquals(webPage.getWebPageType(), WebPage.HOME_WEBPAGE);
        }
        for(WebPage webPage : bWebPages) {
            assertEquals(webPage.getWebPageType(), WebPage.WORK_WEBPAGE);
        }
    }

    public void testMultipleAddress() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setMultipleFieldsSupported(true);

        Contact c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country2\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country3\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country4\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country5\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country6\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country7\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country8\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country9\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country0\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country2"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country3"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country4"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country5"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country6"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country7"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country8"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country9"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country0"));

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country2"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country3"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country4"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country5"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country6"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country7"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country8"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country9"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country0"));

        List<Address> pAddresses = c.getBusinessDetail().getAddresses();

        assertEquals(pAddresses.size(), 10);

        for(Address address : pAddresses) {
            assertEquals(address.getAddressType(), Address.WORK_ADDRESS);
        }
    }

    public void testMultipleAddress_Update() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setMultipleFieldsSupported(true);

        Contact c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1\r\n" +
                    "ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country1\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country1\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country2\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country3\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country4\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country5\r\n" +
                    "END:VCARD\r\n").getBytes());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1"));
        assertTrue(vcard.contains("ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country1"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country1"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country2"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country3"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country4"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country5"));

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "ADR;HOME:;;;;;;\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country1\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country2\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Update the contact
        cm.beginTransaction();
        cm.update(id, c);
        cm.commit();

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1"));
        assertTrue(!vcard.contains("ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country1"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country1"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country2"));
        assertTrue(!vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country3"));
        assertTrue(!vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country4"));
        assertTrue(!vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country5"));

        List<Address> pAddresses = c.getPersonalDetail().getAddresses();
        List<Address> bAddresses = c.getBusinessDetail().getAddresses();

        assertEquals(pAddresses.size(), 2);
        assertEquals(bAddresses.size(), 1);

        for(Address address : pAddresses) {
            assertEquals(address.getAddressType(), Address.OTHER_ADDRESS);
        }
        for(Address address : bAddresses) {
            assertEquals(address.getAddressType(), Address.WORK_ADDRESS);
        }
    }

    public void testMultiplePhoto() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setMultipleFieldsSupported(true);

        Contact c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +                          
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto()));

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto()));

        List<Photo> photos = c.getPersonalDetail().getPhotoObjects();
        assertEquals(photos.size(), 10);
    }

    public void testMultiplephoto_Update() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setMultipleFieldsSupported(true);

        Contact c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "END:VCARD\r\n").getBytes());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto()));

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Update the contact
        cm.beginTransaction();
        cm.update(id, c);
        cm.commit();

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto()));

        List<Photo> pPhotos = c.getPersonalDetail().getPhotoObjects();

        assertEquals(pPhotos.size(), 2);
    }

    public void testMultipleMixedFields() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setMultipleFieldsSupported(true);

        Contact c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "TEL;VOICE;WORK:1111111111\r\n" +
                    "TEL;VOICE;WORK:2222222222\r\n" +
                    "TEL;VOICE;WORK:3333333333\r\n" +
                    "TEL;VOICE;WORK:4444444444\r\n" +
                    "TEL;VOICE;WORK:5555555555\r\n" +
                    "TEL;VOICE;WORK:6666666666\r\n" +
                    "TEL;VOICE;HOME:7777777777\r\n" +
                    "TEL;VOICE;HOME:8888888888\r\n" +
                    "TEL;CELL:0000000000\r\n" +
                    "EMAIL;INTERNET:other@other1.com\r\n" +
                    "EMAIL;INTERNET:other@other2.com\r\n" +
                    "EMAIL;INTERNET:other@other3.com\r\n" +
                    "EMAIL;INTERNET:other@other4.com\r\n" +
                    "EMAIL;INTERNET:other@other5.com\r\n" +
                    "EMAIL;INTERNET:other@other6.com\r\n" +
                    "EMAIL;INTERNET:other@other7.com\r\n" +
                    "EMAIL;INTERNET:other@other8.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home1.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home2.com\r\n" +
                    "EMAIL;INTERNET;WORK:work@work1.com\r\n" +
                    "URL;HOME:www.home1.com\r\n" +
                    "URL;HOME:www.home2.com\r\n" +
                    "URL;HOME:www.home3.com\r\n" +
                    "URL:www.other1.com\r\n" +
                    "URL;WORK:www.work1.com\r\n" +
                    "URL;WORK:www.work2.com\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1\r\n" +
                    "ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country1\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country1\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country2\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country3\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country4\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "END:VCARD\r\n").getBytes());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("TEL;VOICE;WORK:1111111111"));
        assertTrue(vcard.contains("TEL;VOICE;WORK:2222222222"));
        assertTrue(vcard.contains("TEL;VOICE;WORK:3333333333"));
        assertTrue(vcard.contains("TEL;VOICE;WORK:4444444444"));
        assertTrue(vcard.contains("TEL;VOICE;WORK:5555555555"));
        assertTrue(vcard.contains("TEL;VOICE;WORK:6666666666"));
        assertTrue(vcard.contains("TEL;VOICE;HOME:7777777777"));
        assertTrue(vcard.contains("TEL;VOICE;HOME:8888888888"));
        assertTrue(vcard.contains("TEL;CELL:0000000000"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other1.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other2.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other3.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other4.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other5.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other6.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other7.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other8.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home1.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home2.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;WORK:work@work1.com"));
        assertTrue(vcard.contains("URL;HOME:www.home1.com"));
        assertTrue(vcard.contains("URL;HOME:www.home2.com"));
        assertTrue(vcard.contains("URL;HOME:www.home3.com"));
        assertTrue(vcard.contains("URL:www.other1.com"));
        assertTrue(vcard.contains("URL;WORK:www.work1.com"));
        assertTrue(vcard.contains("URL;WORK:www.work2.com"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1"));
        assertTrue(vcard.contains("ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country1"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country1"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country2"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country3"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country4"));
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto()));

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "TEL;VOICE;WORK:\r\n" +
                    "TEL;VOICE;HOME:9999999999\r\n" +
                    "EMAIL;INTERNET:other@other1.com\r\n" +
                    "EMAIL;INTERNET:other@other2.com\r\n" +
                    "EMAIL;INTERNET:other@other3.com\r\n" +
                    "EMAIL;INTERNET:other@other4.com\r\n" +
                    "EMAIL;INTERNET:other@other5.com\r\n" +
                    "EMAIL;INTERNET:other@other6.com\r\n" +
                    "EMAIL;INTERNET;HOME:\r\n" +
                    "EMAIL;INTERNET;WORK:work@work1.com\r\n" +
                    "URL:\r\n" +
                    "URL;WORK:www.work1.com\r\n" +
                    "URL;WORK:www.work2.com\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country4\r\n" +
                    "PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto() + "\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Update the contact
        cm.beginTransaction();
        cm.update(id, c);
        cm.commit();

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(!vcard.contains("TEL;VOICE;WORK:1111111111"));
        assertTrue(!vcard.contains("TEL;VOICE;WORK:2222222222"));
        assertTrue(!vcard.contains("TEL;VOICE;WORK:3333333333"));
        assertTrue(!vcard.contains("TEL;VOICE;WORK:4444444444"));
        assertTrue(!vcard.contains("TEL;VOICE;WORK:5555555555"));
        assertTrue(!vcard.contains("TEL;VOICE;WORK:6666666666"));
        assertTrue(!vcard.contains("TEL;VOICE;HOME:7777777777"));
        assertTrue(!vcard.contains("TEL;VOICE;HOME:8888888888"));
        assertTrue( vcard.contains("TEL;VOICE;HOME:9999999999"));
        assertTrue( vcard.contains("TEL;CELL:0000000000"));
        assertTrue( vcard.contains("EMAIL;INTERNET:other@other1.com"));
        assertTrue( vcard.contains("EMAIL;INTERNET:other@other2.com"));
        assertTrue( vcard.contains("EMAIL;INTERNET:other@other3.com"));
        assertTrue( vcard.contains("EMAIL;INTERNET:other@other4.com"));
        assertTrue( vcard.contains("EMAIL;INTERNET:other@other5.com"));
        assertTrue( vcard.contains("EMAIL;INTERNET:other@other6.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET:other@other7.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET:other@other8.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET;HOME:home@home1.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET;HOME:home@home2.com"));
        assertTrue( vcard.contains("EMAIL;INTERNET;WORK:work@work1.com"));
        assertTrue( vcard.contains("URL;HOME:www.home1.com"));
        assertTrue( vcard.contains("URL;HOME:www.home2.com"));
        assertTrue( vcard.contains("URL;HOME:www.home3.com"));
        assertTrue(!vcard.contains("URL:www.other1.com"));
        assertTrue( vcard.contains("URL;WORK:www.work1.com"));
        assertTrue( vcard.contains("URL;WORK:www.work2.com"));
        assertTrue( vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1"));
        assertTrue( vcard.contains("ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country1"));
        assertTrue(!vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country1"));
        assertTrue(!vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country2"));
        assertTrue(!vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country3"));
        assertTrue( vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country4"));
        assertTrue( vcard.contains("PHOTO;ENCODING=BASE64;TYPE=image/jpeg:\r\n " + getSamplePhoto()));

        List<Phone> pPhones = c.getPersonalDetail().getPhones();
        List<Phone> bPhones = c.getBusinessDetail().getPhones();
        List<Email> pEmails = c.getPersonalDetail().getEmails();
        List<Email> bEmails = c.getBusinessDetail().getEmails();
        List<WebPage> pWebPages = c.getPersonalDetail().getWebPages();
        List<WebPage> bWebPages = c.getBusinessDetail().getWebPages();
        List<Address> pAddresses = c.getPersonalDetail().getAddresses();
        List<Address> bAddresses = c.getBusinessDetail().getAddresses();
        List<Photo> pPhotos = c.getPersonalDetail().getPhotoObjects();

        assertEquals(pPhones.size(), 2);
        assertEquals(bPhones.size(), 0);
        assertEquals(pEmails.size(), 6);
        assertEquals(bEmails.size(), 1);
        assertEquals(pWebPages.size(), 3);
        assertEquals(bWebPages.size(), 2);
        assertEquals(pAddresses.size(), 2);
        assertEquals(bAddresses.size(), 1);
        assertEquals(pPhotos.size(), 1);

        for(Phone phone : pPhones) {
            assertTrue(phone.getPhoneType().equals(Phone.HOME_PHONE_NUMBER) ||
                       phone.getPhoneType().equals(Phone.MOBILE_PHONE_NUMBER));
        }
        for(Email email : pEmails) {
            assertTrue(Email.OTHER_EMAIL.equals(email.getEmailType()));
        }
        for(Email email : bEmails) {
            assertTrue(Email.WORK_EMAIL.equals(email.getEmailType()));
        }
        for(WebPage webPage : pWebPages) {
            assertEquals(webPage.getWebPageType(), WebPage.HOME_WEBPAGE);
        }
        for(WebPage webPage : bWebPages) {
            assertEquals(webPage.getWebPageType(), WebPage.WORK_WEBPAGE);
        }
        for(Address address : pAddresses) {
            assertTrue(address.getAddressType().equals(Address.HOME_ADDRESS) ||
                       address.getAddressType().equals(Address.OTHER_ADDRESS));
        }
        for(Address address : bAddresses) {
            assertEquals(address.getAddressType(), Address.WORK_ADDRESS);
        }
    }

    protected String getSamplePhoto() {
        return new Utils("UTF-8").fold(
               "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEABALDA4MChAODQ4SERATGCgaGBYWGDEj" +
               "JR0oOjM9PDkzODdASFxOQERXRTc4UG1RV19iZ2hnPk1xeXBkeFxlZ2MBERISGBUY" +
               "LxoaL2NCOEJjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2Nj" +
               "Y2NjY2NjY2NjY//AABEIADwAUAMBIgACEQEDEQH/xAGiAAABBQEBAQEBAQAAAAAA" +
               "AAAAAQIDBAUGBwgJCgsQAAIBAwMCBAMFBQQEAAABfQECAwAEEQUSITFBBhNRYQci" +
               "cRQygZGhCCNCscEVUtHwJDNicoIJChYXGBkaJSYnKCkqNDU2Nzg5OkNERUZHSElK" +
               "U1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6g4SFhoeIiYqSk5SVlpeYmZqio6Slpqeo" +
               "qaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2drh4uPk5ebn6Onq8fLz9PX29/j5" +
               "+gEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoLEQACAQIEBAMEBwUEBAABAncA" +
               "AQIDEQQFITEGEkFRB2FxEyIygQgUQpGhscEJIzNS8BVictEKFiQ04SXxFxgZGiYn" +
               "KCkqNTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqCg4SFhoeI" +
               "iYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2dri" +
               "4+Tl5ufo6ery8/T19vf4+fr/2gAMAwEAAhEDEQA/AORsdKkvGUn5IyfvUmo6ZJZX" +
               "JiZgRjKnGMiusCxwPGDhVUVB4nhiutN+0QsGaEgHHoaUrLRDSujjMUUuKXFAhtFO" +
               "xRikA2jFPxRigBmKUCnYpcYouFjqJr8ucqACOoJ6VJcSodIu0ZsO8fC49PesCPzX" +
               "lXg4NXJJJDG0bHGeB71D1ZV9DGC1K0WIVbHWrCwq3Cgjj9akaNjEEIOBzTlcSt1K" +
               "ASl2Vq2ulT3SuYImcIMtgdBVVodrEHrRcCps4o24rf0HS7fUbwQ3EwiTaTmpNQ0C" +
               "O1tjMJg43lVx0IHei4HN4pCOKtvblMqR+NQsv7vrTA0Yf9fHkcc84rUNrFPDzuyv" +
               "IwM81jvO0ax/KCAD0/CpYtQc8KDWcqbezHGVnsTW1o7RuCm2RW6swAx/jWjcbZbG" +
               "C2ItozHkmUMMn2OKxkdlOBGVJJyTzQZVDcxdKtpkmla3k+mmRLa4Qh12nbzxVE7Z" +
               "HJPU0xJQDlAEPf3pPMl3cEDJzwKLAX7CG2hV2uDIxx8m35QD71evL5J40R/LVEGF" +
               "BPasf5xgs5OfU0wOivlhnPoKLIEWLh7ecqiBRwAWC4x71TMEUcpEhJA6EVYMmRhV" +
               "GPpUbkHrkUXHYR4lMqDBICnv9KkjiVOkWD61ZcA3CcD7h/mKsbRnpWjFYoOJCp28" +
               "VGkDHlhWkVGelAQY6VLGkUHhG3hcEelQSlolB2kZ6E1rlRs6YpjRq0fzAH2qWOxl" +
               "lncCjDYAPbpV1oI0QsopREpXNS2UkRQq2MkEr7VMFR+gJ/CnwqMMD26VOvBoTHY/" +
               "/9k=");
    }

}
