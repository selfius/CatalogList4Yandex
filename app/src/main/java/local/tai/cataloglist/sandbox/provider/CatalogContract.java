package local.tai.cataloglist.sandbox.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by tai on 8/9/2015.
 */
public class CatalogContract {

    public static final String TABLE_NAME = "catalog";
    public static final String CONTENT_AUTHORITY = "local.tai.cataloglist";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String CATALOG_PATH = "cataloglist";

    private CatalogContract() {
    }

    public interface Element extends BaseColumns {

        String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/items";

        String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/item";

        Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(CATALOG_PATH).build();

        String TITLE = "title";

        String YANDEX_ID = "yandex_id";

        String PARENT_ID = "parent_id";

    }
}
