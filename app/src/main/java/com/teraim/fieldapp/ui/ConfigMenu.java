package com.teraim.fieldapp.ui;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.BarcodeReader;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ConfigMenu is now an AppCompatActivity that hosts the SettingsFragment.
 * This is the standard approach for settings screens using the AndroidX Preference library.
 */
public class ConfigMenu extends AppCompatActivity {

	private static boolean anyChange = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		anyChange = false;
		// Display the fragment as the main content.
		// We use getSupportFragmentManager() which is the AndroidX version.
		getSupportFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment())
				.commit();
		setTitle(R.string.settings);
	}

	/**
	 * The SettingsFragment now extends PreferenceFragmentCompat from the AndroidX library.
	 */
	public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

		private EditTextPreference serverPref;
		private EditTextPreference exp_serverPref;
		private EditTextPreference teamPref;
		private EditTextPreference userPref;
		private EditTextPreference appPref;
		private CheckBoxPreference devFuncPref;
		private AlertDialog progressDialog; // Use AlertDialog for progress display

		// Note: onActivityResult is deprecated. For a modern approach, consider using the Activity Result APIs.
		// However, for a direct migration, this will still function.
		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode,resultCode,data);
			Log.d("vortex", "IN ONACTIVITY RESULT ");

			Log.d("vortex", "request code " + requestCode + " result code " + resultCode);
			if (requestCode == Constants.QR_SCAN_REQUEST) {
				if (Activity.RESULT_OK == resultCode) {
					Log.d("vortex", "code img taken...scan!");
					String url = (new BarcodeReader(requireActivity())).analyze();

					Log.d("vortex", "GOT " + (url == null ? "null" : url));


					//www.teraim.com?project=Rlotst&team=Rlo2017&name=Lotta&sync=Internet&control=major
					if (url != null) {
						Uri uri = Uri.parse(url);

						final String application = uri.getQueryParameter("application");
						final String team = uri.getQueryParameter("team");
						final String name = uri.getQueryParameter("name");
						final String server = uri.getPath();

						(new AlertDialog.Builder(requireActivity())).setTitle("Recieved QR configuration")
								.setMessage("The following QR setting was received:" +
										Tools.printIfNotNull("\nApplication: ", application) +
										Tools.printIfNotNull("\nTeam: ", team) +
										Tools.printIfNotNull("\nName: ", name) +
										Tools.printIfNotNull("\nServer: ", server) +
										"\n********************" +
										"\nApply these changes?"

								)
								.setCancelable(false)
								.setNegativeButton(R.string.cancel, (dialog, which) -> {

								})
								.setPositiveButton(R.string.ok, (dialog, which) -> {


									if (team != null)
										teamPref.setText(team);

									if (application != null) {
										appPref.setText(application);
									}
									if (name != null)
										userPref.setText(name);
									if (server != null)
										serverPref.setText(server);
								})
								.show();
						askForRestart();

					} else {
						new AlertDialog.Builder(requireActivity()).setTitle("Error")
								.setMessage("NO QR code found in image.")
								.setPositiveButton(R.string.ok, (dialog, which) -> {})
								.setCancelable(false)
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();
					}
				}
			}
		}


		/**
		 * In PreferenceFragmentCompat, we use onCreatePreferences instead of onCreate.
		 * This is where the preference hierarchy is created.
		 * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
		 * @param rootKey If non-null, this preference fragment should be rooted at the PreferenceScreen with this key.
		 */
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			// Set the preference manager to use the shared preferences file we want.
			getPreferenceManager().setSharedPreferencesName(Constants.GLOBAL_PREFS);
			// Load the preferences from an XML resource.
			setPreferencesFromResource(R.xml.configmenu, rootKey);

			//Create a filter that stops users from entering disallowed characters.
			InputFilter filter = (source, start, end, dest, dstart, dend) -> {
				boolean keepOriginal = true;
				StringBuilder sb = new StringBuilder(end - start);
				for (int i = start; i < end; i++) {
					char c = source.charAt(i);
					if (isCharAllowed(c))
						sb.append(c);
					else
						keepOriginal = false;
				}
				if (keepOriginal)
					return null;
				else {
					if (source instanceof Spanned) {
						SpannableString sp = new SpannableString(sb);
						TextUtils.copySpansFrom((Spanned) source, start, sb.length(), null, sp, 0);
						return sp;
					} else {
						return sb;
					}
				}
			};

			devFuncPref = findPreference("dev_switch");
			teamPref = findPreference(PersistenceHelper.LAG_ID_KEY);
			String syncGroupText = teamPref.getText();

			if (!isEmpty(syncGroupText))
				teamPref.setSummary(syncGroupText);
			else {
				teamPref.setText(null);
				teamPref.setSummary(getText(R.string.Team_s));
			}
			teamPref.setEnabled(devFuncPref.isChecked());
			// In AndroidX, we use a listener to access the EditText view to set filters.
			teamPref.setOnBindEditTextListener(editText -> editText.setFilters(new InputFilter[] {filter}));

			userPref = findPreference(PersistenceHelper.USER_ID_KEY);
			String syncUserText = userPref.getText();
			if (!isEmpty(syncUserText))
				userPref.setSummary(syncUserText);
			else {
				userPref.setText(null);
				userPref.setSummary(getText(R.string.UserName_dm));
			}
			userPref.setOnBindEditTextListener(editText -> editText.setFilters(new InputFilter[] {filter}));


			serverPref = findPreference(PersistenceHelper.SERVER_URL);
			serverPref.setText(Tools.server(serverPref.getText()));
			serverPref.setSummary(serverPref.getText());

			exp_serverPref = findPreference(PersistenceHelper.EXPORT_SERVER_URL);
			exp_serverPref.setText(exp_serverPref.getText());
			exp_serverPref.setSummary(exp_serverPref.getText());

			appPref = findPreference(PersistenceHelper.BUNDLE_NAME);
			appPref.setSummary(appPref.getText());
			appPref.setOnBindEditTextListener(editText -> editText.setFilters(new InputFilter[] {filter}));


//			EditTextPreference backupPref = findPreference(PersistenceHelper.BACKUP_LOCATION);
//			if (backupPref.getText() == null || backupPref.getText().isEmpty()) {
//				File[] externalStorageVolumes =
//						ContextCompat.getExternalFilesDirs(requireContext(), null);
//				File primaryExternalStorage = externalStorageVolumes[0];
//				backupPref.setText(primaryExternalStorage.getAbsolutePath()+"/"+appPref.getText()+"/backup");
//			}
//			backupPref.setSummary(backupPref.getText());

			ListPreference logLevels = findPreference(PersistenceHelper.LOG_LEVEL);
			logLevels.setSummary(logLevels.getEntry());

			Preference button = findPreference("reset_cache");
			final ExecutorService executorService = Executors.newSingleThreadExecutor();


			button.setOnPreferenceClickListener(preference -> {
				new AlertDialog.Builder(requireActivity())
						.setTitle(getResources().getString(R.string.resetCache))
						.setMessage(getResources().getString(R.string.reset_cache_warn))
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setCancelable(false) // User cannot dismiss warning dialog easily
						.setPositiveButton(R.string.ok, (dialog, which) -> {
							// Show the progress/loading dialog
							showProgressDialog();

							// Execute the cleanup on a background thread
							executorService.execute(() -> {
								String bundleName = requireActivity().getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE).getString(PersistenceHelper.BUNDLE_NAME, "");
								int n = 0; // To store the count of erased files

								if (!bundleName.isEmpty()) {
									Log.d("vortex", "Erasing cache for " + bundleName);
									// Perform file system cleanup
									n = Tools.eraseFolder(requireContext().getFilesDir()+"/"+bundleName.toLowerCase(Locale.ROOT) + "/cache/");

									// Erase all frozen flag
									SharedPreferences sharedPrefs = requireActivity().getApplicationContext().getSharedPreferences(bundleName, Context.MODE_PRIVATE);
									PersistenceHelper ph = new PersistenceHelper(sharedPrefs);
									ph.put(PersistenceHelper.ALL_MODULES_FROZEN + "moduleLoader", false);

									GlobalState gs = GlobalState.getInstance();
									if (gs != null) {
										if (gs.getDb() != null) {
											Log.d("vortex", "cleaning database");
											// Perform database cleanup
											gs.getDb().cleanDatabase();
										}
									}
								}

								final int finalN = n; // Need final variable for lambda
								// Update UI on the main thread after cleanup is done
								requireActivity().runOnUiThread(() -> {
									dismissProgressDialog(); // Dismiss the loading dialog

									Toast.makeText(getActivity(), finalN + " " + getResources().getString(R.string.reset_cache_toast), Toast.LENGTH_LONG).show();
									askForRestart();
								});
							});
						})
						.setNegativeButton(R.string.cancel, (dialog, which) -> {})
						.show();
				return true;
			});

			final Preference QRPref = findPreference("scan_qr_code");

			QRPref.setOnPreferenceClickListener(preference -> {
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				File[] externalStorageVolumes =
						ContextCompat.getExternalFilesDirs(requireContext(), null);
				File primaryExternalStorage = externalStorageVolumes[0];
				//create data folder.
				File picsDir = new File(primaryExternalStorage.getAbsolutePath() + "/pics/");
				if (!picsDir.exists()) {
					picsDir.mkdirs();
				}
				File file = new File(picsDir, Constants.TEMP_BARCODE_IMG_NAME);
				Uri outputFileUri = Uri.fromFile(file);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
				// Exposing file Uris is discouraged. Consider using FileProvider for better security.
				StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
				StrictMode.setVmPolicy(builder.build());
				startActivityForResult(intent, Constants.QR_SCAN_REQUEST);
				return true;
			});


		}

		private boolean isCharAllowed(char c) {
			return Character.isLetterOrDigit(c);
		}

		@Override
		public void onPause() {
			super.onPause();
			// Use getPreferenceScreen().getSharedPreferences() to be consistent with the fragment's lifecycle.
			getPreferenceScreen().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(this);
			if (anyChange)
				Tools.restart(requireActivity());

		}

		@Override
		public void onResume() {
			super.onResume();
			getPreferenceScreen().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this);
		}

		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, @NonNull String key) {
			askForRestart();
			Preference pref = findPreference(key);
			// This can be null if the preference is not on the current screen.
			if (pref == null) {
				return;
			}

			teamPref.setEnabled(devFuncPref.isChecked());

			if (pref instanceof EditTextPreference) {
				EditTextPreference etp = (EditTextPreference) pref;
				if (!isEmpty(etp.getText())) {
					if (key.equals(PersistenceHelper.BUNDLE_NAME)) {
						Log.d("vortex", "changing bundle");
						char[] strA = etp.getText().toCharArray();
						if (strA.length > 0) {
							strA[0] = Character.toUpperCase(strA[0]);
						}
						String bundleName = new String(strA);
						etp.setText(bundleName);
						String syncGroup = bundleName+"synk"+ Calendar.getInstance().get(Calendar.YEAR);
						teamPref.setText(syncGroup);
						teamPref.setSummary(syncGroup);
					} else if (key.equals(PersistenceHelper.SERVER_URL)) {
						Log.d("vortex", "changing server");
						etp.setText(Tools.server(etp.getText()));
					}
				}
				pref.setSummary(etp.getText());
			}
			else if (pref instanceof ListPreference) {
				ListPreference letp = (ListPreference) pref;
				pref.setSummary(letp.getEntry());
				if (key.equals(PersistenceHelper.LOG_LEVEL)) {
					String logLevelStr = letp.getValue();
					Log.d("vortex", "changing log level to " + logLevelStr);
					if ("CRITICAL".equalsIgnoreCase(logLevelStr)) {
						LogRepository.getInstance().setLogLevel(LogRepository.LogLevel.CRITICAL);
					} else {
						LogRepository.getInstance().setLogLevel(LogRepository.LogLevel.NORMAL);
					}
				}
			}

		}

		private void askForRestart() {
			anyChange =true;
		}

		private boolean isEmpty(String s) {
			return s == null || s.isEmpty();
		}

		private void showProgressDialog() {
			// Using AlertDialog with a custom layout for a more modern look than ProgressDialog
			AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
			LayoutInflater inflater = getLayoutInflater();
			View dialogView = inflater.inflate(R.layout.dialog_progress, null); // You'll need to create dialog_progress.xml
			TextView messageText = dialogView.findViewById(R.id.progress_message); // Assuming you have a TextView with this ID
			messageText.setText(getResources().getString(R.string.cleaning_up_please_wait)); // Create this string resource

			builder.setView(dialogView);
			builder.setCancelable(false); // Important: User cannot dismiss it
			progressDialog = builder.create();
			progressDialog.show();

		}

		// Helper method to dismiss the progress dialog
		private void dismissProgressDialog() {
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
				progressDialog = null;
			}
		}
	}
}
