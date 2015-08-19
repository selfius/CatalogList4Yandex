package local.tai.cataloglist.sandbox;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import local.tai.cataloglist.sandbox.provider.CatalogContract;


public class ElementListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "local.tai.cataloglist";
    // The account name
    public static final String ACCOUNT = "dummyaccount";
    public static final int ID_IDX = 0;
    public static final int TITLE_IDX = 1;
    public static final int IS_LEAF_IDX = 2;
    private static final String[] PROJECTION = new String[]{
            CatalogContract.Element._ID,
            CatalogContract.Element.TITLE,
            CatalogContract.Element.IS_LEAF
    };
    /**
     * List of Cursor columns to read from when preparing an adapter to populate the ListView.
     */
    private static final String[] FROM_COLUMNS = new String[]{
            CatalogContract.Element.TITLE
    };
    /**
     * List of Views which will be populated by Cursor data.
     */
    private static final int[] TO_FIELDS = new int[]{
            android.R.id.text1
    };
    private SimpleCursorAdapter mAdapter;
    /**
     * Options menu used to populate ActionBar.
     */
    private Menu mOptionsMenu = null;
    /**
     * Crfate a new anonymous SyncStatusObserver. It's attached to the app's ContentResolver in
     * onResume(), and removed in onPause(). If status changes, it sets the state of the Refresh
     * button. If a sync is active or pending, the Refresh button is replaced by an indeterminate
     * ProgressBar; otherwise, the button itself is displayed.
     */
    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        /** Callback invoked with the sync adapter status changes. */
        @Override
        public void onStatusChanged(int which) {
            getActivity().runOnUiThread(new Runnable() {
                /**
                 * The SyncAdapter runs on a background thread. To update the UI, onStatusChanged()
                 * runs on the UI thread.
                 */
                @Override
                public void run() {
                    Account account = new Account(ACCOUNT, ACCOUNT_TYPE);
                    if (account == null) {

                        setRefreshActionButtonState(false);
                        return;
                    }

                    // Test the ContentResolver to see if the sync adapter is active or pending.
                    // Set the state of the refresh button accordingly.
                    boolean syncActive = ContentResolver.isSyncActive(
                            account, CatalogContract.CONTENT_AUTHORITY);
                    boolean syncPending = ContentResolver.isSyncPending(
                            account, CatalogContract.CONTENT_AUTHORITY);
                    setRefreshActionButtonState(syncActive || syncPending);
                }
            });
        }
    };
    /**
     * Handle to a SyncObserver. The ProgressBar element is visible until the SyncObserver reports
     * that the sync is complete.
     * <p/>
     * <p>This allows us to delete our SyncObserver once the application is no longer in the
     * foreground.
     */
    private Object mSyncObserverHandle;

    public static ElementListFragment getInstance(String parentId) {
        ElementListFragment f = new ElementListFragment();
        Bundle args = new Bundle();
        args.putString("parentId", parentId);
        f.setArguments(args);
        return f;
    }

    public static Account CreateSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            ContentResolver.setIsSyncable(newAccount, CatalogContract.CONTENT_AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(newAccount, CatalogContract.CONTENT_AUTHORITY, true);
            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(
                    newAccount, CatalogContract.CONTENT_AUTHORITY, new Bundle(), 60 * 60);
        } else {
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
        }
        return newAccount;
    }

    /**
     * Helper method to trigger an immediate sync ("refresh").
     * <p/>
     * <p>This should only be used when we need to preempt the normal sync schedule. Typically, this
     * means the user has pressed the "refresh" button.
     * <p/>
     * Note that SYNC_EXTRAS_MANUAL will cause an immediate sync, without any optimization to
     * preserve battery life. If you know new data is available (perhaps via a GCM notification),
     * but the user is not actively waiting for that data, you should omit this flag; this will give
     * the OS additional freedom in scheduling your sync request.
     */
    public static void TriggerRefresh(Account account) {
        Bundle b = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(
                account, // Sync account
                CatalogContract.CONTENT_AUTHORITY,                 // Content authority
                b);                                             // Extras
    }

    @Override
    public void onAttach(Activity activity) {


        super.onAttach(activity);
        boolean setupComplete = PreferenceManager
                .getDefaultSharedPreferences(getActivity()).getBoolean("setup_complete", false);
        if (!setupComplete) {
            TriggerRefresh(CreateSyncAccount(activity));
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .putBoolean("setup_complete", true).commit();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new SimpleCursorAdapter(
                getActivity(),       // Current context
                android.R.layout.simple_list_item_activated_2,  // Layout for individual rows
                null,                // Cursor
                FROM_COLUMNS,        // Cursor columns to use
                TO_FIELDS,           // Layout fields to use
                0                    // No flags
        );
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int i) {

                // Let SimpleCursorAdapter handle other fields automatically
                return false;
            }
        });
        setListAdapter(mAdapter);
        setEmptyText("Loading");
        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Object parentId = getArguments().get("parentId");
        boolean parentIdIsEmpty = (parentId == null || parentId.toString().trim().length() == 0);
        CursorLoader cursorLoader = new CursorLoader(getActivity(),
                CatalogContract.Element.CONTENT_URI, PROJECTION,
                parentIdIsEmpty ? "parent_id is null" : "parent_id=?",
                parentIdIsEmpty ? null : new String[]{(String) parentId},
                CatalogContract.Element.TITLE + " desc");
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Cursor c = (Cursor) mAdapter.getItem(position);
        if (c.getInt(IS_LEAF_IDX) == CatalogSyncAdapter.IS_CATEGORY) {
            String oid = c.getString(ID_IDX); //c'os it _ID, right?
            //FragmentManager fragmentManager = getActivity().getFragmentManager();
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            ElementListFragment fragment = ElementListFragment.getInstance(oid);
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();


            //Here we should replace the activity

            fragmentTransaction.replace(R.id.fragment_container, fragment, oid);
            fragmentTransaction.addToBackStack(null);

            fragmentTransaction.commit();

            //fragmentTransaction = fragmentManager.beginTransaction();
            //fragmentManager.popBackStack();
        }
        boolean some = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mOptionsMenu = menu;
        inflater.inflate(R.menu.menu, menu);
    }

    public void setRefreshActionButtonState(boolean refreshing) {
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.waiting_sync);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSyncStatusObserver.onStatusChanged(0);

        // Watch for sync state changes
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    /**
     * Respond to user gestures on the ActionBar.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // If the user clicks the "Refresh" button.
            case R.id.menu_refresh:
                TriggerRefresh(new Account(ACCOUNT, ACCOUNT_TYPE));
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                for (int i = 0; i < fragmentManager.getBackStackEntryCount(); ++i) {
                    fragmentManager.popBackStack();
                }
                //Fragment rootOne = fragmentManager.findFragmentByTag("rootOne");
                //fragmentTransaction.add(R.id.fragment_container, rootOne, "rootOne");
                Fragment rootOne = fragmentManager.findFragmentByTag("rootOne");
                fragmentTransaction.detach(rootOne);
                fragmentTransaction.attach(rootOne);
                fragmentTransaction.commit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
