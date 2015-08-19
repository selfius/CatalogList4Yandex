package local.tai.cataloglist.sandbox.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import local.tai.cataloglist.sandbox.toremove.SelectionBuilder;

/**
 * Created by tai on 8/9/2015.
 */
public class CatalogDataProvider extends ContentProvider {

    public static final int ITEM = 1;
    public static final int ITEMS_ID = 2;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static CatalogDatabase databaseHelper;

    static {
        sUriMatcher.addURI(CatalogContract.CONTENT_AUTHORITY, CatalogContract.CATALOG_PATH, ITEM);
        sUriMatcher.addURI(CatalogContract.CONTENT_AUTHORITY, CatalogContract.CATALOG_PATH + "/*", ITEMS_ID);
    }

    @Override
    public boolean onCreate() {
        databaseHelper = new CatalogDatabase(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case ITEMS_ID:
                // Return a single entry, by ID.
                String id = uri.getLastPathSegment();
                builder.where(CatalogContract.Element._ID + "=?", id);
            case ITEM:
                // Return all known entries.
                builder.table(CatalogContract.TABLE_NAME)
                        .where(selection, selectionArgs);
                Cursor c = builder.query(db, projection, sortOrder);
                // Note: Notification URI must be manually set here for loaders to correctly
                // register ContentObservers.
                Context ctx = getContext();
                assert ctx != null;
                c.setNotificationUri(ctx.getContentResolver(), uri);
                return c;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEM:
                return CatalogContract.Element.CONTENT_TYPE;
            case ITEMS_ID:
                return CatalogContract.Element.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();
        assert db != null;
        final int match = sUriMatcher.match(uri);
        Uri result;
        switch (match) {
            case ITEM:
                long id = db.insertOrThrow(CatalogContract.TABLE_NAME, null, values);
                result = Uri.parse(CatalogContract.Element.CONTENT_URI + "/" + id);
                break;
            case ITEMS_ID:
                throw new UnsupportedOperationException("Insert not supported on URI: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return result;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ITEM:
                count = builder.table(CatalogContract.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case ITEMS_ID:
                String id = uri.getLastPathSegment();
                count = builder.table(CatalogContract.TABLE_NAME)
                        .where(CatalogContract.Element._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ITEM:
                count = builder.table(CatalogContract.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case ITEMS_ID:
                String id = uri.getLastPathSegment();
                count = builder.table(CatalogContract.TABLE_NAME)
                        .where(CatalogContract.Element._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    static class CatalogDatabase extends SQLiteOpenHelper {

        public static final int VERSION = 1;
        public static final String DB_NAME = "catalogs";

        public static final String CREATE_SCHEMA =
                "CREATE TABLE " + CatalogContract.TABLE_NAME + " (" +
                        CatalogContract.Element._ID + " INTEGER PRIMARY KEY, " +
                        CatalogContract.Element.TITLE + " TEXT, " +
                        CatalogContract.Element.YANDEX_ID + " INTEGER UNIQUE, " +
                        CatalogContract.Element.PARENT_ID + " INTEGER, " +
                        CatalogContract.Element.IS_LEAF + " INTEGER, " +
                        "FOREIGN KEY(" + CatalogContract.Element.PARENT_ID + ") REFERENCES " + CatalogContract.TABLE_NAME + "(" + CatalogContract.Element._ID + "))";
        public static final String DROP_SCHEMA = "DROP TABLE IF EXISTS " + CatalogContract.TABLE_NAME;

        public CatalogDatabase(Context context) {
            super(context, DB_NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_SCHEMA);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(DROP_SCHEMA);
            onCreate(db);
        }
    }
}
