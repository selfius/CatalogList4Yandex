package local.tai.cataloglist.sandbox.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by tai on 8/10/2015.
 */
public class AuthenticatorService extends Service {

    // Instance field that stores the authenticator object
    private AuthenticatorStub mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new AuthenticatorStub(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
