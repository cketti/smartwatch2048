package sexy.fairly.smartwatch.game2048;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import sexy.fairly.smartwatch.game2048.util.PurchaseHelper;


@SuppressWarnings("deprecation")
public class GamePreferenceActivity extends PreferenceActivity {

    private static final int DIALOG_INSTRUCTIONS = 1;


    private PurchaseHelper mPurchaseHelper;
    private PurchaseHelper.PurchaseListener mPurchaseListener = new PurchaseListener();
    private boolean mIsFullVersion = false;
    private ListPreference mMoveMode;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        PreferenceManager.setDefaultValues(this, R.xml.preference, true);
        addPreferencesFromResource(R.xml.preference);

        setupMoveModePreference();

        setupInstructionsPreference();
        setupPremiumVersionPreference();

        setupVersionPreference();
        setupSupportPreference();
        setupGooglePlusPreference();

        mPurchaseHelper = new PurchaseHelper(this, mPurchaseListener);
    }

    private void setupMoveModePreference() {
        mMoveMode = (ListPreference) findPreference(getString(R.string.preference_key_move_mode));
        setMoveMoveSummary(mMoveMode.getValue());
        mMoveMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!mMoveMode.getValue().equals(newValue)) {
                    GameExtensionService.settingsChanged(getApplicationContext());
                }

                setMoveMoveSummary((String) newValue);

                return true;
            }
        });
    }

    private void setupInstructionsPreference() {
        findPreference(getText(R.string.preference_key_instructions))
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showDialog(DIALOG_INSTRUCTIONS);
                        return true;
                    }
                });
    }

    private void setupPremiumVersionPreference() {
        Preference buyPreference = findPreference(getText(R.string.preference_key_premium_version));
        buyPreference.setSummary(R.string.preference_summary_loading_purchase_state);
        buyPreference.setEnabled(false);
        buyPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    BuyGameActivity.show(GamePreferenceActivity.this);
                    return true;
                }
        });
    }

    private void setupVersionPreference() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            findPreference(getText(R.string.preference_key_version)).setSummary(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            // Ignore
        }
    }

    private void setupSupportPreference() {
        findPreference(getText(R.string.preference_key_support))
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_SENDTO);
                            intent.setData(Uri.parse("mailto:" + getString(R.string.support_email) +
                                    "?subject=" + getString(R.string.support_subject)));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (Exception e) {
                            // Ignore
                        }
                        return true;
                    }
                });
    }

    private void setupGooglePlusPreference() {
        findPreference(getText(R.string.preference_key_google_plus))
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(getString(R.string.google_plus_url)));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (Exception e) {
                            // Ignore
                        }
                        return true;
                    }
                });
    }

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
            case DIALOG_INSTRUCTIONS:
                dialog = createReadMeDialog();
                break;
        }

        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DIALOG_INSTRUCTIONS: {
                ((AlertDialog) dialog).setMessage(getReadMeMessage());
                break;
            }
        }
    }

    private String getReadMeMessage() {
        MoveMode moveMode;
        try {
            moveMode = MoveMode.valueOf(mMoveMode.getValue());
        } catch (IllegalArgumentException e) {
            moveMode = MoveMode.CLICK;
        }

        String message;
        switch (moveMode) {
            default:
            case CLICK: {
                message = getString(R.string.preference_option_instructions_txt_click);
                break;
            }
            case SWIPE: {
                message = getString(R.string.preference_option_instructions_txt_swipe);
                break;
            }
            case CLICK_OR_SWIPE: {
                message = getString(R.string.preference_option_instructions_txt_click_or_swipe);
                break;
            }
        }

        if (!mIsFullVersion) {
            message += getString(R.string.preference_option_instructions_txt_free);
        }

        return message;
    }

    private Dialog createReadMeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("")
                .setTitle(R.string.preference_option_instructions)
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
        mPurchaseHelper.destroy();
    }

    private void setMoveMoveSummary(String value) {
        int index = mMoveMode.findIndexOfValue(value);
        if (index == -1) {
            mMoveMode.setSummary("");
        } else {
            mMoveMode.setSummary(mMoveMode.getEntries()[index]);
        }
    }

    private void removeBuyPreference() {
        Preference buyPreference = findPreference(
                getString(R.string.preference_key_premium_version));
        PreferenceCategory miscCategory = (PreferenceCategory) findPreference(
                getString(R.string.preference_key_miscellaneous));
        miscCategory.removePreference(buyPreference);
    }

    class PurchaseListener implements PurchaseHelper.PurchaseListener {
        @Override
        public void purchaseState(boolean fullVersion) {
            Preference preference = findPreference(
                    getText(R.string.preference_key_premium_version));
            if (fullVersion) {
                mIsFullVersion = true;
                preference.setSummary(R.string.preference_summary_bought_full_version);
            } else {
                preference.setEnabled(true);
                preference.setSummary(R.string.preference_summary_buy_full_version);
            }
        }

        @Override
        public void billingNotAvailable() {
            removeBuyPreference();
        }
    }
}
