package local.tai.cataloglist.sandbox.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import local.tai.cataloglist.sandbox.CatalogSyncAdapter;

/**
 * Service to retrieve data from network and persist it
 * Created by tai on 8/8/2015.
 */
//todo think about semantic of the name
public class SomeService extends Service {

    private static final Object lock = new Object();
    private static CatalogSyncAdapter adapter = null;

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (lock) {
            if (adapter == null) {
                adapter = new CatalogSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return adapter.getSyncAdapterBinder();
    }
}
