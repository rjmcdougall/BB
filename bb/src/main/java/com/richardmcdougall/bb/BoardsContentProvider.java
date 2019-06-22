package com.richardmcdougall.bb;

import java.util.ArrayList;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class BoardsContentProvider extends ContentProvider {

   // public static final String TAG = "m039";

    public FindMyFriends mFindMyFriends = null;

    public static final String AUTHORITY = "com.richardmcdougall.bb";

    public BoardsContentProvider() {

    }

    public BoardsContentProvider(FindMyFriends fmf) {
        mFindMyFriends = fmf;
    }

    private static class JSONRow {
        private int ID;
        public String JSON;

        public JSONRow() {
            ID = 1;
            JSON = "";
        }

        public JSONRow(int i,String JSON) {
            this.ID = i;
            this.JSON = JSON;
        }
    }

    static ArrayList<JSONRow> sJSON = new ArrayList<JSONRow>();

    {
       // mFindMyFriends.getBoardLocationsJSON(0);
        sJSON.add(new JSONRow(1,"JSON STRING"));
    }

    public static final class Words {
        public static class JSON {
            public static final String JSON_TYPE =
                    "vnd.android.cursor.dir" + "/" + AUTHORITY + "." + JSONColumns.JSON;

            public static final Uri CONTENT_URI;

            static {
                CONTENT_URI = Uri
                        .parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY + "/" + JSONColumns.JSON);
            }

        }

        public interface JSONColumns extends BaseColumns {
            String JSON = "json";
        }
    }

    private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int GET_JSON = 1;

    static {
        sUriMatcher.addURI(AUTHORITY, Words.JSONColumns.JSON, GET_JSON);
    }

    @Override
    public Cursor query (Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (sUriMatcher.match(uri)) {
            case GET_JSON:
            {
                int count = sJSON.size();
                Cursor cursors[] = new Cursor[count];

                for(int i = 0; i < count; i++) {
                    cursors[i] = query(ContentUris.withAppendedId(uri, i),
                            projection,
                            selection,
                            selectionArgs,
                            sortOrder);
                }

                return new MergeCursor(cursors);
            }

            default:
                throw new IllegalArgumentException("Invalide URI");
        }
    }

    //not implemented.
    @Override
    public Uri insert (Uri uri, ContentValues values) {
        return null;
    }

    // not implemented
    @Override
    public int delete (Uri uri, String selection, String[] selectionArgs) {
        return -1;
    }

    // not implemented
    @Override
    public int update (Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return -1;
    }

    @Override
    public boolean onCreate () {
        return true;
    }

    @Override
    public String getType (Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case GET_JSON:
                return Words.JSON.JSON_TYPE;
            default:
                return null;
        }

    }

}

