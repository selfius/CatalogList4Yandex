package local.tai.cataloglist.sandbox.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract for out persistence stuff
 * Describes how and where we will store data
 */
public class CatalogContract {

    /**
     * Single table name for all our items
     */
    public static final String TABLE_NAME = "catalog";

    /**
     * ca
     */
    public static final String CONTENT_AUTHORITY = "local.tai.cataloglist";

    public static final String CATALOG_PATH = "cataloglist";

    /**
     * base for out content uris
     * they should look like content://local.tai.cataloglist/cataloglist/
     */
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * No instances for this class
     */
    private CatalogContract() {
    }

    /**
     * Our backend scheme
     */
    public interface Element extends BaseColumns {

        /**
         * type for multiple items
         */
        String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/items";

        /**
         * type for single item
         */
        String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/item";

        /**
         * base url + path
         */
        Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(CATALOG_PATH).build();

        /**
         * Title field, same json field
         */
        String TITLE = "title";

        /**
         * Field to store json "id" field
         */
        String YANDEX_ID = "yandex_id";

        /**
         * Reference to parent record (we have a tree of this items)
         */
        String PARENT_ID = "parent_id";

        /**
         * Flag that shows that we have no child records
         * It's a denormalized field, we can run actual query to count child records,
         * but i don't think it's a good idea to do so every time we tap something in UI
         */
        String IS_LEAF = "is_leaf";

    }
}
