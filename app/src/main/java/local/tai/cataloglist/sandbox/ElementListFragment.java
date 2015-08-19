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

/**
 * Main fragment component for the application.
 * Displays lists of items in the given catalog
 */
public class ElementListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    /*this is unit is the most complex, i think i should break it down*/

    //TAG for topmost fragment
    public static final String ROOT_FRAGMENT_TAG = "rootOne";
    // An account type, in the form of a domain name
    private static final String ACCOUNT_TYPE = "local.tai.cataloglist";
    // The account name
    private static final String ACCOUNT = "dummyaccount";
    //field indexes in query result sets
    private static final int ID_IDX = 0;
    @SuppressWarnings("unused")
    private static final int TITLE_IDX = 1;
    private static final int IS_LEAF_IDX = 2;
    private static final String[] PROJECTION = new String[]{
            CatalogContract.Element._ID,
            CatalogContract.Element.TITLE,
            CatalogContract.Element.IS_LEAF
    };
    // List of Cursor columns to read from when preparing an adapter to populate the ListView.
    private static final String[] FROM_COLUMNS = new String[]{
            CatalogContract.Element.TITLE
    };
    //List of Views which will be populated by Cursor data.
    private static final int[] TO_FIELDS = new int[]{
            android.R.id.text1
    };
    //Key in shared preferences that shows whether it's first application launch or not
    private static final String SETUP_COMPLETE = "setup_complete";
    //just a key to store parent id value in a bundle, take a look at the method below
    private static final String PARENT_ID = "parentId";
    private Menu mOptionsMenu = null;
    private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
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
                    boolean syncActive = ContentResolver.isSyncActive(
                            account, CatalogContract.CONTENT_AUTHORITY);
                    boolean syncPending = ContentResolver.isSyncPending(
                            account, CatalogContract.CONTENT_AUTHORITY);
                    setRefreshActionButtonState(syncActive || syncPending);
                }
            });
        }
    };
    private Object mSyncObserverHandle;
    private SimpleCursorAdapter mAdapter;

    /**
     * This is a some kind of helper method to create fragment instance correctly
     * and pass ID argument to it
     *
     * @param parentId pass here id of the catalog content of which you want to display in this fragment
     * @return fragment instance
     */
    public static ElementListFragment getInstance(String parentId) {
        ElementListFragment f = new ElementListFragment();
        Bundle args = new Bundle();
        args.putString(PARENT_ID, parentId);
        f.setArguments(args);
        return f;
    }

    /**
     * Helper method to create account and make sure the account/provider is syncable
     *
     * @param context execution context
     * @return account
     */
    private static Account CreateSyncAccount(Context context) {
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            ContentResolver.setIsSyncable(newAccount, CatalogContract.CONTENT_AUTHORITY, 1);
        }
        return newAccount;
    }

    /**
     * Helper method to trigger an immediate sync ("refresh")
     */
    private static void TriggerRefresh(Account account) {
        Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(account, CatalogContract.CONTENT_AUTHORITY, b);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        boolean setupComplete = PreferenceManager
                .getDefaultSharedPreferences(getActivity()).getBoolean(SETUP_COMPLETE, false);
        if (!setupComplete) {

            TriggerRefresh(CreateSyncAccount(activity));
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .putBoolean(SETUP_COMPLETE, true).commit();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new SimpleCursorAdapter(
                getActivity(),
                android.R.layout.simple_list_item_activated_2, null, FROM_COLUMNS, TO_FIELDS, 0
        );
        setListAdapter(mAdapter);
        setEmptyText(getString(R.string.loading));
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Object parentId = getArguments().get(PARENT_ID);
        boolean parentIdIsEmpty = (parentId == null || parentId.toString().trim().length() == 0);
        return new CursorLoader(getActivity(),
                CatalogContract.Element.CONTENT_URI, PROJECTION,
                CatalogContract.Element.PARENT_ID + (parentIdIsEmpty ? " is null" : "=?"),
                parentIdIsEmpty ? null : new String[]{(String) parentId},
                CatalogContract.Element.TITLE + " desc");
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
            String oid = c.getString(ID_IDX);
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            ElementListFragment fragment = ElementListFragment.getInstance(oid);
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment, oid);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
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

    private void setRefreshActionButtonState(boolean refreshing) {
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // If the user clicks the "Refresh" button.
            case R.id.menu_refresh:
                TriggerRefresh(new Account(ACCOUNT, ACCOUNT_TYPE));
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                for (int i = 0; i < fragmentManager.getBackStackEntryCount(); ++i) {
                    //we should clear backstack
                    fragmentManager.popBackStack();
                }

                Fragment rootOne = fragmentManager.findFragmentByTag(ROOT_FRAGMENT_TAG);
                fragmentTransaction.detach(rootOne);
                fragmentTransaction.attach(rootOne);
                fragmentTransaction.commit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
