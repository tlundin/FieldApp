package com.teraim.fieldapp.dynamic.blocks;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Button;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_StatusButton;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ToggleButton;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.ExportDialog;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.BarcodeReader;
import com.teraim.fieldapp.utils.Connectivity;
import com.teraim.fieldapp.utils.Exporter;
import com.teraim.fieldapp.utils.Exporter.ExportReport;
import com.teraim.fieldapp.utils.Exporter.Report;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * buttonblock
 * Class for all created Buttons
 * @author Terje
 */
public  class ButtonBlock extends Block  implements EventListener {
	/**
	 *
	 */
	private static final long serialVersionUID = 6454431627090793561L;
	private String exportMethod="file";
	private String exportFormat = "csv";
	private String exportFileName = null;

	private final String onClick;
	private final String name;
	private final String containerId;
	private Boolean validationResult = true;
	private final Type type;
	private android.graphics.drawable.Drawable originalBackground;
	private final List<EvalExpr>textE;
	private final List<EvalExpr> targetE;
	private final List<EvalExpr> buttonContextE;
	private final List<EvalExpr> imgFilterE;
	private List<EvalExpr> statusContextE;

	private WF_Context myContext;
	private final boolean isVisible;
	private String statusVar=null;
	private OnclickExtra extraActionOnClick=null;
	private GlobalState gs;
	private PopupWindow mpopup=null;

	private String targetMailAdress=null;


	private final boolean enabled;

	private DB_Context buttonContextOld=null,buttonContext=null;
	private final boolean syncRequired;
	private VariableCache varCache;
	private WF_Button button = null;
	private Map<String,String> statusVariableHash=null;

	enum Type {
		action,
		toggle
	}

	private class ExportEntry {
		String name;
		String path;
		public ExportEntry(String name, String path) {
			this.name=name;
			this.path=path;
		}
	}

	public ButtonBlock(String id,String lbl,String action, String name,String container,String target, String type, String statusVariableS,boolean isVisible,String exportFormat,String exportMethod, boolean enabled, String buttonContextS, String statusContextS,boolean requestSync, String imgFilter) {
		Log.d("NILS","In NEW for Button "+name+" with context: "+buttonContextS);
		this.blockId=id;
		this.textE = Expressor.preCompileExpression(lbl);
		this.onClick=action;
		this.name=name;
		this.containerId = container;
		this.targetE=Expressor.preCompileExpression(target);
		this.type=type.equals("toggle")?Type.toggle:Type.action;
		this.isVisible = isVisible;
		this.statusVar = statusVariableS;
		if (statusVar!=null&&statusVar.length()==0)
			statusVar=null;
		this.enabled=enabled;
		this.buttonContextE=Expressor.preCompileExpression(buttonContextS);
		this.statusContextE=Expressor.preCompileExpression(statusContextS);
		this.imgFilterE=Expressor.preCompileExpression(imgFilter);
		if (statusVar!=null && statusContextE==null)
			statusContextE=buttonContextE;
		Log.d("blorg","button "+textE+" statusVar: "+statusVar+" status_context: "+statusContextS);
		this.syncRequired = requestSync;
		//Log.d("vortex","syncRequired is "+syncRequired);
		//if export, what kind of delivery method

		if (exportMethod!=null) {
			this.exportMethod = exportMethod;
			if (exportMethod.startsWith("mailto:")) {
				targetMailAdress="";
				if (exportMethod.length() > 7) {

					if (exportMethod.contains("@")) {
						targetMailAdress = exportMethod.substring(7, exportMethod.length());
						Log.d("vortex", "Target mail address is : " + targetMailAdress);
					} else {
						//config error
						targetMailAdress = null;
					}
				}

			}
		}


		if (exportFormat != null) {
			this.exportFormat = exportFormat;

		}


	}


	private String getText() {
		return Expressor.analyze(textE);
	}



	public void onEvent(Event e) {
		Log.d("bulla","in event for button "+this.getText());

		if (button!=null && !myContext.myEndIsNear()) {
			button.setText(getText());
			if (button instanceof WF_StatusButton) {
				Log.d("bulla","calling refresh for "+this.getText());
				((WF_StatusButton)button).refreshStatus();
			}
			Log.d("bulla","aftercall");
			if (buttonContextE!=null&&!buttonContextE.isEmpty())
				buttonContext = DB_Context.evaluate(buttonContextE);

		} else
			Log.d("vortex","disregarded event on button");
	}

	public String getName() {
		if (name!=null)
			return name;
		else
			return getText();
	}

	public String getTarget() {
		return Expressor.analyze(targetE);
	}


	public void create(final WF_Context myContext) {
		button = null;
		this.myContext=myContext;
		final Context ctx = myContext.getContext();
		myContext.registerEventListener(this, Event.EventType.onSave);
		gs = GlobalState.getInstance();
		varCache = gs.getVariableCache();
		o=gs.getLogger();
		final LayoutInflater inflater = (LayoutInflater)ctx.getSystemService
				(Context.LAYOUT_INFLATER_SERVICE);
		Log.d("nils","In CREATE for BUTTON "+getText());
		Container myContainer = myContext.getContainer(containerId);
		if (myContainer!=null) {
			//Is the context provided already?
			if (buttonContextOld!=null)
				buttonContext=buttonContextOld;
			else {
				Log.d("vortex","ButtonContextS: "+buttonContextE);
				Log.d("vortex","statusContextS: "+statusContextE);
				//If not, parse the buttoncontext if provided in the button.
				//Else, use context in flow
				if (buttonContextE!=null&&!buttonContextE.isEmpty())
					buttonContext = DB_Context.evaluate(buttonContextE);
				else {
					Log.e("vortex","No button context. Will use default");
					buttonContext = myContext.getHash();
				}
			}

			Log.d("nils","Buttoncontext set to: "+buttonContext+" for button: "+getText());

			if (type == Type.action) {
				button=null;
				if (statusVar!=null) {
					button = new WF_StatusButton(blockId, WF_StatusButton.createInstance(0, getText(), ctx), isVisible, myContext, statusVar,statusContextE);
					if(((WF_StatusButton)button).refreshStatus()) {
						Log.d("vortex","sucessfully created statusbutton "+(button instanceof WF_StatusButton));
					} else {
						//button=null;
						o.addRow("");
						if (buttonContext==null) {
							o.addRedText("Statusvariable [" + statusVar + "], has something wrong with its context. Check precompile log.");
							button = null;
						}
						Log.d("abba","buttonContext: "+buttonContext.getContext().toString());
						//o.addRedText("Statusvariable [" + statusVar + "], buttonblock " + blockId + " does not exist. Will use normal button");
						Log.e("vortex", "Statusvariable [" + statusVar + "], buttonblock " + blockId + " does not exist. Will use normal button");

					}
				}
				//If status wrong, no lamp
				if (button==null)
					button = new WF_Button(blockId,WF_Button.createInstance(getText(),ctx),isVisible,myContext);

				button.setOnClickListener(new View.OnClickListener() {
					boolean clickOngoing = false;

					@Override
					public void onClick(View view) {
						if (clickOngoing)
							return;
						else
							clickOngoing = true;
						originalBackground = view.getBackground();
						view.setBackgroundColor(Color.parseColor(Constants.Color_Pressed));
						//ACtion = workflow to execute.
						//Commence!
						if (extraActionOnClick != null) {
							extraActionOnClick.onClick();
						}

						if (onClick.startsWith("template"))
							myContext.getTemplate().execute(onClick, getTarget());
						else {


							if (onClick.equals("Go_Back") || onClick.equals("go_back_export")) {

								final Variable statusVariable;
								String statusVar = myContext.getStatusVariable();

								if (statusVar != null) {
									Log.d("vorto", "found statusvar named " + statusVar);
									statusVariable = varCache.getVariable(buttonContext.getContext(), statusVar);
								} else
									statusVariable = null;

								Set<Rule> myRules = myContext.getRulesThatApply();
								boolean showPop = false;

								View popUpView = null; // inflating popup layout

								if (myRules != null && myRules.size() > 0) {
									Log.d("nils", "I have " + myRules.size() + " rules!");
									validationResult = null;
									//We have rules. Each rule adds a line in the popup.
									popUpView = inflater.inflate(R.layout.rules_popup, null);
									mpopup = new PopupWindow(popUpView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, true); //Creation of popup
									mpopup.setAnimationStyle(android.R.style.Animation_Dialog);
									LinearLayout frame = popUpView.findViewById(R.id.pop);
									Button avsluta = popUpView.findViewById(R.id.avsluta);
									Button korrigera = popUpView.findViewById(R.id.korrigera);
									if (onClick.equals("go_back_export")) {
										showPop = true;
										avsluta.setText(ctx.getString(R.string.export_and_finalize));
									}
									avsluta.setOnClickListener(new OnClickListener() {
										@Override
										public void onClick(View v) {
											if (onClick.equals("go_back_export"))
												displayExportDialog();
											else {
												if (statusVariable != null) {
													statusVariable.setValue(validationResult ? Constants.STATUS_AVSLUTAD_EXPORT_MISSLYCKAD :
															Constants.STATUS_STARTAD_MED_FEL);
													Log.e("vortex", "SETTING STATUSVAR: " + statusVariable.getId() + " key: " + statusVariable.getKeyChain() + "Value: " + statusVariable.getValue());
													//Save value of all variables to database in current flow.
												} else
													Log.d("nils", "Found no status variable");
											}
											Set<Variable> variablesToSave = myContext.getTemplate().getVariables();
											Log.d("vortex", "Variables To save contains " + (variablesToSave == null ? "null" : variablesToSave.size() + " objects."));
											if (variablesToSave != null) {
												for (Variable var : variablesToSave) {
													Log.d("vortex", "Saving " + var.getLabel());
													boolean resultOfSave = var.setValue(var.getValue());
												}
											}
											myContext.registerEvent(new WF_Event_OnSave(ButtonBlock.this.getBlockId()));
											mpopup.dismiss();
											goBack();
										}
									});

									korrigera.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View v) {
											mpopup.dismiss();
										}
									});
									LinearLayout row;
									TextView header, body;
									ImageView indicator;
									//Assume correct.
									validationResult = true;
									boolean isDeveloper = gs.getGlobalPreferences().getB(PersistenceHelper.DEVELOPER_SWITCH);

									for (Rule r : myRules) {
										Boolean ok = false;

										ok = r.execute();

										if (ok != null) {
											Rule.Type type = r.getType();
											int indicatorId = 0;
											boolean bok = false;
											if (ok) {
												indicatorId = R.drawable.btn_icon_ready;
												bok = true;
											} else if (type == Rule.Type.ERROR) {
												indicatorId = R.drawable.btn_icon_started_with_errors;
											} else {
												indicatorId = R.drawable.btn_icon_started;
												bok = true;
											}
											if (!bok) {
												validationResult = false;
											}
											if (!ok || isDeveloper) {
												showPop = true;
												row = (LinearLayout) inflater.inflate(R.layout.rule_row, null);
												header = row.findViewById(R.id.header);
												body = row.findViewById(R.id.body);
												indicator = row.findViewById(R.id.indicator);
												indicator.setImageResource(indicatorId);
												Log.d("nils", " Rule header " + r.getRuleHeader() + " rule body: " + r.getRuleText());
												header.setText(r.getRuleHeader());
												body.setText(r.getRuleText());
												frame.addView(row);
											}
										}
									}
									if (validationResult == false) {
										if (button instanceof WF_StatusButton) {
											((WF_StatusButton) button).changeStatus(WF_StatusButton.Status.started_with_errors);
										}
										avsluta.setEnabled(false);
									}

								}

								if (showPop)
									mpopup.showAtLocation(popUpView, Gravity.TOP, 0, 0);    // Displaying popup
								else {
									//no rules? Then validation is always ok.
									Log.d("nils", "No rules found - exiting");
									if (statusVariable != null) {
										statusVariable.setValue(WF_StatusButton.Status.ready.ordinal() + "");
										//Log.e("grogg","PSETTING STATUSVAR: "+statusVariable.getId()+" key: "+statusVariable.getKeyChain()+ "Value: "+statusVariable.getValue());
										myContext.registerEvent(new WF_Event_OnSave(ButtonBlock.this.getBlockId()));

									} else
										Log.d("nils", "Found no status variable");
									Set<Variable> variablesToSave = myContext.getTemplate().getVariables();
									Log.d("nils", "Variables To save contains " + variablesToSave.size() + " objects.");
									for (Variable var : variablesToSave) {
										Log.d("nils", "Saving " + var.getLabel());
										var.setValue(var.getValue());
									}
									goBack();
								}
							} else if (onClick.equals("Start_Local_Workflow")) {

							} else if (onClick.equals("Start_Workflow")) {
								String target = getTarget();
								Workflow wf = gs.getWorkflow(target);
								if (buttonContext != null) {
									Log.d("vortex", "Will use buttoncontext: " + buttonContext);
									gs.setDBContext(buttonContext);
								}
								if (wf == null) {
									Log.e("NILS", "Cannot find workflow [" + target + "] referenced by button " + getName());
									o.addRow("");
									o.addRow("Cannot find workflow [" + target + "] referenced by button " + getName());
								} else {
									o.addRow("");
									o.addRow("Action button pressed. Executing wf: " + target + " with statusvar " + statusVar);
									Log.d("Vortex", "Action button pressed. Executing wf: " + target + " with statusvar " + statusVar);
									//If the template called is empty, mark this flow as "caller" to make it possible to refresh its ui after call ends.
									String calledTemplate = wf.getTemplate();
									Log.d("vortex", "template: " + calledTemplate);
									if (calledTemplate == null) {
										Log.d("vortex", "call to empty template flow. setcaller.");
										myContext.setCaller();
									}
									Start.singleton.changePage(wf, statusVar);

								}

							} else if (onClick.equals("export")) {

								if (buttonContext == null) {
									Log.e("export", "Export failed...no context");
								} else {

									boolean done = false;

									if (button instanceof WF_StatusButton) {
										WF_StatusButton statusButton = ((WF_StatusButton) button);
										WF_StatusButton.Status status = statusButton.getStatus();
										if (status == WF_StatusButton.Status.ready) {
											final WF_StatusButton tmpSB = statusButton;
											new AlertDialog.Builder(ctx)
													.setTitle("Reset")
													.setMessage("Are you sure you want to reset this button? Status will change back to neutral.")
													.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
														public void onClick(DialogInterface dialog, int which) {
															tmpSB.changeStatus(WF_StatusButton.Status.none);
														}
													})
													.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
														@Override
														public void onClick(DialogInterface dialog, int which) {

														}
													})
													.setIcon(android.R.drawable.ic_dialog_alert)
													.show();

											done = true;
										}
									}
									if (!done) {
										displayExportDialog();

									}
								}
								} else if (onClick.equals("Start_Camera")) {
									if (getTarget() != null) {
										Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
										File photoFile = null;
										if (intent.resolveActivity(ctx.getPackageManager()) != null) {
											// Create the File where the photo should go

											File[] externalStorageVolumes =
													ContextCompat.getExternalFilesDirs(ctx, null);
											File primaryExternalStorage = externalStorageVolumes[0];
											//create data folder. This will also create the ROOT folder for the Strand app.
											photoFile = new File(primaryExternalStorage.getAbsolutePath() + "/pics/", getTarget());
											// Continue only if the File was successfully created
											if (photoFile != null) {
												Uri photoURI = FileProvider.getUriForFile(ctx,
														"com.teraim.fieldapp.fileprovider",
														photoFile);
												intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
												Log.d("photo", "storing image to " + photoFile);
												((Activity) ctx).startActivityForResult(intent, Constants.TAKE_PICTURE);
											}
										}
										if (photoFile == null) {
											Log.e("photo", "failed to take picture");
											o.addRow("");
											o.addRedText("Failed to take picture. Permission or memory problem. BlockId: " + ButtonBlock.this.getBlockId());
										}
									} else {
										o.addRow("");
										o.addRedText("No target (filename) specified for camera action button. BlockId: " + ButtonBlock.this.getBlockId());
									}
								} else if (onClick.equals("barcode")) {
									Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
									File photoFile;
									if (intent.resolveActivity(ctx.getPackageManager()) != null) {
										// Create the File where the photo should go
										File[] externalStorageVolumes =
												ContextCompat.getExternalFilesDirs(ctx, null);
										File primaryExternalStorage = externalStorageVolumes[0];
										//create data folder. This will also create the ROOT folder for the Strand app.
										photoFile = new File(primaryExternalStorage.getAbsolutePath() + "/pics/", Constants.TEMP_BARCODE_IMG_NAME);
										// Continue only if the File was successfully created
										if (photoFile != null) {
											Uri photoURI = FileProvider.getUriForFile(ctx,
													"com.teraim.fieldapp.fileprovider",
													photoFile);
											intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
											((Activity) ctx).startActivityForResult(intent, Constants.TAKE_PICTURE);
										}
									}
									//wait for image to be captured.
									myContext.registerEventListener(new BarcodeReader(myContext, getTarget()), Event.EventType.onActivityResult);
								} else if (onClick.equals("backup")) {
									boolean success = GlobalState.getInstance().getBackupManager().backupDatabase();
									new AlertDialog.Builder(ctx)
											.setTitle("Backup " + (success ? "succesful" : "failed"))
											.setMessage(success ? "A file named 'backup_" + Constants.getSweDate() + "' has been created in your backup folder." : "Failed. Please check if the backup folder you specified under the config menu exists.")
											.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int which) {
												}
											})
											.setIcon(android.R.drawable.ic_dialog_alert)
											.show();
								} else if (onClick.equals("restore_from_backup")) {

									new AlertDialog.Builder(ctx)
											.setTitle("Warning!")
											.setMessage("If you go ahead, you current database will be replaced by a backup file.")
											.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int which) {
													boolean success = GlobalState.getInstance().getBackupManager().restoreDatabase();
													new AlertDialog.Builder(ctx)
															.setTitle("Restore " + (success ? "succesful" : "failed"))
															.setMessage(success ? "Your database has been restored from backup. Please restart the app now." : "Failed. Please check that the backup file is in the staging area")
															.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
																public void onClick(DialogInterface dialog, int which) {
																}
															})
															.setIcon(android.R.drawable.ic_dialog_alert)
															.show();
												}
											})
											.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int which) {
												}
											})
											.setIcon(android.R.drawable.ic_dialog_alert)
											.show();


								} else if (onClick.equals("synctest")) {
									Log.e("vortex", "gets HEREE!!!!");


									// Pass the settings flags by inserting them in a bundle
									Bundle settingsBundle = new Bundle();
									settingsBundle.putBoolean(
											ContentResolver.SYNC_EXTRAS_MANUAL, true);
									settingsBundle.putBoolean(
											ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
									/*
									 * Request the sync for the default account, authority, and
									 * manual sync settings
									 */
									Account mAccount = GlobalState.getmAccount(ctx);
									final String AUTHORITY = "com.teraim.fieldapp.provider";
									ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);

									//Also try to say hello.


								} else {
									o.addRow("");
									o.addRedText("Action button had no associated action!");
								}
							}

							clickOngoing = false;
							view.setBackground(originalBackground);
						}
						private void displayExportDialog() {
							exportFileName = getTarget();
							final Exporter exporter = Exporter.getInstance(ctx, exportFormat.toLowerCase(), new ExportDialog());
							//Run export in new thread. Create UI to update user on progress.
							if (exporter != null) {
								((DialogFragment) exporter.getDialog()).show(((Activity) ctx).getFragmentManager(), "exportdialog");

								Thread t = new Thread() {
									String msg = "";

									@Override
									public void run() {
										Report jRep = gs.getDb().export(buttonContext.getContext(), exporter, exportFileName);
										ExportReport exportResult = jRep.getReport();
										if (exportResult == ExportReport.OK) {
											msg = jRep.noOfVars + " variables exported to file: " + exportFileName + "." + exporter.getType() + "\n";
											Log.d("vortex", "Exportmetod: " + exportMethod);
											if (exportMethod == null || exportMethod.equalsIgnoreCase("file")) {
												//nothing more to do...file is already on disk.
											} else if (exportMethod.startsWith("mail")) {
												if (targetMailAdress == null) {
													((Activity) ctx).runOnUiThread(new Runnable() {
														@Override
														public void run() {
															exporter.getDialog().setCheckSend(Exporter.FAILED);
															exporter.getDialog().setSendStatus("Configuration error");
															msg += "\nForwarding to " + exportMethod + " failed." + "\nPlease check your configuration.";
														}

													});

												} else {
													Tools.sendMail((Activity) ctx, exportFileName + "." + exporter.getType(), targetMailAdress);
													((Activity) ctx).runOnUiThread(new Runnable() {
														@Override
														public void run() {
															exporter.getDialog().setCheckSend(Exporter.SUCCESS);
															exporter.getDialog().setSendStatus("OK");
															if (!targetMailAdress.isEmpty())
																msg += "\nFile forwarded to " + targetMailAdress + ".";
															else
																msg += "\nFile forwarded by mail.";
														}

													});
												}


											} else if (exportMethod.startsWith("upload")) {

												if (!Connectivity.isConnected((Activity) ctx)) {
													o.addRow("");
													o.addRedText("Export failed - no network");
													msg = "Check your connection and try again";
													((Activity) ctx).runOnUiThread(new Runnable() {
														@Override
														public void run() {
															exporter.getDialog().setCheckSend(Exporter.FAILED);
															exporter.getDialog().setSendStatus("No network");
														}
													});
												} else {
													String exportServerURL = gs.getGlobalPreferences().get(PersistenceHelper.EXPORT_SERVER_URL);
													if (exportServerURL == PersistenceHelper.UNDEFINED) {
														o.addRow("");
														o.addRedText("Export Server URL not defined - Please configure in the Settings Menu");
														msg = "Export Server URL not defined - Please configure in the Settings Menu";
													} else {
														String exportFileEndpoint = exportServerURL + "/upload";
														File[] externalStorageVolumes =
																ContextCompat.getExternalFilesDirs(GlobalState.getInstance().getContext(), null);
														File primaryExternalStorage = externalStorageVolumes[0];
														String exportFolder = primaryExternalStorage.getAbsolutePath() + "/export/";
														String imageFolder = primaryExternalStorage.getAbsolutePath() + "/pics/";
														String nameWithType = exportFileName + "." + exporter.getType();
														File exportFile = new File(exportFolder + nameWithType);
														RequestBody exportDataFileBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
																.addFormDataPart(nameWithType, nameWithType, RequestBody.create(MediaType.parse("application/json; charset=utf-8"), exportFile)).build();

														File directory = new File(imageFolder);
														File[] imgs = directory.listFiles();
														Log.d("Files", "Size: " + imgs.length);
														String filter = null;
														if (imgFilterE != null)
															filter = Expressor.analyze(imgFilterE, buttonContext.getContext());
														Set<String> imgNames = new HashSet<>();
														List<ExportEntry> imagesToExport = new ArrayList<>();
														SharedPreferences sp = gs.getPreferences().getPreferences();
														Set<String> alreadyExported = sp.getStringSet(PersistenceHelper.EXPORTED_IMAGES_KEY, Collections.emptySet());
														Set<String> newSetAfterExport = new HashSet<String>();
														newSetAfterExport.addAll(alreadyExported);
														Log.d("Mando", "Images I know: " + alreadyExported.toString());
														for (int i = 0; i < imgs.length; i++) {
															Log.d("Files", "Image name: " + imgs[i].getName());
															String imageName = imgs[i].getName();
															if (isInFilter(imageName, filter) && !alreadyExported.contains(imageName)) {
																imgNames.add(imageName);
																String imgPath = imgs[i].getPath();
																Log.d("vortex", "imgpath is " + imgPath);
																imagesToExport.add(new ExportEntry(imageName, imgPath));
															} else
																Log.d("Mando", "Excluded " + imageName);
														}
														int totalToExport = imgNames.size() + 1;
														((Activity) ctx).runOnUiThread(new Runnable() {
															@Override
															public void run() {

																exporter.getDialog().setCheckSend(Exporter.IN_PROGRESS);
																exporter.getDialog().setSendStatus("[0/" + totalToExport + "]. Waiting for response...");
															}
														});
														final OkHttpClient client = gs.getHTTPClient();
														final AtomicInteger counter = new AtomicInteger(0);

														final Callback cb = new Callback() {
															@Override
															public void onFailure(Call call, IOException e) {
																// Cancel the post on failure.
																Log.d("FAIL", e.getMessage());
																final String err = e.getMessage();
																((Activity) ctx).runOnUiThread(new Runnable() {
																	@Override
																	public void run() {
																		exporter.getDialog().setCheckSend(Exporter.FAILED);
																		exporter.getDialog().setSendStatus("FAILED");
																		if ("timeout".equals(err) && counter.get() > 0) {
																			exporter.getDialog().setOutCome("Network Timeout. [" + (counter.get() - 1) + "] images exported. Please retry to send the remaining images");
																		} else
																			exporter.getDialog().setOutCome("Export failed.\n Error: " + err);
																	}
																});
																if (call != null)
																	call.cancel();
															}

															@Override
															public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
																counter.incrementAndGet();
																final String resp = response.body().string();
																final int code = response.code();
																if (code != HttpsURLConnection.HTTP_OK) {
																	((Activity) ctx).runOnUiThread(new Runnable() {
																		@Override
																		public void run() {
																			exporter.getDialog().setCheckSend(Exporter.FAILED);
																			exporter.getDialog().setSendStatus("FAILED");
																			exporter.getDialog().setOutCome("Export failed.\nResponse: " + resp + "\nReturn code: " + code);
																		}
																	});
																	call.cancel();
																} else {
																	String exportedImgName = "";
																	if (counter.get() >= 2) {
																		exportedImgName = imagesToExport.get(counter.get() - 2).name;
																		newSetAfterExport.add(exportedImgName);
																		sp.edit().putStringSet(PersistenceHelper.EXPORTED_IMAGES_KEY, newSetAfterExport).commit();
																	}
																	if (counter.get() == totalToExport) {
																		StringBuilder eMsg = new StringBuilder();
																		if (imagesToExport.size() == 0)
																			eMsg.append("No new images to export.");
																		else
																			eMsg.append("All files exported.");

																		((Activity) ctx).runOnUiThread(() -> {
																			exporter.getDialog().setSendStatus("[" + (counter.get()) + "/" + totalToExport + "]");
																			exporter.getDialog().setCheckSend(Exporter.SUCCESS);
																			exporter.getDialog().setOutCome(eMsg.toString());
																			if (button instanceof WF_StatusButton) {
																				Log.d("fenris","status set to ready_exported");
																				((WF_StatusButton) button).changeStatus(WF_StatusButton.Status.ready_exported);
																			}
																		});
																	} else {
																		String finalExportedImgName = exportedImgName;
																		((Activity) ctx).runOnUiThread(() -> {
																			exporter.getDialog().setSendStatus("[" + (counter.get()) + "/" + totalToExport + "]");
																			exporter.getDialog().setOutCome(finalExportedImgName);
																			exporter.getDialog().setCheckSend(Exporter.IN_PROGRESS);
																		});

																		RequestBody requestBody = createBody(imagesToExport.get(counter.get() - 1));
																		if (requestBody != null) {
																			Request request = new Request.Builder()
																					.url(exportFileEndpoint)
																					.post(requestBody)
																					.build();
																			client.newCall(request).enqueue(this);
																		} else {
																			o.addRow("");
																			o.addRedText("Failed to compress bitmap. Export failed");
																		}
																	}
																}
															}
														};
														Request request = new Request.Builder()
																.url(exportFileEndpoint)
																.post(exportDataFileBody)
																.build();
														client.newCall(request).enqueue(cb);
													}
												}
											}
										} else {
											if (exportResult == ExportReport.NO_DATA)
												msg = "Nothing to export! Have you entered any values? Have you marked your export variables as 'global'? (Local variables are not exported)";
											else
												msg = "Export failed. Reason: " + exportResult;
										}

										((Activity) ctx).runOnUiThread(new Runnable() {
											@Override
											public void run() {
												{
													exporter.getDialog().setOutCome(msg);
												}
											}
										});
									}
								};
								t.start();
							} else
								Log.e("vortex", "Exporter null in buttonblock");
						}




					private boolean isInFilter(String fileName, String filter) {
						if (filter == null)
							return true;
						return fileName.contains(filter);
					}

					private RequestBody createBody(ExportEntry img) {
						ByteArrayOutputStream stream = new ByteArrayOutputStream();
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inPreferredConfig = Bitmap.Config.RGB_565;
						try {
							// Read BitMap by file path.
							Bitmap bitmap = BitmapFactory.decodeFile(img.path, options);
							bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
						} catch (Exception e) {
							e.printStackTrace();
							return null;
						}
						byte[] byteArray = stream.toByteArray();
						RequestBody postBodyImage = new MultipartBody.Builder().setType(MultipartBody.FORM)
								.addFormDataPart(img.name, img.name, RequestBody.create(MediaType.parse("image/*jpg"), byteArray)).build();
						return postBodyImage;
					}



					//Check if a sync is required. Pop current fragment.
					private void goBack() {
						if (myContext.getFragmentActivity() instanceof androidx.fragment.app.FragmentActivity) {
							((androidx.fragment.app.FragmentActivity) myContext.getFragmentActivity()).getSupportFragmentManager().popBackStackImmediate();
						}
						//myContext.reload();
						if (syncRequired)
							gs.sendEvent(MenuActivity.SYNC_REQUIRED);

					}

				});
				myContainer.add(button);
			} else if (type == Type.toggle) {
				final String text =this.getText();
				o.addRow("Creating Toggle Button with text: "+text);
				Log.d("cair","Creating Toggle Button with text: "+text);
				final ToggleButton toggleB = new ToggleButton(ctx);
				//final ToggleButton toggleB = (ToggleButton)LayoutInflater.from(ctx).inflate(R.layout.toggle_button,null);
				//ToggleButton toggleB = new ToggleButton(ctx);
				toggleB.setTextOn(text);
				toggleB.setTextOff(text);
				toggleB.setChecked(enabled);
//				LayoutParams params = new LayoutParams();
//				params.width = LayoutParams.WRAP_CONTENT;
//				params.height = LayoutParams.WRAP_CONTENT;
// 				toggleB.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
// 				toggleB.setLayoutParams(params);

				toggleB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if(onClick==null||onClick.trim().length()==0) {
							o.addRow("");
							o.addRedText("Button "+text+" has no onClick action!");
							Log.e("cair","Button clicked ("+text+") but found no action");
						} else {
							o.addRow("Togglebutton "+text+" pressed. Executing function "+onClick);
							Log.d("cair","Togglebutton "+text+" pressed. Executing function "+onClick+" I am checked: "+isChecked);
							String target = getTarget();
							if (onClick.startsWith("template")) {
								boolean result = myContext.getTemplate().execute(onClick, target);
								if (!result) {
									Log.d("cair","toggling!");
									toggleB.toggle();
								}
							}
							else if (onClick.equals("toggle_visible")) {
								Log.d("cair","Executing toggle");
								Drawable d = myContext.getDrawable(target);
								if (d!=null) {
									if(d.isVisible())
										d.hide();
									else
										d.show();
								} else {
									Log.e("cair","Couldn't find target "+target+" for button");
									for (Drawable dd:myContext.getDrawables()) {
										Log.d("cair",((WF_Widget)dd).getId());
									}
									o.addRow("");
									o.addRedText("Target for button missing: "+target);
								}

							}
						}
					}
				});
//				toggleB.setOnClickListener(new OnClickListener() {
//
//					@Override
//					public void onClick(View v) {
//
//					}
//				});
				button = new WF_ToggleButton(text,toggleB,isVisible,myContext);
				myContainer.add(button);
			}
		} else {
			o.addRow("");
			o.addRedText("Failed to add text field block with id "+blockId+" - missing container "+myContainer);
		}
	}



	public String getStatusVariable() {
		return statusVar;
	}

	public List<EvalExpr> getPrecompiledButtonContext() {
		return buttonContextE;
	}
}