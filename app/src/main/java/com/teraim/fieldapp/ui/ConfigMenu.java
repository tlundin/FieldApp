package com.teraim.fieldapp.ui;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.BarcodeReader;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ConfigMenu extends PreferenceActivity {
	private final SettingsFragment sf = new SettingsFragment();
	private static boolean anyChange = false;
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		anyChange = false;
		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, sf)
				.commit();
		setTitle(R.string.settings);


	}

	public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

		private EditTextPreference serverPref;
		private EditTextPreference exp_serverPref;
		private ListPreference versionControlPref;
		private ListPreference syncPref;
		private EditTextPreference teamPref;
		private EditTextPreference userPref;
		private EditTextPreference appPref;
		private CheckBoxPreference devFuncPref;

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode,resultCode,data);
			Log.d("vortex", "IN ONACTIVITY RESULT ");

			Log.d("vortex", "request code " + requestCode + " result code " + resultCode);
			if (requestCode == Constants.QR_SCAN_REQUEST) {
				if (Activity.RESULT_OK == resultCode) {
					Log.d("vortex", "code img taken...scan!");
					String url = (new BarcodeReader(this.getActivity())).analyze();

					Log.d("vortex", "GOT " + (url == null ? "null" : url));


					//www.teraim.com?project=Rlotst&team=Rlo2017&name=Lotta&sync=Internet&control=major
					if (url != null) {
						Uri uri = Uri.parse(url);

						final String application = uri.getQueryParameter("application");
						final String team = uri.getQueryParameter("team");
						final String name = uri.getQueryParameter("name");
						final String sync = uri.getQueryParameter("sync");
						final String control = uri.getQueryParameter("control");
						final String server = uri.getPath();

						(new AlertDialog.Builder(this.getActivity())).setTitle("Recieved QR configuration")
								.setMessage("The following QR setting was received:" +
										Tools.printIfNotNull("\nApplication: ", application) +
										Tools.printIfNotNull("\nTeam: ", team) +
										Tools.printIfNotNull("\nName: ", name) +
										Tools.printIfNotNull("\nSync: ", sync) +
										Tools.printIfNotNull("\nVersion Control: ", control) +
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
                                    if (sync != null) {
                                        syncPref.setValue(sync);
                                        syncPref.setSummary(sync);
                                    }
                                    if (control != null) {
                                        versionControlPref.setValue(control);
                                        versionControlPref.setSummary(control);
                                    }
                                    if (server != null)
                                        serverPref.setText(server);
                                })
								.show();
						askForRestart();

					} else {
						new AlertDialog.Builder(this.getActivity()).setTitle("Error")
								.setMessage("NO QR code found in image.")
								.setPositiveButton(R.string.ok, (dialog, which) -> {


                                })

								.setCancelable(false)
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();
					}
				}
			}
		}



		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			this.getPreferenceManager().setSharedPreferencesName(Constants.GLOBAL_PREFS);
			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.configmenu);

			//Set default values for the prefs.
			//			getPreferenceScreen().getSharedPreferences()
			//			.registerOnSharedPreferenceChangeListener(this);
			this.getActivity().getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE)
					.registerOnSharedPreferenceChangeListener(this);

			//Create a filter that stops users from entering disallowed characters.
			InputFilter filter = new InputFilter() {
				@Override
				public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
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
				}

				private boolean isCharAllowed(char c) {
					return Character.isLetterOrDigit(c);
				}
			};


			devFuncPref = (CheckBoxPreference)findPreference("dev_switch");
			teamPref = (EditTextPreference) findPreference(PersistenceHelper.LAG_ID_KEY);
			String syncGroupText = teamPref.getText();

			if (!isEmpty(syncGroupText))
				teamPref.setSummary(syncGroupText);
			else {
				teamPref.setText(null);
				teamPref.setSummary(getText(R.string.Team_s));
			}
			teamPref.setEnabled(devFuncPref.isChecked());
			teamPref.getEditText().setFilters(new InputFilter[] {filter});

			userPref = (EditTextPreference) findPreference(PersistenceHelper.USER_ID_KEY);
			String syncUserText = userPref.getText();
			if (!isEmpty(syncUserText))
				userPref.setSummary(syncUserText);
			else {
				userPref.setText(null);
				userPref.setSummary(getText(R.string.UserName_dm));
			}
			userPref.getEditText().setFilters(new InputFilter[] {filter});


//			ListPreference color = (ListPreference)findPreference(PersistenceHelper.DEVICE_COLOR_KEY_NEW);
//			color.setSummary(color.getValue());

			versionControlPref = (ListPreference)findPreference(PersistenceHelper.VERSION_CONTROL);
			versionControlPref.setSummary(versionControlPref.getValue());
			versionControlPref.setEnabled(devFuncPref.isChecked());

			syncPref = (ListPreference)findPreference(PersistenceHelper.SYNC_METHOD);
			syncPref.setSummary(syncPref.getValue());

			serverPref = (EditTextPreference) findPreference(PersistenceHelper.SERVER_URL);
			serverPref.setText(Tools.server(serverPref.getText()));
			serverPref.setSummary(serverPref.getText());

			exp_serverPref = (EditTextPreference) findPreference(PersistenceHelper.EXPORT_SERVER_URL);
			exp_serverPref.setText(exp_serverPref.getText());
			exp_serverPref.setSummary(exp_serverPref.getText());

			appPref = (EditTextPreference) findPreference(PersistenceHelper.BUNDLE_NAME);
			appPref.setSummary(appPref.getText());
			appPref.getEditText().setFilters(new InputFilter[] {filter});

			EditTextPreference backupPref = (EditTextPreference) findPreference(PersistenceHelper.BACKUP_LOCATION);
			backupPref.setSummary(backupPref.getText());
			//backupPref.setSummary(backupPref.getSummary());

			ListPreference logLevels = (ListPreference)findPreference(PersistenceHelper.LOG_LEVEL);
			logLevels.setSummary(logLevels.getEntry());

			Preference reload_button = findPreference("reload_config");
			reload_button.setOnPreferenceClickListener(preference -> {
				new AlertDialog.Builder(getActivity())
						.setTitle(getResources().getString(R.string.reload_config))
						.setMessage(getResources().getString(R.string.reload_config_warn))
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setCancelable(false)
						.setPositiveButton(R.string.ok, (dialog, which) -> {
							String bundleName = getActivity().getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE).getString(PersistenceHelper.BUNDLE_NAME, "");
							if (!bundleName.isEmpty()) {
								PersistenceHelper ph = new PersistenceHelper(getActivity().getApplicationContext().getSharedPreferences(bundleName, Context.MODE_PRIVATE));
								ph.put(PersistenceHelper.ALL_MODULES_FROZEN + "moduleLoader",false);
								ph.put(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE,-1f);
								Toast.makeText(getActivity(), getResources().getString(R.string.reload_config_toast), Toast.LENGTH_LONG).show();
								askForRestart();
							}
						})
						.setNegativeButton(R.string.cancel, (dialog, which) -> {

						})
						.show();
				return true;
			});
			Preference button = findPreference("reset_cache");
			button.setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.resetCache))
                        .setMessage(getResources().getString(R.string.reset_cache_warn))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            String bundleName = getActivity().getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE).getString(PersistenceHelper.BUNDLE_NAME, "");
                            if (!bundleName.isEmpty()) {
                                Log.d("vortex", "Erasing cache for " + bundleName);
                                int n = Tools.eraseFolder(getContext().getFilesDir()+"/"+bundleName.toLowerCase(Locale.ROOT) + "/cache/");
                                //erase allfrozen flag
								PersistenceHelper ph = new PersistenceHelper(getActivity().getApplicationContext().getSharedPreferences(bundleName, Context.MODE_PRIVATE));
								ph.put(PersistenceHelper.ALL_MODULES_FROZEN + "moduleLoader",false);
                                Toast.makeText(getActivity(), n + " " + getResources().getString(R.string.reset_cache_toast), Toast.LENGTH_LONG).show();
                                askForRestart();
                            }
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {

                        })
                        .show();
                return true;
            });

			final CheckBoxPreference pref = (CheckBoxPreference)findPreference("local_config");
			final PreferenceGroup devOpt = (PreferenceGroup)findPreference("developer_options");
			final PreferenceGroup genOpt = (PreferenceGroup)findPreference("general_options");
			final Preference folderPref = findPreference(PersistenceHelper.FOLDER);
			final Preference QRPref = findPreference("scan_qr_code");

			QRPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				File[] externalStorageVolumes =
						ContextCompat.getExternalFilesDirs(getContext(), null);
				File primaryExternalStorage = externalStorageVolumes[0];
				//create data folder. This will also create the ROOT folder for the Strand app.
                File file = new File(primaryExternalStorage.getAbsolutePath() + "/pics/",Constants.TEMP_BARCODE_IMG_NAME);
                Uri outputFileUri = Uri.fromFile(file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
                startActivityForResult(intent, Constants.QR_SCAN_REQUEST);
                return true;
            });
			//check if local folder exists. If not, create it.
			pref.setOnPreferenceChangeListener((preference, o) -> {

                if (pref.isChecked()) {
                    devOpt.removePreference(folderPref);
                    genOpt.addPreference(serverPref);
                } else {

                    genOpt.removePreference(serverPref);
                    setFolderPref(folderPref);
                    devOpt.addPreference(folderPref);

                }
                return true;
            });

			if (!pref.isChecked()) {
				devOpt.removePreference(folderPref);
				genOpt.addPreference(serverPref);
			} else {
				genOpt.removePreference(serverPref);
				setFolderPref(folderPref);
				devOpt.addPreference(folderPref);
			}

			folderPref.setOnPreferenceClickListener(preference -> {
                setFolderPref(folderPref);
                return true;
            });


		}

		private void setFolderPref(Preference folderPref) {
			//Null if serverbased preference displayed.
			if (folderPref==null)
				return;
			String bundleName = getActivity().getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE).getString(PersistenceHelper.BUNDLE_NAME,"");
			if (bundleName.isEmpty())
				folderPref.setSummary("Application name missing");
			else {
				String path = getContext().getFilesDir() + "/"+ bundleName.toLowerCase(Locale.ROOT) + "/config";
				File folder = new File(path);
				StringBuilder textToDisplay = new StringBuilder("Location: " + path);
				StringBuilder filesList = new StringBuilder("No configuration files in folder. Please add");
				boolean folderExists= true;
				if (!folder.exists())
                    folderExists=folder.mkdir();
				if (folderExists) {
                    File[] files = folder.listFiles();
                    if (files != null && files.length > 0) {
                        filesList = new StringBuilder();
                        for (File f : files) {
                            filesList.append(f.getName()).append(",");
                        }
                        filesList = new StringBuilder(filesList.substring(0, filesList.length() - 1));
                    }
                    textToDisplay.append("\n").append(filesList);
                    folderPref.setSummary(textToDisplay.toString());
                    folderPref.getEditor().putString(path, "").commit();
                }
			}
		}


		/* (non-Javadoc)
		 * @see android.app.Fragment#onPause()
		 */
		@Override
		public void onPause() {
			super.onPause();
			this.getActivity().getApplicationContext().getSharedPreferences("GlobalPrefs", Context.MODE_PRIVATE)
					.unregisterOnSharedPreferenceChangeListener(this);
			if (anyChange)
				Tools.restart(this.getActivity());

		}




		/* (non-Javadoc)
		 * @see android.app.Fragment#onResume()
		 */
		@Override
		public void onResume() {
			//this.getPreferenceManager().setSharedPreferencesName(phone);
			this.getActivity().getApplicationContext().getSharedPreferences("GlobalPrefs", Context.MODE_PRIVATE)
					.registerOnSharedPreferenceChangeListener(this);
			//getPreferenceScreen().getSharedPreferences()
			//.registerOnSharedPreferenceChangeListener(this);
			super.onResume();
		}




		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			askForRestart();
			Preference pref = findPreference(key);
			Account mAccount = GlobalState.getmAccount(getActivity());
			teamPref.setEnabled(devFuncPref.isChecked());
			versionControlPref.setEnabled(devFuncPref.isChecked());
			if (pref instanceof EditTextPreference) {
				EditTextPreference etp = (EditTextPreference) pref;
				if (!isEmpty(etp.getText())) {
					char[] strA = etp.getText().toCharArray();
					if (key.equals(PersistenceHelper.BUNDLE_NAME)) {
						Log.d("vortex", "changing bundle");
						setFolderPref(findPreference(PersistenceHelper.FOLDER));
						strA[0] = Character.toUpperCase(strA[0]);
						String bundleName = new String(strA);
						etp.setText(bundleName);
						String syncGroup = bundleName+"synk"+ Calendar.getInstance().get(Calendar.YEAR);
						teamPref.setText(syncGroup);
						teamPref.setSummary(syncGroup);
					}
				}
				pref.setSummary(etp.getText());
			}
			else if (pref instanceof ListPreference) {
                ListPreference letp = (ListPreference) pref;
                pref.setSummary(letp.getEntry());
            }
		}



		private void askForRestart() {
			anyChange =true;
		}

		private boolean isEmpty(String s) {
			return s == null || s.isEmpty();
		}

	}








}




