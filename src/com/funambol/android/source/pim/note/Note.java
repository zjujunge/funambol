/*
 * Funambol is a mobile platform developed by Funambol, Inc.
 * Copyright (C) 2011 Funambol, Inc.
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

package com.funambol.android.source.pim.note;

import java.util.TimeZone;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.funambol.common.pim.model.common.Property;
import com.funambol.util.Log;

/**
 * A Note item with a body and title.
 */
public class Note {

    private static final String TAG = "Note";

    private long id = -1;
    private Property title;
    private Property body;

    public Note() {
        super();
    }

    public void setId(String id) {
        setId(Long.parseLong(id));
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setTitle(Property title) {
        this.title = title;
    }

    public Property getTitle() {
        return title;
    }

    public void setBody(Property body) {
        this.body = body;
    }

    public Property getBody() {
        return body;
    }

    public void setPlainText(byte note[]) throws UnsupportedEncodingException {
        // there is just a body for plain notes
        String bodyValue = new String(note, "UTF-8");
        body = new Property(bodyValue);

        // Extract the title
        int lfIdx = bodyValue.indexOf("\n");
        if (lfIdx >= 0) {
            title = new Property(bodyValue.substring(0, lfIdx));
        } else {
            if (bodyValue.length() < 32) {
                title = new Property(bodyValue);
            }
        }
    }

    public void toPlainText(OutputStream os, boolean allFields) throws IOException {
        try {
            String bodyValue = Property.stringFrom(body);
            if (bodyValue == null) {
                bodyValue = "";
            }
            byte bytes[] = bodyValue.getBytes("UTF-8");
            os.write(bytes);
        } catch (Exception e) {
            Log.error(TAG, "Cannot format plain note", e);
            throw new IOException("Cannot format note");
        }
    }
}
