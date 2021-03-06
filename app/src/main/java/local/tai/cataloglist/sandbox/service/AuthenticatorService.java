package local.tai.cataloglist.sandbox.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Authenticator Service
 * We do not really need authentication, but api enforces us to have one
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
