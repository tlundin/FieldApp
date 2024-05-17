package com.teraim.fieldapp.loadermodule;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import com.teraim.fieldapp.FileLoadedCb;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.Connectivity;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.util.List;
import java.util.Random;

import static com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode.Unsupported;
import static com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode.thawed;

public class ModuleLoader implements FileLoadedCb{
    private final LoggerI frontPageLog;
    private final Configuration myModules;
    private final LoggerI debug;
    private final ModuleLoaderListener caller;
    private final String loaderId;
    private final String bundleName;
    private final Context ctx;
    private final boolean allFrozen;
    private boolean active=false,socketBroken=false;
    //not known before loading bundle file.
    private Boolean majorVersionUpdated=null;




    public interface ModuleLoaderListener {

        void loadSuccess(String loaderId, boolean majorVersionChange, CharSequence loadText, boolean socketBroken);
        void loadFail(String loaderId);
    }



    public ModuleLoader(String loaderId, Configuration currentlyKnownModules, LoggerI log, PersistenceHelper glPh, boolean allFrozen, LoggerI debugConsole,ModuleLoaderListener caller,Context ctx) {
        myModules = currentlyKnownModules;
        frontPageLog = log;

        this.debug=debugConsole;
        //Passive logger will be the main log window...text will appear on About page.
        this.caller = caller;
        this.loaderId=loaderId;
        this.ctx=ctx;
        //boolean that tells if all files already exist frozen.
        this.allFrozen = allFrozen;

        bundleName = glPh.get(PersistenceHelper.BUNDLE_NAME);
    }


    private ConfigurationModule module=null;

    public boolean isActive() {
        return active;
    }

    //make sure only one call
    public void loadModules(Boolean majorVersionUpdated, boolean socketBroken) {
        if (isActive())
            Log.e("vortex","Loader is active..discard");
        active=true;
        this.socketBroken=socketBroken;
        //if major version is unknown, bundle will be (pre)loaded.
        this.majorVersionUpdated=majorVersionUpdated;
        _loadModules();
    }

    private void _loadModules() {
        //check if any module needs to load. Block any calls if load ongoing.
        frontPageLog.clear();
        boolean majorVersionIsKnown = majorVersionUpdated !=null;
        if (!majorVersionIsKnown) {
            module = myModules.getModule(bundleName);
            if (module==null) {
                frontPageLog.clear();
                frontPageLog.addRow("Load aborted (no bundle)");
                return;
            } else {
                Log.d("amazon","Loading bundle"+bundleName+" to get version");
            }
        } else {
            module = myModules.next();
        }
        if (module != null) {
            debug.addRow("");
            debug.addCriticalText(module.getLabel()+" - ");
            frontPageLog.addRow("Loading " + module.getLabel());
            frontPageLog.draw();
            Log.d("amazon", module.getLabel() + " :");
            //force load if major version is unknown and version handler set to major
            boolean detailed = module.versionControl.startsWith("Det");
            boolean forced = module.versionControl.equals("Forced") || (!majorVersionIsKnown && !detailed);

            //All already loaded and no major update?
            Log.d("beboop","method: "+module.versionControl+" allfrozen: "+allFrozen+" majorupdated: "+majorVersionUpdated+" module: "+module.getLabel());
            if (allFrozen) {
                Log.d("Terje", "Calling onFileloaded for " + module.getLabel());
                onFileLoaded(new LoadResult(module, ErrorCode.sameold));
            }
//            if (!forced && !detailed && allFrozen && !majorVersionUpdated) {
//                Log.d("beboop","allfrozen and major not updated and not forced and not detailed");
//                onFileLoaded(new LoadResult(module, ErrorCode.sameold));
//            }
            //Supposed to load from web, but no connection?
            else if ((socketBroken || (module.source == ConfigurationModule.Source.internet) && !Connectivity.isConnected(ctx))) {
                majorVersionUpdated=false;
                if(allFrozen) {
                    Log.d("vortex", "no network");
                    if (module.thaw(this)) {
                        thawed(module, new LoadResult(module, thawed));
                        _loadModules();
                    } else {
                        Log.d("nonet", "thawing started " + module.getLabel());

                    }
                } else {
                    failAndExitLoad(module,new LoadResult(module,socketBroken?ErrorCode.socket_timeout:ErrorCode.IOError,"Network is not reachable."));
                    return;
                }

            } else {
                //Forced, detailed or the module is not frozen yet?
                if (forced || detailed || !module.frozenFileExists() || majorVersionUpdated) {
                    if (module.source == ConfigurationModule.Source.internet) {
                        frontPageLog.addText("waiting for network...");
                    }
                    module.load(this);
                } else {
                    //module is frozen, but majorVersion not yet known.
                    onFileLoaded(new LoadResult(module, ErrorCode.sameold));
                }
            }

        } else {
            frontPageLog.clear();
            frontPageLog.addRow("Loaded [" + loaderId + "].");
            active=false;
            caller.loadSuccess(loaderId, majorVersionUpdated, debug.getLogText(),socketBroken);
        }
    }





    @Override
    public void onUpdate(Integer ...args) {
        Start.singleton.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (args.length==1)
                    frontPageLog.addText(module.getLabel()+": "+args[0].toString());
                else
                    frontPageLog.addText(module.getLabel()+": "+args[0].toString()+"/"+args[1].toString());

            }
        });

    }


    int n=0;
    String[] ticks = new String[] { "/","-", "\\", "|", "/", "-", "|"};
    boolean somethingChanged=false;

    @Override
    public void onFileLoaded(final LoadResult res) {
        //assume all done for this module
        boolean allDone = true;

        final ConfigurationModule module = res.module;
        if (module != null) {
            debug.addRow("Module " + res.module.fileName + " loaded. Returns code " + res.errCode.name() + (res.errorMessage != null ? " and errorMessage: " + res.errorMessage : ""));
            Log.d("vortex", "Module " + res.module.fileName + " loaded. Returns code " + res.errCode.name() + (res.errorMessage != null ? " and errorMessage: " + res.errorMessage : ""));
            switch (res.errCode) {
                case existingVersionIsMoreCurrent:
                    debug.addCriticalText("*****");
                    debug.addCriticalText("Module: "+module.getLabel());
                    debug.addCriticalText(" Remote version older and therefore not loaded: ");
                    debug.addRedText(res.errorMessage);
                    debug.addCriticalText("*****");
                case sameold:
                    if (module.isBundle) {
                        majorVersionUpdated = false;
                    }

                    //continue immediately on true = already thawed.
                    if (module.thaw(this)) {
                        thawed(module,new LoadResult(module, thawed));
                        break;
                    }
                    else {
                        Log.d("babush","thawing started "+module.getLabel());
                        //wait for result
                        allDone = false;
                    }
                    break;

                case thawed:
                    thawed(module,res);
                    //if bundle failed to load but was thawed, we know the major version has not been updated.
                    if (module.isBundle && module.tryingThawAfterFail) {
                        debug.addRow("");
                        debug.addRedText("Loading existing file succeeded.");
                        majorVersionUpdated = false;
                    }
                    break;

                case frozen:
                    //if bundle was frozen, we know the major version has been updated.
                    if (module.isBundle) {
                        Log.d("amazon","new bundle version");
                        majorVersionUpdated=true;
                    }
                    module.setLoaded(true);
                    String txt = "";
                    if (module.newVersion != -1)
                        txt = txt + "["+Float.toString(module.newVersion)+"] new!";
                    debug.addCriticalText(txt);
                    break;
                case thawFailed:
                    //check if allfrozen is set. in that case, something is wrong.
                    if (allFrozen) {
                        debug.addRedText("Thawing failed. for "+module.getLabel()+". Please restart the App.");
                        failAndExitLoad(module, new LoadResult(module,ErrorCode.configurationError,"please reload the App."));
                        return;
                    }
                    //if thaw failed, remove file and try again except if it has already tried.
                    if (module.tryingThawAfterFail) {
                        Log.d("vortex", "Failing finally.");
                        debug.addRedText("Thawing failed. for "+module.getLabel()+"..giving up");
                        if (module.isRequired()) {
                            failAndExitLoad(module, res);
                            return;
                        } else
                            module.setNotFound();

                    } else {
                        Log.d("vortex", "Thaw failed..Retrying...reload from network");
                        debug.addYellowText("thaw failed...trying to reload from network");
                        module.tryingWebAfterFail=true;
                        module.deleteFrozen();
                        module.setFrozenVersion(-1);
                        module.load(this);
                        allDone = false;
                    }
                    break;
                case socket_timeout:
                    //set network to dead.
                    socketBroken=true;
                    break;
                case Unsupported:
                case IOError:
                case BadURL:
                case ParseError:
                case Aborted:
                case notFound:
                case noData:
                case HostNotFound:
                case slowConnection:
                    printError(res);
                    if (module.tryingWebAfterFail) {
                        debug.addRow("");
                        debug.addRedText("Failed to load file from network.");
                        if (module.isRequired()) {
                            failAndExitLoad(module,res);
                            return;
                        } else
                            module.setNotFound();
                    }
                    else if (module.isRequired() && res.errCode == Unsupported) {
                        if (module.frozenFileExists()) {
                            new AlertDialog.Builder(ModuleLoader.this.ctx).setTitle(R.string.warning)
                                    .setMessage(R.string.old_xml_message+res.errorMessage+R.string.old_xml_message_cont)
                                    .setPositiveButton(R.string._continue, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (module.frozenFileExists()) {
                                                if (module.thaw(ModuleLoader.this)) {
                                                    thawed(module, new LoadResult(module, thawed));
                                                    //continue to next module.
                                                    _loadModules();
                                                } else {
                                                    Log.d("vortex", "Thawing restarted (unsupported) for " + module.getLabel());
                                                }
                                            }
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                                    .setCancelable(false)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                            allDone=false;
                            break;
                        } else {
                            failAndExitLoad(module,res);
                            return;
                        }
                    } else if (module.isRequired() && !module.frozenFileExists()) {
                        new AlertDialog.Builder(ModuleLoader.this.ctx).setTitle("Error")
                                .setMessage("Load aborted. Failed to load required module "+module.getFileName())
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        failAndExitLoad(module,res);
                                    }})
                                .setCancelable(false)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                        allDone=false;
                        break;
                        //try use frozen if exists.
                    } else if (module.frozenFileExists()) {
                        //continue immediately on true = already thawed.
                        Log.d("vortex","frozen exists. ");
                        debug.addRow("");
                        debug.addCriticalText("Using current: " );
                        if (module.thaw(this)) {
                            thawed(module,new LoadResult(module, thawed));
                        }
                        else {
                            Log.d("vortex","Thawing after fail for "+module.getLabel());
                            module.tryingThawAfterFail=true;
                            allDone=false;
                        }

                    } else {
                        Log.d("vortex","not found. ");
                        module.setNotFound();
                    }

                    break;
                case reloadDependant:
                    Log.d("vortex", "Dependant [" + res.errorMessage + "] needs to be reloaded.");
                    ConfigurationModule reloadModule = myModules.getModule(res.errorMessage);
                    if (reloadModule != null) {
                        if (reloadModule.isMissing() && !reloadModule.isRequired()) {
                            Log.d("vortex", "Dependant is not required and is not defined");

                        } else {
                            reloadModule.setLoaded(false);
                            reloadModule.setFrozenVersion(-1);

                            //module.setFrozenVersion(-1);
                            //module.setLoaded(false);
                            Log.d("vortex", "Now retry load of modules");
                            //debug.addRow("");
                            //debug.addGreenText("Reload required for dependant "+res.errorMessage);
                            //majorVersionChange=true;
                            reloadModule.load(this);
                            allDone = false;

                        }
                    }
                    break;
                default:
                    debug.addCriticalText("?: " + res.errCode.name());
                    break;
            }
            //Is this module ready? Continue on next.
            if(allDone) {
                _loadModules();
            }
        }
        Log.d("vortex","Falling out of onFileLoaded");
    }

    private void failAndExitLoad(ConfigurationModule module, LoadResult res) {
        debug.addCriticalText(res.errCode.name());
        debug.addCriticalText(res.errorMessage);
        frontPageLog.addRedText(module.getLabel()+" [" + res.errCode.name() + "]. Check log for details");
        //Need to enable the settings menu.
        caller.loadFail(loaderId);
    }

    private void thawed(ConfigurationModule module, LoadResult res) {
        debug.addRow("Module " + module.getFileName() + " was thawed.");
        module.setLoaded(true);

        float frozenModuleVersion = module.getFrozenVersion();
        if (frozenModuleVersion != -1)
            debug.addCriticalText(" [" + frozenModuleVersion + "]");


    }



    private void printError(LoadResult res) {
        ErrorCode errCode = res.errCode;
        debug.addCriticalText("*********");
        if (errCode==ErrorCode.notFound && !module.isRequired())
            debug.addCriticalText(module.getFileName());
        else
            debug.addRedText(module.getFileName());
        if (res.errorMessage!=null)
            debug.addCriticalText(res.errorMessage);
        debug.addCriticalText(errCode+"");
        debug.addCriticalText("*********");

        if (errCode==ErrorCode.IOError) {
            Log.d("vortex","Io-error");
            if (ctx!=null && !Connectivity.isConnected(ctx)) {
                debug.addRow("No network");
                Log.d("vortex", "No network");
            }
            else {
                if (res.errorMessage !=null)
                    debug.addRow(res.errorMessage);
                else
                    debug.addRow("File not found (Network error)");
            }
        } else if (errCode== Unsupported) {
            debug.addRow("");
            debug.addRedText("The version of FieldPad you use cannot run the latest bundle! Min version required is: "+res.errorMessage+".");
        } else if (errCode==ErrorCode.ParseError) {
            debug.addRow("");
            debug.addRedText("Error. Please check log for details");
           Log.e("fall",res.errorMessage==null?"":res.errorMessage);
        } else
            debug.addYellowText(" "+res.errCode.name());


    }

    //TODO:REMOVE
    public void onFileLoaded(ErrorCode errCode, String version) {}

    public void stop() {
        Log.e("vortex","In stop for "+loaderId);
        List<ConfigurationModule> list = myModules.getAll();
        for (ConfigurationModule module:list) {
            Log.e("vortex","Cancelling loader process for "+module.getFileName());
            module.cancelLoader();
        }
    }


}
