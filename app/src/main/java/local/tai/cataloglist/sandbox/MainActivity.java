package local.tai.cataloglist.sandbox;


import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        ElementListFragment fragment = ElementListFragment.getInstance("");
        fragmentTransaction.add(R.id.fragment_container, fragment, "rootOne");
        fragmentTransaction.commit();
    }
}
