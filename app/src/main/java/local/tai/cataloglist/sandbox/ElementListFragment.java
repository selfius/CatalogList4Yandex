package local.tai.cataloglist.sandbox;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
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
    private static final String[] PROJECTION = new String[]{
            CatalogContract.Element._ID,
            CatalogContract.Element.TITLE

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
        TriggerRefresh(CreateSyncAccount(activity));
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
        String oid = c.getString(0); //c'os it _ID, right?
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        ElementListFragment fragment = ElementListFragment.getInstance(oid);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
