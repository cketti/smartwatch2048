package sexy.fairly.smartwatch.game2048;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import sexy.fairly.smartwatch.game2048.util.IabHelper;
import sexy.fairly.smartwatch.game2048.util.IabResult;
import sexy.fairly.smartwatch.game2048.util.Inventory;
import sexy.fairly.smartwatch.game2048.util.Purchase;


public class GamePreferenceActivity extends PreferenceActivity {

    private static final int DIALOG_READ_ME = 1;


    private IabHelper mHelper;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference);

        // Handle read me
        findPreference(getText(R.string.preference_key_read_me))
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showDialog(DIALOG_READ_ME);
                        return true;
                    }
                });

        findPreference(getText(R.string.preference_key_support))
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_SENDTO);
                            intent.setData(Uri.parse("mailto:" + getString(R.string.support_email) +
                                "?subject=" + getString(R.string.support_subject)));
                            startActivity(intent);
                        } catch (Exception e) {
                            // Ignore
                        }
                        return true;
                    }
                });

        findPreference(getText(R.string.preference_key_buy_full_version))
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        BuyGameActivity.show(GamePreferenceActivity.this);
                        return true;
                    }
                });

        mHelper = new IabHelper(this, BuyGameActivity.LICENSE_PUBLIC_KEY);
        mHelper.enableDebugLogging(BuyGameActivity.DEBUG);
        startIabHelper();
    }

    private void startIabHelper() {
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
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
                return;
            }

            Purchase premiumPurchase = inventory.getPurchase(BuyGameActivity.SKU_FULL_VERSION);
            boolean isPremium = (premiumPurchase != null &&
                    BuyGameActivity.verifyDeveloperPayload(premiumPurchase));

            if (isPremium) {
                Preference preference = findPreference(getText(R.string.preference_key_buy_full_version));
                preference.setEnabled(false);
                preference.setTitle(R.string.preference_option_bought_full_version);
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
            case DIALOG_READ_ME:
                dialog = createReadMeDialog();
                break;
        }

        return dialog;
    }

    /**
     * Create the Read me dialog
     *
     * @return the Dialog
     */
    private Dialog createReadMeDialog() {
        String message = getString(R.string.preference_option_read_me_txt) +
                getString(R.string.preference_option_read_me_txt_free);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(R.string.preference_option_read_me)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        return builder.create();
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
