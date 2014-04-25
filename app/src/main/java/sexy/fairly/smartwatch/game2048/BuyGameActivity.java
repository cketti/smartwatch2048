package sexy.fairly.smartwatch.game2048;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import sexy.fairly.smartwatch.game2048.util.IabHelper;
import sexy.fairly.smartwatch.game2048.util.IabResult;
import sexy.fairly.smartwatch.game2048.util.Inventory;
import sexy.fairly.smartwatch.game2048.util.Purchase;


public class BuyGameActivity extends Activity {
    public static final boolean DEBUG = false;

    public static final String SKU_FULL_VERSION = "full_version";
    public static final String LICENSE_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjIOpxkDWlUW+/eMjMn7x4fAQGTs7" +
            "Ef5xLEmDeLy3U8cG31rmi0d7YtjYQNrJx4hN8KDDVl0WRsNL5B9Yb+u9K0NiJ8Tf+CZHCoM0" +
            "l4490FSX971PRmLaQ4NZMFSmJITeum0JhNhuB/rMMmT/nbHEaDJcDf3nfQLrngTli+4YbSBi" +
            "BRqmnCzA9NfHOv6qRA090ipAJFAQBnKGpsbm6G29Q0+OWN1lZY64TD7dQuJMkKZzKMDxFE3B" +
            "9FL9a65D5RIZ1bwr7hmxHKsuNPpFIJRjQYHU/2sk2rYd+lfSvYqs+Vc8LdQR6hNlwtFHlm9S" +
            "vkxFwUfKOJg0ROrd6ovpRVV8hQIDAQAB";

    private static final int RC_REQUEST = 10001;
    private static final String EXTRA_DISPLAY_SUCCESS_MESSAGE = "display_success_message";


    public static void show(Context context) {
        Intent intent = new Intent(context, BuyGameActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }


    private IabHelper mHelper;
    private boolean mDisplaySuccessMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_game);

        mHelper = new IabHelper(this, LICENSE_PUBLIC_KEY);
        mHelper.enableDebugLogging(DEBUG);

        if (savedInstanceState != null) {
            mDisplaySuccessMessage = savedInstanceState.getBoolean(EXTRA_DISPLAY_SUCCESS_MESSAGE);
        } else {
            mDisplaySuccessMessage = false;
        }

        if (mDisplaySuccessMessage) {
            displaySuccessMessage();
        } else {
            startIabHelper();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_DISPLAY_SUCCESS_MESSAGE, mDisplaySuccessMessage);
    }

    private void startIabHelper() {
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    //complain("Problem setting up in-app billing: " + result);
                    finish();
                    return;
                }

                if (mHelper == null) return;

                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (mHelper == null) return;

            if (result.isFailure()) {
                //complain("Failed to query inventory: " + result);
                finish();
                return;
            }

            Purchase premiumPurchase = inventory.getPurchase(SKU_FULL_VERSION);
            boolean isPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));

            if (isPremium) {
                GameExtensionService.purchaseComplete(getApplicationContext());
                finish();
            }

            String payload = "";
            mHelper.launchPurchaseFlow(BuyGameActivity.this, SKU_FULL_VERSION, RC_REQUEST,
                    mPurchaseFinishedListener, payload);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mHelper == null || !mHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /** Verifies the developer payload of a purchase. */
    public static boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        return true;
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (mHelper == null) return;

            if (result.isFailure()) {
                GameExtensionService.purchaseCancelled(getApplicationContext());
                //complain("Error purchasing: " + result);
                finish();
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                GameExtensionService.purchaseCancelled(getApplicationContext());
                //complain("Error purchasing. Authenticity verification failed.");
                finish();
                return;
            }

            if (!purchase.getSku().equals(SKU_FULL_VERSION)) {
                return;
            }

            displaySuccessMessage();
            GameExtensionService.purchaseComplete(getApplicationContext());
        }
    };

    private void displaySuccessMessage() {
        mDisplaySuccessMessage = true;
        findViewById(R.id.thanks_for_buying_message).setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }
}
