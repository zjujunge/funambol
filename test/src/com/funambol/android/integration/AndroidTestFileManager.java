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

package com.funambol.android.integration;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Vector;

import android.content.res.AssetManager;
import android.content.Context;
import com.funambol.client.test.util.TestFileManager;
import com.funambol.util.StringUtil;
import com.funambol.util.Log;



public class AndroidTestFileManager extends TestFileManager {

    private static final String TAG_LOG = "AndroidTestFileManager";

    private Context context;

    public AndroidTestFileManager(Context context) {
        super();
        this.context = context;
    }

    @Override
    protected String getScriptViaFile(String url) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        getFileViaFile(url, bos);
        return bos.toString();
    }

    @Override
    protected String getFileViaFile(String url, OutputStream output) throws Exception {
        String fileName = null;
        if (url.startsWith("file://")) {
            fileName = url.substring("file://".length());
        } else {
            throw new IOException("Invalid script file name " + url);
        }
        fileName = StringUtil.simplifyFileName(fileName);

        AssetManager assetManager = context.getAssets();
        InputStream is = assetManager.open(fileName);
        byte chunk[] = new byte[1024];
        int size;
        do {
            size = is.read(chunk);
            if (size > 0) {
                output.write(chunk, 0, size);
            }
        } while (size != -1);
        is.close();
        return getMimeTypeFromUrl(url);
    }
}
