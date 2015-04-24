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

import com.funambol.common.pim.model.common.Property;
import com.funambol.common.pim.model.contact.Address;
import com.funambol.common.pim.model.contact.Email;
import com.funambol.common.pim.model.contact.Phone;
import com.funambol.common.pim.model.contact.Photo;
import com.funambol.common.pim.model.contact.WebPage;


public class ContactManagerPreferredFieldsTest extends ContactManagerBaseTest {

    public void testPreferredPhone() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setPreferredFieldsSupported(true);

        Contact c = new Contact();

        Phone mobile = new Phone("0000000000");
        mobile.setPhoneType(Phone.MOBILE_PHONE_NUMBER);

        Phone home = new Phone("1111111111");
        home.setPhoneType(Phone.HOME_PHONE_NUMBER);

        Phone work = new Phone("2222222222");
        work.setPhoneType(Phone.BUSINESS_PHONE_NUMBER);
        work.setPreferred(true);

        c.getPersonalDetail().addPhone(mobile);
        c.getPersonalDetail().addPhone(home);
        c.getBusinessDetail().addPhone(work);

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("TEL;CELL:0000000000"));
        assertTrue(vcard.contains("TEL;VOICE;HOME:1111111111"));
        assertTrue(vcard.contains("TEL;VOICE;WORK;PREF:2222222222"));

        c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "TEL;CELL:0000000000\r\n" +
                    "TEL;VOICE;HOME:1111111111\r\n" +
                    "TEL;VOICE;WORK;PREF:2222222222\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("TEL;CELL:0000000000"));
        assertTrue(vcard.contains("TEL;VOICE;HOME:1111111111"));
        assertTrue(vcard.contains("TEL;VOICE;WORK;PREF:2222222222"));

        List<Phone> pPhones = c.getPersonalDetail().getPhones();
        List<Phone> bPhones = c.getBusinessDetail().getPhones();

        assertEquals(pPhones.size(), 2);
        assertEquals(bPhones.size(), 1);

        for(Phone phone : pPhones) {
            assertTrue(!phone.isPreferred());
        }
        for(Phone phone : bPhones) {
            assertTrue(phone.isPreferred());
        }
    }

    public void testPreferredEmail() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setPreferredFieldsSupported(true);

        Contact c = new Contact();

        Email home = new Email("home@home.com");
        home.setEmailType(Email.HOME_EMAIL);
 
        Email work = new Email("work@work.com");
        work.setEmailType(Email.WORK_EMAIL);
        work.setPreferred(true);

        Email other = new Email("other@other.com");
        other.setEmailType(Email.OTHER_EMAIL);
        
        c.getPersonalDetail().addEmail(home);
        c.getBusinessDetail().addEmail(work);
        c.getPersonalDetail().addEmail(other);

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;WORK;PREF:work@work.com"));

        c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "EMAIL;INTERNET:other@other.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home.com\r\n" +
                    "EMAIL;INTERNET;WORK;PREF:work@work.com\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;WORK;PREF:work@work.com"));

        List<Email> pEmails = c.getPersonalDetail().getEmails();
        List<Email> bEmails = c.getBusinessDetail().getEmails();

        assertEquals(pEmails.size(), 2);
        assertEquals(bEmails.size(), 1);

        for(Email email : pEmails) {
           assertTrue(!email.isPreferred());
        }
        for(Email email : bEmails) {
            assertTrue(email.isPreferred());
        }
    }

    public void testPreferredAddress() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setPreferredFieldsSupported(true);

        Contact c = new Contact();

        Address home = new Address();
        home.setCity(new Property("h_city"));
        home.setCountry(new Property("h_country"));
        home.setExtendedAddress(new Property("h_ex_address"));
        home.setPostOfficeAddress(new Property("h_po"));
        home.setPostalCode(new Property("h_code"));
        home.setState(new Property("h_state"));
        home.setStreet(new Property("h_street"));
        home.setPropertyType(Address.HOME_ADDRESS);

        Address work = new Address();
        work.setCity(new Property("w_city"));
        work.setCountry(new Property("w_country"));
        work.setExtendedAddress(new Property("w_ex_address"));
        work.setPostOfficeAddress(new Property("w_po"));
        work.setPostalCode(new Property("w_code"));
        work.setState(new Property("w_state"));
        work.setStreet(new Property("w_street"));
        work.setPropertyType(Address.WORK_ADDRESS);

        Address other = new Address();
        other.setCity(new Property("o_city"));
        other.setCountry(new Property("o_country"));
        other.setExtendedAddress(new Property("o_ex_address"));
        other.setPostOfficeAddress(new Property("o_po"));
        other.setPostalCode(new Property("o_code"));
        other.setState(new Property("o_state"));
        other.setStreet(new Property("o_street"));
        other.setPropertyType(Address.OTHER_ADDRESS);
        other.setPreferred(true);

        c.getPersonalDetail().addAddress(home);
        c.getPersonalDetail().addAddress(other);
        c.getBusinessDetail().addAddress(work);

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("ADR;PREF:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country"));
        assertTrue(vcard.contains("ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country"));

        c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "ADR;PREF:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country\r\n" +
                    "ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("ADR;PREF:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country"));
        assertTrue(vcard.contains("ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country"));

        List<Address> pAddresses = c.getPersonalDetail().getAddresses();
        List<Address> bAddresses = c.getBusinessDetail().getAddresses();

        assertEquals(pAddresses.size(), 2);
        assertEquals(bAddresses.size(), 1);

        for(Address address : pAddresses) {
            if(address.getAddressType().equals(Address.OTHER_ADDRESS)) {
                assertTrue(address.isPreferred());
            } else {
                assertTrue(!address.isPreferred());
            }
        }
        for(Address address : bAddresses) {
            assertTrue(!address.isPreferred());
        }
    }

    public void testPreferredWebsite() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setPreferredFieldsSupported(true);

        Contact c = new Contact();

        WebPage home = new WebPage("www.home.com");
        home.setWebPageType("HomeWebPage");

        WebPage work = new WebPage("www.work.com");
        work.setWebPageType("BusinessWebPage");
        work.setPreferred(true);

        WebPage other = new WebPage("www.other.com");
        other.setWebPageType("WebPage");

        c.getPersonalDetail().addWebPage(home);
        c.getBusinessDetail().addWebPage(work);
        c.getPersonalDetail().addWebPage(other);

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("URL:www.other.com"));
        assertTrue(vcard.contains("URL;HOME:www.home.com"));
        assertTrue(vcard.contains("URL;WORK;PREF:www.work.com"));

        c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "URL:www.other.com\r\n" +
                    "URL;HOME:www.home.com\r\n" +
                    "URL;WORK;PREF:www.work.com\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("URL:www.other.com"));
        assertTrue(vcard.contains("URL;HOME:www.home.com"));
        assertTrue(vcard.contains("URL;WORK;PREF:www.work.com"));

        List<WebPage> pWebPages = c.getPersonalDetail().getWebPages();
        List<WebPage> bWebPages = c.getBusinessDetail().getWebPages();

        assertEquals(pWebPages.size(), 2);
        assertEquals(bWebPages.size(), 1);

        for(WebPage webPage : pWebPages) {
            assertTrue(!webPage.isPreferred());
        }
        for(WebPage webPage : bWebPages) {
            assertTrue(webPage.isPreferred());
        }
    }

    public void testPreferredPhoto() throws Exception {

        // Here we want to check the support of the PREF parameter
        cm.setPreferredFieldsSupported(true);

        Contact c = new Contact();

        Photo photo = new Photo();
        photo.setImage("photo".getBytes());
        photo.setPreferred(true);
        c.getPersonalDetail().addPhotoObject(photo);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());

        assertTrue(vcard.contains("PHOTO;PREF;ENCODING=BASE64:\r\n cGhvdG8="));

        c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "PHOTO;PREF;ENCODING=BASE64:\r\n cGhvdG8=\r\n" +
                    "END:VCARD\r\n").getBytes());

        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);

        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());

        assertTrue(vcard.contains("PHOTO;PREF;ENCODING=BASE64:\r\n cGhvdG8="));

        List<Photo> photos = c.getPersonalDetail().getPhotoObjects();
        assertEquals(photos.size(), 1);
        assertTrue(photos.get(0).isPreferred());
    }

    public void testPreferredMultipleMixedFields() throws Exception {

        cm.setMultipleFieldsSupported(true);
        cm.setPreferredFieldsSupported(true);

        Contact c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "TEL;VOICE;WORK:1111111111\r\n" +
                    "TEL;VOICE;WORK:2222222222\r\n" +
                    "TEL;VOICE;WORK;PREF:3333333333\r\n" +
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
                    "EMAIL;INTERNET;PREF:other@other6.com\r\n" +
                    "EMAIL;INTERNET:other@other7.com\r\n" +
                    "EMAIL;INTERNET:other@other8.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home1.com\r\n" +
                    "EMAIL;INTERNET;HOME:home@home2.com\r\n" +
                    "EMAIL;INTERNET;WORK:work@work1.com\r\n" +
                    "URL;HOME:www.home1.com\r\n" +
                    "URL;HOME;PREF:www.home2.com\r\n" +
                    "URL;HOME:www.home3.com\r\n" +
                    "URL:www.other1.com\r\n" +
                    "URL;WORK:www.work1.com\r\n" +
                    "URL;WORK:www.work2.com\r\n" +
                    "ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1\r\n" +
                    "ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country1\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country1\r\n" +
                    "ADR;PREF:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country2\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country3\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country4\r\n" +
                    "PHOTO;ENCODING=BASE64:\r\n cGhvdG8x\r\n" +
                    "PHOTO;ENCODING=BASE64:\r\n cGhvdG8y\r\n" +
                    "PHOTO;ENCODING=BASE64:\r\n cGhvdG8z\r\n" +
                    "PHOTO;ENCODING=BASE64:\r\n cGhvdG80\r\n" +
                    "PHOTO;ENCODING=BASE64:\r\n cGhvdG81\r\n" +
                    "PHOTO;ENCODING=BASE64;PREF:\r\n cHJlZl9waG90bw==\r\n" +
                    "END:VCARD\r\n").getBytes());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        String vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("TEL;VOICE;WORK:1111111111"));
        assertTrue(vcard.contains("TEL;VOICE;WORK:2222222222"));
        assertTrue(vcard.contains("TEL;VOICE;WORK;PREF:3333333333"));
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
        assertTrue(vcard.contains("EMAIL;INTERNET;PREF:other@other6.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other7.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other8.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home1.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home2.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;WORK:work@work1.com"));
        assertTrue(vcard.contains("URL;HOME:www.home1.com"));
        assertTrue(vcard.contains("URL;HOME;PREF:www.home2.com"));
        assertTrue(vcard.contains("URL;HOME:www.home3.com"));
        assertTrue(vcard.contains("URL:www.other1.com"));
        assertTrue(vcard.contains("URL;WORK:www.work1.com"));
        assertTrue(vcard.contains("URL;WORK:www.work2.com"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1"));
        assertTrue(vcard.contains("ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country1"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country1"));
        assertTrue(vcard.contains("ADR;PREF:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country2"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country3"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country4"));
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG8x"));
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG8y"));
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG8z"));
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG80"));
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG81"));
        assertTrue(vcard.contains("PHOTO;PREF;ENCODING=BASE64:\r\n cHJlZl9waG90bw=="));
        
        // Add the new contact
        cm.beginTransaction();
        cm.add(c);
        Vector keys = cm.commit();
        String id = (String)keys.elementAt(0);
        
        // Load the same contact
        c = cm.load(id);

        os = new ByteArrayOutputStream();
        c.toVCard(os, cm.getSupportedFields());

        vcard = new String(os.toByteArray());
        assertTrue(vcard.contains("TEL;VOICE;WORK:1111111111"));
        assertTrue(vcard.contains("TEL;VOICE;WORK:2222222222"));
        assertTrue(vcard.contains("TEL;VOICE;WORK;PREF:3333333333"));
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
        assertTrue(vcard.contains("EMAIL;INTERNET;PREF:other@other6.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other7.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET:other@other8.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home1.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;HOME:home@home2.com"));
        assertTrue(vcard.contains("EMAIL;INTERNET;WORK:work@work1.com"));
        assertTrue(vcard.contains("URL;HOME:www.home1.com"));
        assertTrue(vcard.contains("URL;HOME;PREF:www.home2.com"));
        assertTrue(vcard.contains("URL;HOME:www.home3.com"));
        assertTrue(vcard.contains("URL:www.other1.com"));
        assertTrue(vcard.contains("URL;WORK:www.work1.com"));
        assertTrue(vcard.contains("URL;WORK:www.work2.com"));
        assertTrue(vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1"));
        assertTrue(vcard.contains("ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country1"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country1"));
        assertTrue(vcard.contains("ADR;PREF:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country2"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country3"));
        assertTrue(vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country4"));
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG8x"));
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG8y"));
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG8z"));
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG80"));
        assertTrue(vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG81"));
        assertTrue(vcard.contains("PHOTO;PREF;ENCODING=BASE64:\r\n cHJlZl9waG90bw=="));
        
        c = new Contact();
        c.setVCard(("BEGIN:VCARD\r\n" +
                    "VERSION:2.1\r\n" +
                    "TEL;VOICE;WORK:\r\n" +
                    "TEL;VOICE;HOME:\r\n" +
                    "TEL;CELL;PREF:0000000000\r\n" +
                    "EMAIL;INTERNET:\r\n" +
                    "EMAIL;INTERNET;HOME:\r\n" +
                    "EMAIL;INTERNET;WORK;PREF:work@work1.com\r\n" +
                    "URL;HOME;PREF:www.home1.com\r\n" +
                    "URL;HOME:www.home2.com\r\n" +
                    "URL:\r\n" +
                    "URL;WORK:\r\n" +
                    "ADR;WORK:;;;;;;\r\n" +
                    "ADR;HOME:;;;;;;\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country1\r\n" +
                    "ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country2\r\n" +
                    "ADR;PREF:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country3\r\n" +
                    "PHOTO;ENCODING=BASE64;PREF:\r\n cGhvdG8x\r\n" +
                    "PHOTO;ENCODING=BASE64:\r\n cHJlZl9waG90bw==\r\n" +
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
        assertTrue(!vcard.contains("TEL;VOICE;WORK;PREF:3333333333"));
        assertTrue(!vcard.contains("TEL;VOICE;WORK:4444444444"));
        assertTrue(!vcard.contains("TEL;VOICE;WORK:5555555555"));
        assertTrue(!vcard.contains("TEL;VOICE;WORK:6666666666"));
        assertTrue(!vcard.contains("TEL;VOICE;HOME:7777777777"));
        assertTrue(!vcard.contains("TEL;VOICE;HOME:8888888888"));
        assertTrue( vcard.contains("TEL;CELL;PREF:0000000000"));
        assertTrue(!vcard.contains("EMAIL;INTERNET:other@other1.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET:other@other2.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET:other@other3.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET:other@other4.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET:other@other5.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET;PREF:other@other6.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET:other@other7.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET:other@other8.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET;HOME:home@home1.com"));
        assertTrue(!vcard.contains("EMAIL;INTERNET;HOME:home@home2.com"));
        assertTrue( vcard.contains("EMAIL;INTERNET;WORK;PREF:work@work1.com"));
        assertTrue( vcard.contains("URL;HOME;PREF:www.home1.com"));
        assertTrue( vcard.contains("URL;HOME:www.home2.com"));
        assertTrue(!vcard.contains("URL;HOME:www.home3.com"));
        assertTrue(!vcard.contains("URL:www.other1.com"));
        assertTrue(!vcard.contains("URL;WORK:www.work1.com"));
        assertTrue(!vcard.contains("URL;WORK:www.work2.com"));
        assertTrue(!vcard.contains("ADR;WORK:w_po;w_ex_address;w_street;w_city;w_state;w_code;w_country1"));
        assertTrue(!vcard.contains("ADR;HOME:h_po;h_ex_address;h_street;h_city;h_state;h_code;h_country1"));
        assertTrue( vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country1"));
        assertTrue( vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country2"));
        assertTrue( vcard.contains("ADR;PREF:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country3"));
        assertTrue(!vcard.contains("ADR:o_po;o_ex_address;o_street;o_city;o_state;o_code;o_country4"));
        assertTrue( vcard.contains("PHOTO;PREF;ENCODING=BASE64:\r\n cGhvdG8x"));
        assertTrue(!vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG8y"));
        assertTrue(!vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG8z"));
        assertTrue(!vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG80"));
        assertTrue(!vcard.contains("PHOTO;ENCODING=BASE64:\r\n cGhvdG81"));
        assertTrue( vcard.contains("PHOTO;ENCODING=BASE64:\r\n cHJlZl9waG90bw=="));

        List<Phone> pPhones = c.getPersonalDetail().getPhones();
        List<Phone> bPhones = c.getBusinessDetail().getPhones();
        List<Email> pEmails = c.getPersonalDetail().getEmails();
        List<Email> bEmails = c.getBusinessDetail().getEmails();
        List<WebPage> pWebPages = c.getPersonalDetail().getWebPages();
        List<WebPage> bWebPages = c.getBusinessDetail().getWebPages();
        List<Address> pAddresses = c.getPersonalDetail().getAddresses();
        List<Address> bAddresses = c.getBusinessDetail().getAddresses();
        List<Photo> pPhotos = c.getPersonalDetail().getPhotoObjects();

        assertEquals(pPhones.size(), 1);
        assertEquals(bPhones.size(), 0);
        assertEquals(pEmails.size(), 0);
        assertEquals(bEmails.size(), 1);
        assertEquals(pWebPages.size(), 2);
        assertEquals(bWebPages.size(), 0);
        assertEquals(pAddresses.size(), 3);
        assertEquals(bAddresses.size(), 0);
        assertEquals(pPhotos.size(), 2);

        for(Phone phone : pPhones) {
            assertTrue(phone.getPhoneType().equals(Phone.MOBILE_PHONE_NUMBER));
            if(phone.isPreferred()) {
                assertTrue("0000000000".equals(phone.getPropertyValueAsString()));
            }
        }
        for(Email email : bEmails) {
            assertTrue(email.getEmailType().equals(Email.WORK_EMAIL));
            if(email.isPreferred()) {
                assertTrue("work@work1.com".equals(email.getPropertyValueAsString()));
            }
        }
        for(WebPage webPage : pWebPages) {
            assertEquals(webPage.getWebPageType(), WebPage.HOME_WEBPAGE);
            if(webPage.isPreferred()) {
                assertTrue("www.home1.com".equals(webPage.getPropertyValueAsString()));
            }
        }
        for(Address address : pAddresses) {
            assertTrue(address.getAddressType().equals(Address.OTHER_ADDRESS));
            if(address.isPreferred()) {
                assertTrue("o_country3".equals(address.getCountry().getPropertyValueAsString()));
            }
        }
        for(Photo photo : pPhotos) {
            if(photo.isPreferred()) {
                assertTrue("photo1".equals(new String(photo.getImage())));
            }
        }
    }

}
