/**
 * Funambol is a mobile platform developed by Funambol, Inc. 
 * Copyright (C) 2003 - 2011 Funambol, Inc.
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

package com.funambol.android.providers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.funambol.platform.FileAdapter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;

/**
 * A content provider for accessing files in MediaHub folder
 */
public class FilesContentProvider extends ContentProvider {

    public static final Uri CONTENT_URI  = Uri.parse("content://com.funambol.files");

    /* (non-Javadoc)
     * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#onCreate()
     */
    @Override
    public boolean onCreate() {
        //TODO: create hashmap with all files in the folder
        return true;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        
//        String allFields[] = {_ID, NAME, DATA, SIZE, DATE_ADDED, DATE_MODIFIED, MIME_TYPE};
//        
//        MatrixCursor res = null;
//        int numCols;
//        if (projection == null) {
//            res = new MatrixCursor(allFields);
//            numCols = allFields.length;
//        } else {
//            res = new MatrixCursor(projection);
//            numCols = projection.length;
//        }
//
//        do {
//            Object row[] = new Object[numCols];
//        }

        
        return null;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        File file = new File(uri.getPath());
        ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        return parcel;
        
//      //here u should mention the path from where u want to get the data
//        URI uri1 = URI.create("file:///data/data/package name /files"+uri.getPath());
//        if u are requesting for file system .here the path what u are sending ,that here will replace(uri.getPath())
//        File file = new File(uri1.getPath());
//        ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
//        return parcel;        
    }


    /**
     * Check the size of a file on the file system
     * @param fullName the file name with path
     * @return long the size on the file long formatted
     * @throws IOException if the FileAdapter pointing to the file encounters a
     * problem
     */
    private long getFileSize(String fullName) throws IOException {
        //Manages to find the device's file using the FS
        long pictureSize;

        FileAdapter fa = new FileAdapter(fullName);
        pictureSize = fa.getSize();
        fa = null;
        return pictureSize;
    }
}
