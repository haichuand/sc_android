package com.mono.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

/**
 * This class is used to provide a helper function to resolve encoded content URIs into path URIs.
 * Doing so enables easier file functions such as checking if the file exist or trying to determine
 * the size of the file that is not easily possible when working directly with content URIs.
 *
 * @author Gary Ng
 */
public class UriHelper {

    public static Uri resolve(Context context, Uri uri) {
        String scheme = uri.getScheme();

        if (scheme.equalsIgnoreCase("file")) {
            return uri;
        } else if (scheme.equalsIgnoreCase("content")) {
            Cursor cursor = null;

            String authority = uri.getAuthority();
            String id;

            if (authority.equalsIgnoreCase("com.android.externalstorage.documents")) {
                String[] args = DocumentsContract.getDocumentId(uri).split(":");
                String type = args[0], file = args[1];

                if (type.equalsIgnoreCase("primary")) {
                    // content://com.android.externalstorage.documents/document/primary%3A[File]
                    return Uri.parse(Environment.getExternalStorageDirectory() + "/" + file);
                } else {
                    // content://com.android.externalstorage.documents/document/0000-0000%3A[File]
                    return Uri.parse("/storage/sdcard1/" + file);
                }
            } else if (authority.equalsIgnoreCase("com.android.providers.downloads.documents")) {
                // content://com.android.providers.downloads.documents/document/[ID]
                id = DocumentsContract.getDocumentId(uri);
                uri = Uri.parse("content://downloads/public_downloads/" + id);
            } else if (authority.equalsIgnoreCase("com.android.providers.media.documents")) {
                // content://com.android.providers.media.documents/document/image%3A[ID]
                String[] args = DocumentsContract.getDocumentId(uri).split(":");
                String type = args[0];
                id = args[1];

                uri = Uri.parse("content://media/external/" + type + "/media/" + id);

                if (type.equalsIgnoreCase("image")) {
                    cursor = context.getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{
                            MediaStore.Images.Media.DATA
                        },
                        MediaStore.Images.Media._ID + " = ?",
                        new String[]{
                            id
                        },
                        null
                    );
                } else if (type.equalsIgnoreCase("video")) {
                    cursor = context.getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        new String[]{
                            MediaStore.Video.Media.DATA
                        },
                        MediaStore.Video.Media._ID + " = ?",
                        new String[]{
                            id
                        },
                        null
                    );
                }
            }

            if (cursor == null) {
                cursor = context.getContentResolver().query(
                    uri,
                    new String[]{
                        MediaStore.Files.FileColumns.DATA
                    },
                    null,
                    null,
                    null
                );
            }

            if (cursor != null) {
                int index = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);

                if (cursor.moveToNext()) {
                    uri = Uri.parse(cursor.getString(index));
                }

                cursor.close();
            }
        }

        return uri;
    }
}
