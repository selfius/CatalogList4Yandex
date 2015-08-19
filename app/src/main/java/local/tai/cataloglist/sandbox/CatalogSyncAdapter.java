package local.tai.cataloglist.sandbox;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.JsonReader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import local.tai.cataloglist.sandbox.provider.CatalogContract;

/**
 * Sync adapter for catalog
 * Created by Andrey Sviridov on 8/9/2015.
 */
public class CatalogSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final int IS_CATEGORY = 0;
    private static final int IS_LEAF = 1;
    private static final String SOURCE_URL = "https://money.yandex.ru/api/categories-list";

    private final ContentResolver mContentResolver;

    public CatalogSyncAdapter(Context context) {
        super(context, true);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        HttpURLConnection urlConnection = null;
        try {
            URL sourceDataUrl = new URL(SOURCE_URL);
            urlConnection = (HttpURLConnection) sourceDataUrl.openConnection();
            InputStream is = new BufferedInputStream(urlConnection.getInputStream());
            handleJSONStream(is);
        } catch (IOException e) {
            //todo we should try to handle this somehow (let the user know we're out of connection)
            throw new RuntimeException(e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private void handleJSONStream(InputStream is) throws UnsupportedEncodingException {
        JsonReader jsonReader = new JsonReader(new InputStreamReader(is, "UTF-8"));
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        try {
            jsonReader.beginArray();
            batch.add(ContentProviderOperation.newDelete(CatalogContract.Element.CONTENT_URI).build()); //0 operation
            int currentOperationIdx = 0;
            while (jsonReader.hasNext()) {
                List<ContentProviderOperation> items = takeCareOfItem(jsonReader, null, currentOperationIdx);
                currentOperationIdx += items.size();
                batch.addAll(items);
            }
            jsonReader.endArray();
            //remove persistence instructions from here
            mContentResolver.applyBatch(CatalogContract.CONTENT_AUTHORITY, batch);
            mContentResolver.notifyChange(
                    CatalogContract.Element.CONTENT_URI,
                    null,
                    false);
        } catch (IOException | RemoteException | OperationApplicationException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ContentProviderOperation> takeCareOfItem(JsonReader reader,
                                                          Item parentItem,
                                                          final int backReferenceIndex)
            throws IOException {
        List<ContentProviderOperation> operationList = new LinkedList<>();


        Item item = new Item();

        List<ContentProviderOperation> innerOperations = new ArrayList<>();
        /*we want leaf-items to be created after their parent,
        to be able to use back references in parent_id field,
        and we can't rely on json structure, so we put them in this temp array and just append to
        the main list of operations*/

        reader.beginObject();
        int currentItemBackReferenceIndex = backReferenceIndex + 1;
        while (reader.hasNext()) {
            String name = reader.nextName();

            if ("title".equals(name)) {
                item.title = reader.nextString();
            } else if ("id".equals(name)) {
                item.yandexId = reader.nextInt();
            } else if ("subs".equals(name)) {
                reader.beginArray();
                while (reader.hasNext()) {
                    List<ContentProviderOperation> innerItems = takeCareOfItem(reader, item, currentItemBackReferenceIndex);
                    innerOperations.addAll(innerItems);
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(CatalogContract.Element.CONTENT_URI)
                .withValue(CatalogContract.Element.TITLE, item.title)
                .withValue(CatalogContract.Element.YANDEX_ID, item.yandexId)
                .withValue(CatalogContract.Element.IS_LEAF, innerOperations.size() > 0 ? IS_CATEGORY : IS_LEAF);
        if (parentItem != null) {
            builder.withValueBackReference(CatalogContract.Element.PARENT_ID, backReferenceIndex);
        }
        operationList.add(builder.build());
        operationList.addAll(innerOperations);

        return operationList;
    }

    private class Item {
        /* this is a simple struct for the single json object...
        don't think we need a ton of getters/setters here actually*/
        String title;
        Integer yandexId;
    }
}
