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


public class SamplePreferenceActivity extends PreferenceActivity {

    private static final int DIALOG_READ_ME = 1;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

}
