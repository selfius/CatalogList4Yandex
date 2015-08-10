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
import java.net.MalformedURLException;
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

    private final ContentResolver mContentResolver;

    public CatalogSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        HttpURLConnection urlConnection = null;
        try {
            URL sourceDataUrl = new URL("https://money.yandex.ru/api/categories-list");
            urlConnection = (HttpURLConnection) sourceDataUrl.openConnection();
            InputStream is = new BufferedInputStream(urlConnection.getInputStream());
            handleJSONStream(is);
        } catch (MalformedURLException e) {
            //will never happen
            throw new RuntimeException(e);
        } catch (IOException e) {
            //todo we should try to handle this somehow (let the user know we're out of connection)
            throw new RuntimeException(e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    protected void handleJSONStream(InputStream is) throws UnsupportedEncodingException {
        //todo parse with JSONReader here, and put them to db i think
        JsonReader jsonReader = new JsonReader(new InputStreamReader(is, "UTF-8"));
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        try {
            jsonReader.beginArray();
            batch.add(ContentProviderOperation.newDelete(CatalogContract.Element.CONTENT_URI).build());
            while (jsonReader.hasNext()) {
                batch.addAll(takeCareOfItem(jsonReader, null));
            }
            jsonReader.endArray();
            //remove persistence instructions from here
            mContentResolver.applyBatch(CatalogContract.CONTENT_AUTHORITY, batch);
            mContentResolver.notifyChange(
                    CatalogContract.Element.CONTENT_URI,
                    null,
                    false);
        } catch (IOException e) {
            //todo malformed json retrieved
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            //todo this will go away after refatoring
            throw new RuntimeException(e);
        } catch (OperationApplicationException e) {
            throw new RuntimeException(e);
        }
    }


    protected List<ContentProviderOperation> takeCareOfItem(JsonReader reader, Item parentItem) throws IOException {
        List<ContentProviderOperation> operationList = new LinkedList<>();

        Item item = new Item();
        reader.beginObject();
        item.setParentItem(parentItem);
        while (reader.hasNext()) {
            String name = reader.nextName();


            if ("title".equals(name)) {
                item.setTitle(reader.nextString());
            } else if ("id".equals(name)) {
                item.setYandexId(reader.nextInt());
            } else if ("subs".equals(name)) {
                reader.beginArray();
                while (reader.hasNext()) {
                    operationList.addAll(takeCareOfItem(reader, item));
                }
                reader.endArray();
            } else {
                reader.skipValue();
                //todo just log this
            }
        }
        operationList.add(ContentProviderOperation.newInsert(CatalogContract.Element.CONTENT_URI)
                .withValue(CatalogContract.Element.TITLE, item.getTitle())
                .withValue(CatalogContract.Element.YANDEX_ID, item.getYandexId())
                .build());

        reader.endObject();
        return operationList;
    }

    private class Item {
        String primaryId;
        String title;
        Integer yandexId;
        Item parentItem;

        public String getPrimaryId() {
            return primaryId;
        }

        public void setPrimaryId(String primaryId) {
            this.primaryId = primaryId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getYandexId() {
            return yandexId;
        }

        public void setYandexId(Integer yandexId) {
            this.yandexId = yandexId;
        }

        public Item getParentItem() {
            return parentItem;
        }

        public void setParentItem(Item parentItem) {
            this.parentItem = parentItem;
        }
    }
}
