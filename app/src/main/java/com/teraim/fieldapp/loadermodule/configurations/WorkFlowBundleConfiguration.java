package com.teraim.fieldapp.loadermodule.configurations;

import static com.teraim.fieldapp.utils.Tools.getFileContentAsString;

import android.content.Context;
import android.util.Log;

import com.google.gson.reflect.TypeToken;
import com.teraim.fieldapp.dynamic.blocks.AddEntryToFieldListBlock;
import com.teraim.fieldapp.dynamic.blocks.AddFilter;
import com.teraim.fieldapp.dynamic.blocks.AddGisFilter;
import com.teraim.fieldapp.dynamic.blocks.AddGisLayerBlock;
import com.teraim.fieldapp.dynamic.blocks.AddGisPointObjects;
import com.teraim.fieldapp.dynamic.blocks.AddSumOrCountBlock;
import com.teraim.fieldapp.dynamic.blocks.AddVariableToEntryFieldBlock;
import com.teraim.fieldapp.dynamic.blocks.AddVariableToEveryListEntryBlock;
import com.teraim.fieldapp.dynamic.blocks.AddVariableToListEntry;
import com.teraim.fieldapp.dynamic.blocks.BarChartBlock;
import com.teraim.fieldapp.dynamic.blocks.Block;
import com.teraim.fieldapp.dynamic.blocks.BlockAddAggregateColumnToTable;
import com.teraim.fieldapp.dynamic.blocks.BlockAddColumnsToTable;
import com.teraim.fieldapp.dynamic.blocks.BlockAddVariableToTable;
import com.teraim.fieldapp.dynamic.blocks.BlockCreateListEntriesFromFieldList;
import com.teraim.fieldapp.dynamic.blocks.BlockCreateTable;
import com.teraim.fieldapp.dynamic.blocks.BlockCreateTableEntriesFromFieldList;
import com.teraim.fieldapp.dynamic.blocks.BlockCreateTextField;
import com.teraim.fieldapp.dynamic.blocks.BlockDeleteMatchingVariables;
import com.teraim.fieldapp.dynamic.blocks.BlockGoSub;
import com.teraim.fieldapp.dynamic.blocks.ButtonBlock;
import com.teraim.fieldapp.dynamic.blocks.ConditionalContinuationBlock;
import com.teraim.fieldapp.dynamic.blocks.ContainerDefineBlock;
import com.teraim.fieldapp.dynamic.blocks.CoupledVariableGroupBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateCategoryDataSourceBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateEntryFieldBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateGisBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateImageBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateSliderEntryFieldBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateSortWidgetBlock;
import com.teraim.fieldapp.dynamic.blocks.DisplayValueBlock;
import com.teraim.fieldapp.dynamic.blocks.JumpBlock;
import com.teraim.fieldapp.dynamic.blocks.LayoutBlock;
import com.teraim.fieldapp.dynamic.blocks.MenuEntryBlock;
import com.teraim.fieldapp.dynamic.blocks.MenuHeaderBlock;
import com.teraim.fieldapp.dynamic.blocks.NoOpBlock;
import com.teraim.fieldapp.dynamic.blocks.PageDefineBlock;
import com.teraim.fieldapp.dynamic.blocks.RoundChartBlock;
import com.teraim.fieldapp.dynamic.blocks.RuleBlock;
import com.teraim.fieldapp.dynamic.blocks.SetValueBlock;
import com.teraim.fieldapp.dynamic.blocks.StartBlock;
import com.teraim.fieldapp.dynamic.blocks.StartCameraBlock;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Not_ClickableField_SumAndCountOfVariables;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.loadermodule.XMLConfigurationModule;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;
import com.teraim.fieldapp.utils.Tools.Unit;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorkFlowBundleConfiguration extends XMLConfigurationModule {

	private String myApplication;
	private final LogRepository o;
	private final String cacheFolder;
	private String language="se";

	private String imageMetaFormat=null;

	public WorkFlowBundleConfiguration(Context context,String cachePath, PersistenceHelper globalPh, PersistenceHelper ph,
									   String urlOrPath, String bundle, LogRepository debugConsole) {
		super(context, globalPh,ph, urlOrPath, bundle,"Workflow bundle       ");
		this.o=debugConsole;
		cacheFolder = cachePath;
		hasSimpleVersion=true;
		isBundle=true;
		o.addGreenText("Parsing Workflow bundle");
	}

	@Override
	public float getFrozenVersion() {
		Log.d("abba","getfrozen called on workbundle");
		//return -1 if first time loaded during a day.
		return (ph.getF(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE));

	}

	@Override
	protected void setFrozenVersion(float version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE,version);
	}

	public boolean isRequired() {
		return true;
	}

	//workflows will be added to this one.
	private final List<Workflow> bundle = new ArrayList<>();

	@Override
	protected LoadResult prepare(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "bundle");
		myApplication = parser.getAttributeValue(null, "application");

		float appVersion,newWorkflowVersion = -1;
		try {
			newWorkflowVersion = Float.parseFloat(parser.getAttributeValue(null, "version"));
			appVersion = Float.parseFloat(parser.getAttributeValue(null, "app_version"));
			ph.put(PersistenceHelper.NEW_APP_VERSION,appVersion);}
		catch (Exception e) {
			Log.e("vortex","No app version, or no workflowversion.");
			o.addCriticalText("No appversion and/or workflow-version. Will default to 0. Please add.");
		}
		String minVersion = parser.getAttributeValue(null, "minVortexVersion");
		//this determines if the image meta data is in file or xml format.
		imageMetaFormat = parser.getAttributeValue(null,"img_meta_format");
		Log.d("franzon","imagemetaformat "+(imageMetaFormat==null?"null":imageMetaFormat));
		Log.d("franzon","minvortexversion "+(minVersion==null?"null":minVersion));





		return null;
	}



	@Override
	protected LoadResult parse(XmlPullParser parser) throws XmlPullParserException, IOException {
		while (parser.next() != XmlPullParser.END_TAG && parser.getEventType() != XmlPullParser.END_DOCUMENT) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				Log.d("NILS","Skipping "+parser.getEventType());
				continue;
			}
			String name = parser.getName();
			if (parser.getName().equals("language")) {
				;
				o.addGreenText("Language set to: "+language);
				language = readText("language",parser);
			}
			else if (name.equals("workflow")) {
				//Add workflow to bundle, return a count.
				bundle.add(readWorkflow(parser));
				return null;
			} else {
				skip(name,parser,o);
			}
		}
		return new LoadResult(this,ErrorCode.parsed);
	}
	private Workflow readWorkflow(XmlPullParser parser) throws XmlPullParserException, IOException {

		Workflow wf = new Workflow();
		parser.require(XmlPullParser.START_TAG, null, "workflow");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}

			String name = parser.getName();
			if (name.equals("blocks")) {
				wf.addBlocks(readBlocks(parser));
			} else {
				skip(name,parser,o);
			}
		}

		return wf;


	}



	/**
	 * Read blocks. Create respective class and return as a list.
	 */
	private static final boolean isSum = false;
	private static final boolean isCount = true;

	private List<Block> readBlocks(XmlPullParser parser) throws IOException, XmlPullParserException {
		List<Block> blocks= new ArrayList<>();
		parser.require(XmlPullParser.START_TAG, null,"blocks");
		String name="";
		try {
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				name = parser.getName();

				switch (name) {
					case "block_start":
						blocks.add(readBlockStart(parser));
						break;
					case "block_define_page":
						blocks.add(readPageDefineBlock(parser));
						break;
					case "block_define_container":
						blocks.add(readContainerDefineBlock(parser));
						break;
					case "block_layout":
						blocks.add(readBlockLayout(parser));
						break;
					case "block_button":
						blocks.add(readBlockButton(parser));
						break;
					case "block_add_rule":
						blocks.add(readBlockAddRule(parser));
						break;
					case "block_create_sort_widget":
						blocks.add(readBlockCreateSorting(parser));
						break;
					//This is a dummy call. Not supported block.
					case "block_create_list_entries":
						dummyWarning("block_create_list_entries", parser);
						break;
					case "block_create_entry_field":
						blocks.add(readBlockCreateEntryField(parser));
						break;
//entryfields
					case "block_add_sum_of_selected_variables_display":
						blocks.add(readBlockAddSelectionOrSum(parser, isSum));
						break;
					case "block_add_number_of_selections_display":
						blocks.add(readBlockAddSelectionOrSum(parser, isCount));
						break;
					case "block_create_slider_entry_field":
						blocks.add(readBlockCreateEntryFieldSlider(parser));
						break;
					case "block_create_display_field":
						blocks.add(readBlockCreateDisplayField(parser));
						break;
//
					case "block_create_list_entries_from_field_list":
						blocks.add(readBlockCreateListEntriesFromFieldList(parser));
						break;
					case "block_create_table":
						blocks.add(readBlockCreateTable(parser));
						break;
					case "block_create_table_entries_from_field_list":
						blocks.add(readBlockCreateTableEntriesFromFieldList(parser));
						break;
					case "block_add_variable_to_every_list_entry":
						blocks.add(readBlockAddVariableToEveryListEntry(parser));
						break;
					case "block_add_variable_to_entry_field":
						blocks.add(readBlockAddVariableToEntryField(parser));
						break;
					case "block_add_column_to_table":
					case "block_add_columns_to_table":
						blocks.add(readBlockAddColumnsToTable(parser));
						break;
					case "block_add_aggregate_column_to_table":
						blocks.add(readBlockAddAggregateColumnToTable(parser));
						break;
					case "block_add_variable_to_table":
						blocks.add(readBlockAddVariableToTable(parser));
						break;
					case "block_add_entry_to_field_list":
						blocks.add(readBlockAddEntryToFieldList(parser));
						break;
					case "block_add_variable_to_list_entry":
						blocks.add(readBlockAddVariableToListEntry(parser));
						break;
					case "block_conditional_continuation":
						blocks.add(readBlockConditionalContinuation(parser));
						break;
					case "block_jump":
						blocks.add(readBlockJump(parser));
						break;
					case "block_set_value":
						blocks.add(readBlockSetValue(parser));
						break;
					case "block_define_menu_header":
						blocks.add(readBlockDefineMenuHeader(parser));
						break;
					case "block_define_menu_entry":
						blocks.add(readBlockDefineMenuEntry(parser));
						break;
					case "block_create_text_field":
						blocks.add(readBlockCreateTextField(parser));
						break;
					case "block_add_filter":
						blocks.add(readBlockAddFilter(parser));
						break;
					case "block_create_round_chart":
						blocks.add(readBlockCreateChart(ChartType.round, parser));
						break;
					case "block_create_bar_chart":
						blocks.add(readBlockCreateChart(ChartType.bar, parser));
						break;
					case "block_create_category_data_source":
						blocks.add(readBlockCreateCategoryDataSource(parser));
						break;
					case "block_create_xy_data_source":
						blocks.add(readBlockCreateCategoryDataSource(parser));
						break;
					case "block_create_picture":
						blocks.add(readBlockCreatePicture(parser));
						break;
					case "block_add_gis_image_view":
						blocks.add(readBlockAddGisView(parser));
						break;
					case "block_add_gis_layer":
						blocks.add(readBlockAddGisLayer(parser));
						break;
					case "block_add_gis_point_objects":
						blocks.add(readBlockAddGisPointObjects(parser, GisObjectType.Point));
						break;
					case "block_add_gis_multipoint_objects":
						blocks.add(readBlockAddGisPointObjects(parser, GisObjectType.Multipoint));
						break;
					case "block_add_gis_polygons":
						blocks.add(readBlockAddGisPointObjects(parser, GisObjectType.Polygon));
						break;
					case "block_add_gis_linestring_objects":
						blocks.add(readBlockAddGisPointObjects(parser, GisObjectType.Linestring));
						break;
					case "block_add_gis_filter":
						blocks.add(readBlockAddGisFilter(parser));
						break;
					case "block_delete_matching_variables":
						blocks.add(readBlockDeleteMatchingVariables(parser));
						break;
					case "block_no_op":
						blocks.add(readBlockNoOp(parser));
						break;
					case "block_go_sub":
						blocks.add(readBlockGoSub(parser));
						break;
					case "block_define_coupled_variable_group":
						blocks.add(readBlockSliderGroup(parser));
						break;
					case "block_start_camera":
						blocks.add(readBlockStartCamera(parser));
						break;
					default:
						skip(name, parser, o);
						break;
				}
			}
		} catch (XmlPullParserException e) {
			Log.d("vortex","Got parse error when reading "+name+" on line "+e.getLineNumber());
			Log.d("vortex","Cause: "+e.getCause());
			Log.d("vortex","Message: "+e.getMessage());
			o.addCriticalText("Got parse error when reading "+name+" on line "+e.getLineNumber());
			o.addText("Message from parser:");
			o.addText(e.getMessage());
			throw e;
		}
		//Check that no block has the same ID
		Set<String> tempSet = new HashSet<>();
		for (Block b:blocks)  {
			if (!tempSet.add(b.getBlockId())) {
				o.addYellowText("Duplicate Block ID "+b.getBlockId()+" This is potentially serious");
				return blocks;
			}
		}
		;
		o.addGreenText("No duplicate block IDs");
		return blocks;
	}

	private Block readBlockStartCamera(XmlPullParser parser) throws IOException, XmlPullParserException {
		o.addText("Parsing block: block_start_camera...");
		String id=null,fileName=null;
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID", parser);
			} else if (name.equals("target")) {
				fileName = readText("target", parser);
			}
			else {

				Log.e("vortex","Skipped "+name);
				skip(name,parser);
			}
		}
		checkForNull(fileName);
		return new StartCameraBlock(id,fileName);
	}
	private Block readBlockSliderGroup(XmlPullParser parser) throws IOException, XmlPullParserException {
		o.addText("Parsing block: block_define_coupled_variable_group...");
		String id=null,groupName=null,function=null,arguments=null,delay=null;


		parser.require(XmlPullParser.START_TAG, null,"block_define_coupled_variable_group");
		Log.d("vortex","In block_define_coupled_variable_group");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "name":
					groupName = readText("name", parser);
					break;
				case "function":
					function = readText("function", parser);
					break;
				case "arg":
					arguments = readText("arg", parser);
					break;
				case "delay":
					delay = readText("delay", parser);
					break;
				default:

					Log.e("vortex", "Skipped " + name);
					skip(name, parser);
					break;
			}
		}

		checkForNull("block_ID",id);
		return new CoupledVariableGroupBlock(id,groupName,function,arguments,delay);

	}

	private Block readBlockGoSub(XmlPullParser parser) throws IOException, XmlPullParserException {
		o.addText("Parsing block: block_go_sub...");
		String id=null,target=null;


		parser.require(XmlPullParser.START_TAG, null,"block_go_sub");
		Log.d("vortex","Parsing block_go_sub!!");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("target")) {
				target = readText("target",parser);
			}
			else {

				Log.e("vortex","Skipped "+name);
				skip(name,parser);
			}
		}

		checkForNull("block_ID",id);
		return new BlockGoSub(id,target);

	}

	private Block readBlockNoOp(XmlPullParser parser) throws IOException, XmlPullParserException {
		o.addText("Parsing block: block_no_operation...");
		String id=null;


		parser.require(XmlPullParser.START_TAG, null,"block_no_op");
		Log.d("vortex","In block_no_op");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			}
			else {

				Log.e("vortex","Skipped "+name);
				skip(name,parser);
			}
		}

		checkForNull("block_ID",id);
		return new NoOpBlock(id);

	}

	private Block readBlockDeleteMatchingVariables(XmlPullParser parser) throws IOException, XmlPullParserException {
		o.addText("Parsing block: block_delete_matching_variables...");
		String id=null,label=null,target=null,pattern=null;


		parser.require(XmlPullParser.START_TAG, null,"block_delete_matching_variables");
		Log.d("vortex","In block_delete_matching_variables");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "target":
					target = readText("target", parser);
					break;
				case "pattern":
					pattern = readText("pattern", parser);
					break;
				case "label":
					label = readText("label", parser);
					break;
				default:

					Log.e("vortex", "Skipped " + name);
					skip(name, parser);
					break;
			}
		}

		checkForNull("block_ID",id,"target",target);
		return new BlockDeleteMatchingVariables(id, label, target, pattern);

	}

	private Block readBlockAddFilter(XmlPullParser parser) throws IOException, XmlPullParserException {
		o.addText("Parsing block: block_add_filter...");
		String id=null,target=null,type=null,selectionField=null,selectionPattern=null;


		parser.require(XmlPullParser.START_TAG, null,"block_add_filter");
		Log.d("vortex","In block_add_filter!!");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "target":
					target = readText("target", parser);
					break;
				case "type":
					type = readText("type", parser);
					break;
				case "selection_field":
					selectionField = readText("selection_field", parser);
					break;
				case "selection_pattern":
					selectionPattern = readText("selection_pattern", parser);
					break;
				default:

					Log.e("vortex", "Skipped " + name);
					skip(name, parser);
					break;
			}
		}

		checkForNull("block_ID",id,"target",target,"type",type);
		return new AddFilter(id,target,type,selectionField,selectionPattern);

	}
	private Block readBlockAddGisFilter(XmlPullParser parser) throws IOException, XmlPullParserException {
		o.addText("Parsing block: block_add_gis_filter...");
		String id=null,nName=null,targetName=null,targetLayer=null,label=null, color=null,polyType=null,fillType=null,
				imgSource=null,radius=null,expression=null;
		boolean hasWidget = true;

		parser.require(XmlPullParser.START_TAG, null,"block_add_gis_filter");
		Log.d("vortex","In block_add_gis_filter!!");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("target_name")) {
				targetName = readText("target_name",parser);
			} else if (name.equals("target_layer")) {
				targetLayer = readText("target_layer",parser);
			} else if (name.equals("label")) {
				label = readText("label",parser);
			} else if (name.equals("color")) {
				color = readText("color",parser);
			} else if (name.equals("paint_style")) {
				fillType = readText("paint_style",parser);
			} else if (name.equals("poly_type")) {
				polyType = readText("poly_type",parser);
			} else if (name.equalsIgnoreCase("name")) {
				nName = readText("name",parser);
			}  else if (name.equalsIgnoreCase("expression")) {
				expression = readText("expression",parser);
			} else if (name.equalsIgnoreCase("img_source")) {
				imgSource = readText("img_source",parser);
			} else if (name.equalsIgnoreCase("radius")) {
				radius= readText("radius",parser);
			} else if (name.equalsIgnoreCase("has_widget")) {
				hasWidget = readText("has_widget",parser).equalsIgnoreCase("true");
			}
			else {

				Log.e("vortex","Skipped "+name);
				skip(name,parser);
			}
		}

		checkForNull("block_ID",id,"targetName",targetName,"targetLayer",targetLayer,"expression",expression);
		return new AddGisFilter(id,nName,label,targetName,targetLayer,expression,imgSource,radius,color,polyType,fillType,hasWidget,o);

	}

	/*
	 <block_add_gis_point_object>
	 <block_ID>2145</block_ID>
	 <name>Rutor</name>
	 <target>Layer 1</target>
	 <coord_type>Sweref</coord_type>
	 <x_variable>Koord5kmLR_EW</x_variable>
	 <y_variable>Koord5kmLR_NS</y_variable>
	 <obj_context>år=?,ruta=?</obj_context>
	 <label>RUTA @ruta</label>
	 </block_add_gis_point_object>
	 */


	private Block readBlockAddGisPointObjects(XmlPullParser parser,GisObjectType type) throws IOException, XmlPullParserException {
		o.addText("Parsing block: block_add_gis_point_objects...");
		String id=null,nName=null,target=null,label=null,coordType = null, color=null,border_color=null,polyType=null,fillType=null,line_width="1",
				palette = null, location=null,objContext=null,imgSource=null,refreshRate=null,radius=null,onClick=null,statusVariable = null;
		boolean isVisible=true,isUser=true,createAllowed=false,use_image_icon_on_map=false;

		//parser.require(XmlPullParser.START_TAG, null,"block_add_gis_point_objects")
		Log.d("vortex","In block_add_gis_point_objects!!");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("target")) {
				target = readText("target",parser);
			} else if (name.equals("label")) {
				label = readText("label",parser);
			} else if (name.equals("color")) {
				color = readText("color",parser);
			} else if (name.equals("border_color")) {
				border_color = readText("border_color",parser);
			} else if (name.equals("paint_style")) {
				fillType = readText("paint_style",parser);
			} else if (name.equals("poly_type")) {
				polyType = readText("poly_type",parser);
			} else if (name.equals("is_visible")) {
				isVisible = readText("is_visible",parser).equals("true");
			} else if (name.equals("is_user")) {
				isUser = readText("is_user",parser).equals("true");
			} else if (name.equalsIgnoreCase("name")) {
				nName = readText("name",parser);
			} else if (name.equalsIgnoreCase("use_image_icon_on_map")) {
				use_image_icon_on_map = readText("use_image_icon_on_map",parser).equals("true");
			} else if (name.equalsIgnoreCase("coord_type")) {
				coordType = readText("coord_type",parser);
			} else if (name.equalsIgnoreCase("gis_variables")) {
				location = readText("gis_variables",parser);
			}  else if (name.equalsIgnoreCase("obj_context")) {
				objContext = readText("obj_context",parser);
			} else if (name.equalsIgnoreCase("source")) {
				imgSource = readText("source",parser);
			} else if (name.equalsIgnoreCase("on_click")) {
				onClick = readText("on_click",parser);
			} else if (name.equalsIgnoreCase("refresh_rate")) {
				refreshRate = readText("refresh_rate",parser);
			} else if (name.equalsIgnoreCase("radius")) {
				radius= readText("radius",parser);
			} else if (name.equalsIgnoreCase("create_allowed")) {
				createAllowed= readText("create_allowed",parser).equals("true");
			} else if (name.equalsIgnoreCase("palette")) {
				palette = readText("palette",parser);
			} else if (name.equalsIgnoreCase("status_variable")) {
				statusVariable = readText("status_variable",parser);
			} else if (name.equalsIgnoreCase("line_width")) {
				line_width = readText("line_width", parser);
			}
			else {
				//Log.e("vortex","Skipped "+name);
				skip(name,parser);
			}
		}

		checkForNull("block_ID",id,"target",target,"location",location);
		if (imgSource!=null&&!imgSource.isEmpty())
			Tools.preCacheImage(baseBundlePath+"extras/",imgSource,cacheFolder,o);

		return new AddGisPointObjects(id,nName,label,target,objContext,coordType,location,imgSource,use_image_icon_on_map,refreshRate,radius,isVisible,type,color,border_color,polyType,fillType,onClick,statusVariable,isUser,createAllowed,palette,line_width,o);

	}

	private Block readBlockAddGisLayer(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_add_gis_layer...");
		String id=null,nName=null,target=null,label=null;
		boolean isVisible=true,hasWidget=true,showLabels=false,isBold=false;

		parser.require(XmlPullParser.START_TAG, null,"block_add_gis_layer");
		//Log.d("vortex","In block block_add_gis_layer!!");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("target")) {
				target = readText("target",parser);
			} else if (name.equals("label")) {
				label = readText("label",parser);
			} else if (name.equals("show_labels")) {
				showLabels = "true".equals(readText("show_labels", parser));
			} else if (name.equals("is_visible")) {
				isVisible = "true".equals(readText("is_visible", parser));
			}else if (name.equalsIgnoreCase("name")) {
				nName = readText("name",parser);
			}else if (name.equalsIgnoreCase("has_widget")) {
				hasWidget = "true".equals(readText("has_widget", parser));
			} else if (name.equalsIgnoreCase("is_bold")) {
				isBold = "true".equals(readText("is_bold", parser));
			}
			else {
				Log.e("vortex","Skipped "+name);
				skip(name,parser);
			}
		}

		checkForNull("block_ID",id,"target",target);
		return new AddGisLayerBlock(id,nName,label,target,isVisible,hasWidget,showLabels,isBold);

	}

	private Block readBlockAddGoogleGis(XmlPullParser parser) throws IOException,XmlPullParserException {
		o.addText("Parsing block: block_add_google_gis...");
		parser.require(XmlPullParser.START_TAG, null,"block_add_google_gis");
		Log.d("vortex","In block block_add_gis_view!!");
		String name;
		return null;
	}
	private Block readBlockAddGisView(XmlPullParser parser) throws IOException,XmlPullParserException {
		o.addText("Parsing block: block_add_gis_image_view...");
		String id=null,nName=null,container=null,source=null;
		String N=null,E=null,S=null,W=null;
		boolean isVisible=true,hasSatNav=false,showTeam=false;

		parser.require(XmlPullParser.START_TAG, null,"block_add_gis_image_view");
		Log.d("vortex","In block block_add_gis_view!!");
		String name;

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("name")) {
				nName = readText("name",parser);
			} else if (name.equals("container_name")) {
				container = readText("container_name",parser);
			} else if (name.equals("source")) {
				source = readText("source",parser);
			} else if (name.equals("is_visible")) {
				isVisible = readText("is_visible",parser).equals("true");
			} else if (name.equalsIgnoreCase("TopN")) {
				N = readText("TopN",parser);
			} else if (name.equalsIgnoreCase("TopE")) {
				W = readText("TopE",parser);
			} else if (name.equalsIgnoreCase("BottomE")) {
				E = readText("BottomE",parser);
			} else if (name.equalsIgnoreCase("BottomN")) {
				S = readText("BottomN",parser);
			} else if (name.equals("car_navigation_on")) {
				hasSatNav = readText("car_navigation_on",parser).equals("true");
			} else if (name.equals("team")) {
				showTeam = readText("team",parser).equals("true");
				Log.d("bortex","showteam is now "+showTeam);
			}
			else {
				Log.e("vortex","Skipped "+name);
				skip(name,parser);
			}
		}

		checkForNull("block_ID",id,"name",nName,"container_name",container,"source",source);

		return new CreateGisBlock(id,nName,container,isVisible,source,N,E,S,W,hasSatNav,showTeam);

	}

	private Block readBlockCreatePicture(XmlPullParser parser) throws IOException, XmlPullParserException {
		o.addText("Parsing block: block_create_picture...");
		String id=null,nName=null,container=null,source=null,scale=null;
		boolean isVisible=true;

		parser.require(XmlPullParser.START_TAG, null,"block_create_picture");
		Log.d("vortex","In block create picture!!");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "name":
					nName = readText("name", parser);
					break;
				case "container_name":
					container = readText("container_name", parser);
					break;
				case "source":
					source = readText("source", parser);
					break;
				case "scale":
					scale = readText("scale", parser);
					break;
				case "is_displayed":
					isVisible = readText("is_displayed", parser).equals("true");
					break;
				default:
					skip(name, parser);
					break;
			}

		}
		checkForNull("block_ID",id,"container_name",container,"source",source);
		return new CreateImageBlock(id,nName,container,source,scale,isVisible);

	}

	private Block readBlockCreateCategoryDataSource(XmlPullParser parser) throws IOException, XmlPullParserException {
		//		o.addText("Parsing block: block_set_value...");
		String id=null,title=null,chart=null,expressions=null;
		String[] categories=null,colorNames=null;
		parser.require(XmlPullParser.START_TAG, null,"block_create_category_data_source");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "title":
					title = readText("title", parser);
					break;
				case "chart_name":
					chart = readText("chart_name", parser);
					break;
				case "categories":
					categories = createStringArray(readText("categories", parser));
					break;
				case "expressions":
					expressions = readText("expressions", parser);
					break;
				case "category_colors":
					colorNames = createStringArray(readText("category_colors", parser));
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id);
		return new CreateCategoryDataSourceBlock(id,title,chart, categories, expressions,colorNames);

	}

	private Block readBlockXYDataSource(XmlPullParser parser) throws IOException, XmlPullParserException {
		String id=null,title=null,chart=null;
		String[] categories=null;
		parser.require(XmlPullParser.START_TAG, null,"block_create_xy_data_source");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "title":
					title = readText("title", parser);
					break;
				case "chart_name":
					chart = readText("chart_name", parser);
					break;
				case "categories":
					categories = createStringArray(readText("categories", parser));
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id);
		return new CreateCategoryDataSourceBlock(id,title,chart, categories, "dummy",null);

	}


	private String[] createStringArray(String args) {
		if (args==null || args.isEmpty())
			return null;
		return args.split(",");
	}

	public String getImageMetaFormat() {
		return imageMetaFormat;
	}

	private enum ChartType {
		round,
		bar,
	}
	private Block readBlockCreateChart(ChartType type,XmlPullParser parser) throws IOException, XmlPullParserException {
		//		o.addText("Parsing block: block_set_value...");
		String id=null,label=null,container=null;
		String textSize=null,margins=null,startAngle=null,mName=null;
		String h = null, w=null;
		int height=-1,width=-1;
		boolean isVisible=true,displayValues=true;
		//parser.require(XmlPullParser.START_TAG, null,"block_create_round_chart");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "name":
					mName = readText("name", parser);
					break;
				case "label":
					label = readText("label", parser);
					break;
				case "container_name":
					container = readText("container_name", parser);
					break;
				case "is_visible":
					isVisible = !readText("is_visible", parser).equals("false");
					break;
				case "text_size":
					textSize = readText("text_size", parser);
					break;
				case "margins":
					margins = readText("margins", parser);
					break;
				case "start_angle":
					startAngle = readText("start_angle", parser);
					break;
				case "height":
					h = readText("height", parser);
					height = (h == null || h.length() == 0) ? -1 : (h.equalsIgnoreCase("fill") ? -2 : Integer.parseInt(h));
					break;
				case "width":
					w = readText("width", parser);
					width = (w == null || w.length() == 0) ? -1 : (w.equalsIgnoreCase("fill") ? -2 : Integer.parseInt(w));
					break;
				case "display_values":
					displayValues = !readText("display_values", parser).equals("false");
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}

		checkForNull("block_ID",id,"name",mName,"label",label,"container",container,
				"text_size",textSize,"margins",margins,"start_angle",startAngle,"height",h,"width",w
		);
		if (type == ChartType.round)
			return new RoundChartBlock(id,mName,label,container,textSize,margins,startAngle,height,width,displayValues,isVisible);
		else
			return new BarChartBlock(id,mName,label,container,textSize,margins,2,height,width,displayValues,isVisible);
	}
	private Block readBlockCreateTextField(XmlPullParser parser) throws IOException, XmlPullParserException {
		//		o.addText("Parsing block: block_set_value...");
		String id=null,label=null,container=null,background=null,horizontalMargin=null,verticalMargin=null,textSize=null;
		boolean isVisible=true;
		parser.require(XmlPullParser.START_TAG, null,"block_create_text_field");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "label":
					label = readText("label", parser);
					break;
				case "bck_color":
					background = readText("bck_color", parser);
					break;
				case "container_name":
					container = readText("container_name", parser);
					break;
				case "is_visible":
					isVisible = !readText("is_visible", parser).equals("false");
					break;
				case "horizontal_margin":
					horizontalMargin = readText("horizontal_margin", parser);
					break;
				case "text_size":
					textSize = readText("text_size", parser);
					break;
				case "vertical_margin":
					verticalMargin = readText("vertical_margin", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id,"label",label,"container",container);
		return new BlockCreateTextField(id,label,background,container,isVisible,textSize,horizontalMargin,verticalMargin);

	}

	private Block readBlockDefineMenuHeader(XmlPullParser parser) throws IOException, XmlPullParserException {
		//		o.addText("Parsing block: block_set_value...");
		String id=null,label=null,textColor=null,bgColor=null;
		parser.require(XmlPullParser.START_TAG, null,"block_define_menu_header");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "label":
					label = readText("label", parser);
					break;
				case "text_color":
					textColor = readText("text_color", parser);
					break;
				case "bck_color":
					bgColor = readText("bck_color", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id,"label",label,"text_color",textColor,"bck_color",bgColor);
		return new MenuHeaderBlock(id,label,textColor,bgColor);

	}

	private Block readBlockDefineMenuEntry(XmlPullParser parser) throws IOException, XmlPullParserException {
		//		o.addText("Parsing block: block_set_value...");
		String id=null,type=null,target=null,textColor=null,bgColor=null;
		parser.require(XmlPullParser.START_TAG, null,"block_define_menu_entry");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "target":
					target = readText("target", parser);
					break;
				case "type":
					type = readText("type", parser);
					break;
				case "text_color":
					textColor = readText("text_color", parser);
					break;
				case "bck_color":
					bgColor = readText("bck_color", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id,"target",target,"type",type);
		return new MenuEntryBlock(id,target,type,bgColor,textColor);

	}
	private Block readBlockSetValue(XmlPullParser parser) throws IOException, XmlPullParserException {
		String id=null,target=null,expression=null;
		String executionBehavior=null;
		parser.require(XmlPullParser.START_TAG, null,"block_set_value");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					o.addText("Parsing block: block_set_value, with id " + id);
					break;
				case "target":
					target = readText("target", parser);
					break;
				case "expression":
					expression = readText("expression", parser);
					break;
				case "execution_behavior":
					executionBehavior = readText("execution_behavior", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id,"target",target,"expression",expression,"execution_behavior",executionBehavior);
		return new SetValueBlock(id,target,expression,executionBehavior);

	}



	private Block readBlockJump(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_jump...");
		String id=null,nextBlockId=null;

		parser.require(XmlPullParser.START_TAG, null,"block_jump");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("next_block_ID")) {
				nextBlockId = readText("next_block_ID",parser);
			}
			else
				skip(name,parser,o);
		}
		checkForNull("block_ID",id,"next_block_id",nextBlockId);
		return new JumpBlock(id,nextBlockId);
	}



	private Block readBlockConditionalContinuation(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_conditional_continuation...");
		List<String> varL=null;
		String id=null,expr=null,elseBlockId=null;
		parser.require(XmlPullParser.START_TAG, null,"block_conditional_continuation");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "expression":
					expr = readText("expression", parser);
					break;
				case "else_block_ID":
					elseBlockId = readText("else_block_ID", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id,"expression",expr,"else_block_ID",elseBlockId);

		return new ConditionalContinuationBlock(id,varL,expr,elseBlockId);
	}



	private Block readBlockAddVariableToListEntry(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_add_variable_to_list_entry...");
		boolean isVisible = true,isDisplayed = false,showHistorical=false;
		String targetList= null,targetField= null,namn=null,format= null,id=null,initialValue=null;
		parser.require(XmlPullParser.START_TAG, null,"block_add_variable_to_list_entry");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "name":
					namn = readText("name", parser);
					break;
				case "target_list":
					targetList = readText("target_list", parser);
					break;
				case "target_field":
					targetField = readText("target_field", parser);
					break;
				case "is_displayed":
					isDisplayed = !readText("is_displayed", parser).equals("false");
					break;
				case "is_visible":
					isVisible = !readText("is_visible", parser).equals("false");
					break;
				case "format":
					format = readText("format", parser);
					break;
				case "show_historical":
					showHistorical = readText("show_historical", parser).equals("true");
					break;
				case "initial_value":
					initialValue = readText("initial_value", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}
		}
		checkForNull("block_ID",id,"name",namn,"target_list",targetList,"target_field",targetField,"format",format);
		return new AddVariableToListEntry(id,namn,
				targetList,targetField, isDisplayed,format,isVisible,showHistorical,initialValue);

	}




	private Block readBlockAddEntryToFieldList(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_add_entry_to_field_list...");

		String target= null,namn= null,label=null,description=null,id=null;
		parser.require(XmlPullParser.START_TAG, null,"block_add_entry_to_field_list");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "name":
					namn = readText("name", parser);
					break;
				case "target":
					target = readText("target", parser);
					break;
				case "label":
					label = readText("label", parser);
					break;
				case "description":
					description = readText("description", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}
		}
		checkForNull("block_ID",id,"name",namn,"target",target,"label",label,"description",description);
		return new AddEntryToFieldListBlock(id,namn,target,label,description);
	}

	private Block readBlockAddVariableToEntryField(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_add_variable_to_entry_field...");
		boolean isVisible = true,isDisplayed=false,showHistorical=false;
		String target= null,namn= null,format= null,id=null,initialValue=null;
		parser.require(XmlPullParser.START_TAG, null,"block_add_variable_to_entry_field");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "name":
					namn = readText("name", parser);
					break;
				case "target":
					target = readText("target", parser);
					break;
				case "is_displayed":
					isDisplayed = !readText("is_displayed", parser).equals("false");
					break;
				case "is_visible":
					isVisible = !readText("is_visible", parser).equals("false");
					break;
				case "format":
					format = readText("format", parser);

					break;
				case "show_historical":
					showHistorical = readText("show_historical", parser).equals("true");
					break;
				case "initial_value":
					initialValue = readText("initial_value", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}
		}
		checkForNull("block_ID",id,"name",namn,"target",target,"format",format);
		return new AddVariableToEntryFieldBlock(id,target,namn,isDisplayed,format,isVisible,showHistorical,initialValue);
	}


	private Block readBlockAddAggregateColumnToTable(XmlPullParser parser) throws IOException, XmlPullParserException {
		o.addText("Parsing block: block_add_aggregate_column_to_table...");
		parser.require(XmlPullParser.START_TAG, null,"block_add_aggregate_column_to_table");
		String id=null,label = null,target=null,expression=null,aggregationFunction=null,format = null,width=null;
		String backgroundColor=null,textColor=null;
		boolean isDisplayed=true;

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("label")) {
				label = readText("label",parser);
			}else if (name.equals("target")) {
				target = readText("target",parser);
			} else if (name.equalsIgnoreCase("expression")) {
				expression = readText("expression",parser);
			} else if (name.equalsIgnoreCase("aggregation_function")) {
				aggregationFunction = readText("aggregation_function",parser);
			} else if (name.equalsIgnoreCase("format")) {
				format = readText("format",parser);
			} else if (name.equalsIgnoreCase("width")) {
				width = readText("width",parser);
			} else if (name.equalsIgnoreCase("bck_color")) {
				backgroundColor = readText("bck_color",parser);
			}else if (name.equalsIgnoreCase("text_color")) {
				textColor = readText("text_color",parser);
			}else if (name.equals("is_displayed")) {
				isDisplayed = readText("is_displayed",parser).trim().equals("true");

			} else {
				Log.e("vortex","Skipped "+name);
				skip(name,parser);
			}
		}

		checkForNull("block_ID",id,"target",target);
		Log.d("mozarella","bck_color set to "+backgroundColor);
		return new BlockAddAggregateColumnToTable(id,label, target,expression,aggregationFunction,format,width,backgroundColor, textColor,isDisplayed);

	}



	private Block readBlockAddColumnsToTable(XmlPullParser parser) throws IOException, XmlPullParserException {
		o.addText("Parsing block: block_add_column_to_table...");
		String id=null,target=null,label=null,type=null,colKey=null,width=null;
		String backgroundColor=null,textColor=null;

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("target")) {
				target = readText("target",parser);
			} else if (name.equals("label")) {
				label = readText("label",parser);

			}else if (name.equalsIgnoreCase("column_key")) {
				colKey = readText("column_key",parser);
			}else if (name.equalsIgnoreCase("type")) {
				type = readText("type",parser);
			}else if (name.equalsIgnoreCase("width")) {
				width = readText("width",parser);
			} else if (name.equalsIgnoreCase("bck_color")) {
				backgroundColor = readText("bck_color",parser);
			}else if (name.equalsIgnoreCase("text_color")) {
				textColor = readText("text_color",parser);
			}
			else {
				Log.e("vortex","Skipped "+name);
				skip(name,parser);
			}
		}

		checkForNull("block_ID",id,"target",target);
		return new BlockAddColumnsToTable(id,target,label,colKey,type,width,backgroundColor,textColor);

	}

	private final Map<String,Block> blockCache = new HashMap<>();

	private Block readBlockCreateListEntriesFromFieldList(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_create_list_entries_from_field_list...");
		String namn=null, type=null,containerId=null,selectionField=null,selectionPattern=null,id=null;
		String labelField=null,descriptionField=null,typeField=null,uriField=null,variatorColumn=null;
		String textColor=null,backgroundColor=null,verticalMargin=null,verticalFormat=null;
		parser.require(XmlPullParser.START_TAG, null,"block_create_list_entries_from_field_list");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					//check if cached object exist.
					if (blockCache.get(id) != null)
						return blockCache.get(id);
					break;
				case "type":
					type = readText("type", parser);
					break;
				case "selection_field":
					selectionField = readText("selection_field", parser);
					break;
				case "selection_pattern":
					selectionPattern = readText("selection_pattern", parser);
					break;
				case "name":
					namn = readText("name", parser);
					break;
				case "container_name":
					containerId = readText("container_name", parser);
					break;
				case "label_field":
					labelField = readText("label_field", parser);
					break;
				case "description_field":
					descriptionField = readText("description_field", parser);
					break;
				case "type_field":
					typeField = readText("type_field", parser);
					break;
				case "variator":
					variatorColumn = readText("variator", parser);
					break;
				case "uri_field":
					uriField = readText("uri_field", parser);
					break;
				case "text_color":
					textColor = readText("text_color", parser);
					break;
				case "bck_color":
					backgroundColor = readText("bck_color", parser);

					break;
				case "vertical_margin":
					verticalMargin = readText("vertical_margin", parser);
					break;
				case "vertical_format":
					verticalFormat = readText("vertical_format", parser);


					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id,"selection_field",selectionField,"name",namn,"container_name",
				containerId,"label_field",labelField,"description_field",descriptionField,"type_field",typeField,
				"uri_field",uriField);
		return new BlockCreateListEntriesFromFieldList(id,namn, type,
				containerId,selectionPattern,selectionField,variatorColumn,textColor,backgroundColor,verticalFormat,verticalMargin);
	}

	private Block readBlockCreateTable(XmlPullParser parser) throws IOException, XmlPullParserException {
		String tableName=null, type=null,containerId=null,id=null,label=null;

		parser.require(XmlPullParser.START_TAG, null,"block_create_table");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "type":
					type = readText("type", parser);
					break;
				case "name":
					tableName = readText("name", parser);
					break;
				case "container_name":
					containerId = readText("container_name", parser);
					break;
				case "label":
					label = readText("label", parser);
					break;
			}
		}
		return new BlockCreateTable(id,type,tableName,containerId,label);
	}

	private Block readBlockCreateTableEntriesFromFieldList(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_create_list_entries_from_field_list...");
		String type=null,selectionField=null,selectionPattern=null,id=null,target=null;
		String keyField = null, labelField=null,descriptionField=null,typeField=null,uriField=null,variatorColumn=null;
		parser.require(XmlPullParser.START_TAG, null,"block_create_table_entries_from_field_list");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "type":
					type = readText("type", parser);
					break;
				case "target":
					target = readText("target", parser);
					break;
				case "selection_field":
					selectionField = readText("selection_field", parser);
					break;
				case "selection_pattern":
					selectionPattern = readText("selection_pattern", parser);
					break;
				case "key_field":
					keyField = readText("key_field", parser);
					break;
				case "label_field":
					labelField = readText("label_field", parser);
					break;
				case "description_field":
					descriptionField = readText("description_field", parser);
					break;
				case "type_field":
					typeField = readText("type_field", parser);
					break;
				case "column_key_name":
					variatorColumn = readText("column_key_name", parser);
					break;
				case "uri_field":
					uriField = readText("uri_field", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id,"selection_field",selectionField,"target",
				target,"label_field",labelField,"description_field",descriptionField,"type_field",typeField,
				"uri_field",uriField);
		return new BlockCreateTableEntriesFromFieldList(id, type,target,
				selectionField, selectionPattern,keyField,labelField,descriptionField,typeField,variatorColumn,uriField);
	}



	private Block readBlockAddVariableToEveryListEntry(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_add_variable_to_every_list_entry...");
		String target=null,variableSuffix=null,format=null,id=null,initialValue=null;
		boolean displayOut=false,isVisible=true,showHistorical=false;

		parser.require(XmlPullParser.START_TAG, null,"block_add_variable_to_every_list_entry");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "target":
					target = readText("target", parser);
					break;
				case "format":
					format = readText("format", parser);
					break;
				case "name":
					variableSuffix = readText("name", parser);
					break;
				case "is_displayed":
					displayOut = readText("is_displayed", parser).trim().equals("true");
					break;
				case "is_visible":
					isVisible = readText("is_visible", parser).trim().equals("true");
					break;
				case "show_historical":
					showHistorical = readText("show_historical", parser).equals("true");
					break;
				case "initial_value":
					initialValue = readText("initial_value", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id,"target",target,"format",format,"name",variableSuffix);
		return new 	AddVariableToEveryListEntryBlock(id,target,
				variableSuffix, displayOut,format,isVisible,showHistorical,initialValue);
	}

	private Block readBlockAddVariableToTable(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_add_variable_to_every_list_entry...");
		String target=null,variableSuffix=null,format=null,id=null,initialValue=null;
		boolean displayOut=false,isVisible=true,showHistorical=false;

		parser.require(XmlPullParser.START_TAG, null,"block_add_variable_to_table");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "target":
					target = readText("target", parser);
					break;
				case "format":
					format = readText("format", parser);
					break;
				case "name":
					variableSuffix = readText("name", parser);
					break;
				case "is_displayed":
					displayOut = readText("is_displayed", parser).trim().equals("true");
					break;
				case "is_visible":
					isVisible = readText("is_visible", parser).trim().equals("true");
					break;
				case "show_historical":
					showHistorical = readText("show_historical", parser).equals("true");
					break;
				case "initial_value":
					initialValue = readText("initial_value", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id,"target",target,"format",format,"name",variableSuffix);
		return new 	BlockAddVariableToTable(id,target,
				variableSuffix, displayOut,format,isVisible,showHistorical,initialValue);
	}



	private DisplayValueBlock readBlockCreateDisplayField(XmlPullParser parser)throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_create_display_field...");
		boolean isVisible = true;
		String namn=null, formula = null, label=null,containerId=null,format = null,id=null,textColor=null,bgColor=null,verticalMargin=null,verticalFormat=null;
		Unit unit=null;
		parser.require(XmlPullParser.START_TAG, null,"block_create_display_field");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "label":
					label = readText("label", parser);
					break;
				case "expression":
					formula = readText("expression", parser);
					break;
				case "name":
					namn = readText("name", parser);
					break;
				case "container_name":
					containerId = readText("container_name", parser);
					break;
				case "unit":
					unit = Tools.convertToUnit(readText("unit", parser));
					break;
				case "is_visible":
					isVisible = !readText("is_visible", parser).equals("false");
					break;
				case "format":
					format = readText("format", parser);
					break;
				case "text_color":
					textColor = readText("text_color", parser);
					break;
				case "bck_color":
					bgColor = readText("bck_color", parser);

					break;
				case "vertical_margin":
					verticalMargin = readText("vertical_margin", parser);
					break;
				case "vertical_format":
					verticalFormat = readText("vertical_format", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("label",label,"expression",formula,"block_ID",id,"name",namn,"container_name",containerId,"format",format);
		return new DisplayValueBlock(id,namn, label,unit,
				formula,containerId,isVisible,format,textColor,bgColor,verticalFormat,verticalMargin);
	}


	private CreateSliderEntryFieldBlock readBlockCreateEntryFieldSlider(XmlPullParser parser)throws IOException, XmlPullParserException {
		o.addText("Parsing block: block_create_slider_entry_field...");
		Log.d("benoz","Parsing block: block_create_slider_entry_field...");
		boolean isVisible = true,showHistorical = false,autoOpenSpinner=true;
		String namn=null,containerId=null,postLabel="",id=null,initialValue=null,label=null,variableName=null,group=null;
		String textColor = "Black";
		int min=0,max=100;
		String backgroundColor = null,verticalMargin=null,verticalFormat=null;
		Unit unit = Unit.nd;
		parser.require(XmlPullParser.START_TAG, null,"block_create_slider_entry_field");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "name":
					namn = readText("name", parser);
					break;
				case "label":
					label = readText("label", parser);
					break;
				case "container_name":
					containerId = readText("container_name", parser);
					break;
				case "is_visible":
					isVisible = !readText("is_visible", parser).equals("false");

					break;
				case "max": {
					String mS = readText("max", parser);
					if (mS != null)
						try {
							max = Integer.parseInt(mS);
						} catch (NumberFormatException e) {
						}
					break;
				}
				case "min": {
					String mS = readText("min", parser);
					if (mS != null)
						try {
							min = Integer.parseInt(mS);
						} catch (NumberFormatException e) {
						}
					break;
				}
				case "show_historical":
					showHistorical = readText("show_historical", parser).equals("true");
					break;
				case "initial_value":
					initialValue = readText("initial_value", parser);
					break;
				case "auto_open_spinner":
					autoOpenSpinner = readText("auto_open_spinner", parser).equals("true");
					break;
				case "variable_name":
					variableName = readText("variable_name", parser);
					break;
				case "slider_group_name":
					group = readText("slider_group_name", parser);
					break;
				case "text_color":
					textColor = readText("text_color", parser);
					break;
				case "bck_color":
					backgroundColor = readText("bck_color", parser);

					break;
				case "vertical_margin":
					verticalMargin = readText("vertical_margin", parser);
					break;
				case "vertical_format":
					verticalFormat = readText("vertical_format", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}
		}
		checkForNull("block_ID",id,"name",namn,"container_name",containerId,"variableName",variableName);
		return new CreateSliderEntryFieldBlock(id,namn, containerId,isVisible,showHistorical,initialValue,label,variableName,group,textColor,backgroundColor,min,max,verticalFormat,verticalMargin);
	}

	private CreateEntryFieldBlock readBlockCreateEntryField(XmlPullParser parser)throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_create_entry_field...");
		boolean isVisible = true,showHistorical = false,autoOpenSpinner=true;
		String namn=null,containerId=null,postLabel="",format=null,id=null,initialValue=null,label=null;
		Unit unit = Unit.nd;
		String textColor = "black";
		String backgroundColor = null,verticalMargin=null,verticalFormat=null;
		parser.require(XmlPullParser.START_TAG, null,"block_create_entry_field");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "name":
					namn = readText("name", parser);
					break;
				case "label":
					label = readText("label", parser);
					break;
				case "container_name":
					containerId = readText("container_name", parser);
					break;
				case "is_visible":
					isVisible = !readText("is_visible", parser).equals("false");
					break;
				case "format":
					format = readText("format", parser);
					break;
				case "show_historical":
					showHistorical = readText("show_historical", parser).equals("true");
					break;
				case "initial_value":
					initialValue = readText("initial_value", parser);
					break;
				case "auto_open_spinner":
					autoOpenSpinner = readText("auto_open_spinner", parser).equals("true");
					break;
				case "text_color":
					textColor = readText("text_color", parser);
					break;
				case "bck_color":
					backgroundColor = readText("bck_color", parser);
					break;
				case "vertical_margin":
					verticalMargin = readText("vertical_margin", parser);
					break;
				case "vertical_format":
					verticalFormat = readText("vertical_format", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}
		}
		checkForNull("block_ID",id,"name",namn,"container_name",containerId,"format",format);
		return new CreateEntryFieldBlock(id,namn, containerId,isVisible,format,showHistorical,initialValue,label,autoOpenSpinner,textColor,backgroundColor,verticalFormat,verticalMargin);
	}

	/**
	 * Creates a Block for adding a sorting function on Target List. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */

	private void dummyWarning(String block,XmlPullParser parser) {
		o.addText("Parsing block: "+block);
		;
		o.addCriticalText("This type of block is not supported");
	}


	/**
	 * Creates a Block for adding a sorting function on Target List. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private CreateSortWidgetBlock readBlockCreateSorting(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_create_sort_widget...");
		String id=null,namn=null,containerName=null,type=null,target=null,selectionField=null,displayField=null,selectionPattern=null;
		boolean isVisible = true;
		parser.require(XmlPullParser.START_TAG, null,"block_create_sort_widget");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "container_name":
					containerName = readText("container_name", parser);

					break;
				case "name":
					namn = readText("name", parser);

					break;
				case "type":
					type = readText("type", parser);

					break;
				case "target":
					target = readText("target", parser);

					break;
				case "is_visible":
					isVisible = !readText("is_visible", parser).equals("false");

					break;
				case "selection_field":
					selectionField = readText("selection_field", parser);

					break;
				case "display_field":
					displayField = readText("display_field", parser);

					break;
				case "selection_pattern":
					selectionPattern = readText("selection_pattern", parser);

					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("BLOCK_ID",id,"CONTAINER_NAME: ",containerName,"DISPLAY_FIELD",displayField,
				"TARGET"+target,"TYPE",type,
				"NAME",namn);
		return new CreateSortWidgetBlock(id,namn,type, containerName,target,selectionField,displayField,selectionPattern,isVisible);
	}

	/**
	 * Creates a Block for displaying the number of selected entries currently in a list. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */

	private AddSumOrCountBlock readBlockAddSelectionOrSum(XmlPullParser parser,boolean isCount) throws IOException, XmlPullParserException {
		String containerName=null,label=null,postLabel = null,filter=null,target=null,result=null,format=null,id=null;
		WF_Not_ClickableField_SumAndCountOfVariables.Type type;
		String bgColor=null,textColor=null,verticalMargin=null,verticalFormat=null;
		boolean isVisible = true;

		if (isCount)
			type = WF_Not_ClickableField_SumAndCountOfVariables.Type.count;
		else
			type = WF_Not_ClickableField_SumAndCountOfVariables.Type.sum;

		if (isCount) {
			//o.addText("Parsing block: block_add_number_of_selections_display...");
			parser.require(XmlPullParser.START_TAG, null,"block_add_number_of_selections_display");
		}
		else {
			//o.addText("Parsing block: block_add_sum_of_selected_variables_display...");
			parser.require(XmlPullParser.START_TAG, null,"block_add_sum_of_selected_variables_display");
		}
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();

			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "container_name":
					containerName = readText("container_name", parser);
					break;
				case "label":
					label = readText("label", parser);
					break;
				case "filter":
					filter = readText("filter", parser);
					break;
				case "target":
					target = readText("target", parser);
					break;
				case "unit":
					postLabel = readText("unit", parser);
					break;
				case "result":
					result = readText("result", parser);
					break;
				case "is_visible":
					isVisible = !readText("is_visible", parser).equals("false");
					break;
				case "format":
					format = readText("format", parser);

					break;
				case "text_color":
					textColor = readText("text_color", parser);
					break;
				case "bck_color":
					bgColor = readText("bck_color", parser);
					break;
				case "vertical_margin":
					verticalMargin = readText("vertical_margin", parser);
					break;
				case "vertical_format":
					verticalFormat = readText("vertical_format", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id,"label",label,"container_name",containerName,"filter",filter,
				"target",target,"result",result,"format",format,"unit",postLabel);
		return new AddSumOrCountBlock(id,containerName,label,postLabel,filter,target,type,result,isVisible,format,textColor,bgColor,verticalFormat,verticalMargin);
	}


	/**
	 *  Creates a CreateListEntriesBlock. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 * @throws EvalException
	 */	
	/*
		private CreateListEntriesBlock readBlockCreateListEntries(XmlPullParser parser) throws IOException, XmlPullParserException, EvalException {
			o.addText("Parsing block: block_create_list_entries...");

			;
			o.addCriticalText("block_create_list_entries is no longer supported. Use block_create_list_entries_from_field_list instead");
			return null;
		}
	 */
	/*
			boolean isVisible = true;
			String type=null,fileName="",containerName=null,namn=null,selectionField=null,selectionPattern=null,filter=null;
			parser.require(XmlPullParser.START_TAG, null,"block_create_list_entries");
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}	
				String name= parser.getName();
				//If a unique varname tag found, instantiate a new XML_variable. 
				if (name.equals("file_name")) {
					fileName = readText("file_name",parser);
					o.addText("FILE_NAME: "+fileName);
				} else if (name.equals("container_name")) {
					containerName = readText("container_name",parser);
					o.addText("CONTAINER_NAME: "+containerName);
				} else if (name.equals("name")) {
					namn = readText("name",parser);
					o.addText("NAME: "+namn);
				} else if (name.equals("type")) {
					type = readText("type",parser);
					o.addText("TYPE: "+type);
				}  else if (name.equals("selection_pattern")) {
					selectionPattern = readText("selection_pattern",parser);
					o.addText("SELECTION_PATTERN: "+selectionPattern);
				} else if (name.equals("selection_field")) {
					selectionField = readText("selection_field",parser);
					o.addText("SELECTION_FIELD: "+selectionField);
				} else if (name.equals("filter")) {
					filter = readText("filter",parser);
					o.addText("FILTER: "+filter);
				} else if (name.equals("is_visible")) {
					isVisible = !readText("is_visible",parser).equals("false");
					o.addText("IS_VISIBLE: "+isVisible);	
				} 			
				else
					skip(name,parser,o);


			}

			return new CreateListEntriesBlock(type,fileName,containerName,namn,selectionField,selectionPattern,filter,isVisible);
		}
	 */


	/**
	 *  Creates a Buttonblock. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	//For now just create dummy.
	private ButtonBlock readBlockButton(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_button...");
		String label=null,onClick=null,myname=null,containerName=null,
				target=null,type=null,id=null,statusVariable=null,exportMethod=null,
				exportFormat=null,buttonContext=null,statusContext=null,filter=null;
		boolean isVisible = true, enabled=false,requestSync=false;
		parser.require(XmlPullParser.START_TAG, null,"block_button");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "on_click":
					onClick = readText("on_click", parser);
					break;
				case "type":
					type = readText("type", parser);
					break;
				case "name":
					myname = readText("name", parser);
					break;
				case "label":
					label = readText("label", parser);
					break;
				case "container_name":
					containerName = readText("container_name", parser);
					break;
				case "export_format":
					exportFormat = readText("export_format", parser);
					break;
				case "target":
					target = readText("target", parser);
					break;
				case "enabled":
					enabled = readText("enabled", parser).equals("true");
					break;
				case "is_visible":
					isVisible = readText("is_visible", parser).equals("true");
					break;
				case "context":
					buttonContext = readText("context", parser);
					break;
				case "export_method":
					exportMethod = readText("export_method", parser);
					break;
				case "button_context":
					buttonContext = readText("button_context", parser);
					break;
				case "status_context":
					statusContext = readText("status_context", parser);
					break;
				case "status_variable":
					statusVariable = readText("status_variable", parser);
					break;
				case "request_sync":
					requestSync = readText("request_sync", parser).equals("true");
					break;
				case "picture_name_pattern":
					filter = readText("picture_name_pattern", parser);
					if (filter != null && filter.isEmpty())
						filter = null;
					break;
				default:
					skip(name, parser, o);
					break;
			}
		}
		checkForNull("block_ID",id,"type",type,"name",myname,"label",label,"container_name",
				containerName,"target",target);

		return new ButtonBlock(id,label,onClick,myname,containerName,target,type,statusVariable,isVisible,exportFormat,exportMethod,enabled,buttonContext,statusContext,requestSync,filter);
	}






	/**
	 *  Creates a Startblock. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	//Block Start contains the name of the worklfow and the Arguments.
	private StartBlock readBlockStart(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_start...");
		String workflowName=null; String[] args =null;
		String context=null;
		String id=null;
		parser.require(XmlPullParser.START_TAG, null,"block_start");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "workflowname":
					workflowName = readSymbol("workflowname", parser);
					;
					o.addGreenText("Reading workflow: [" + workflowName + "]");
					Log.d("NILS", "Reading workflow: " + workflowName);

					break;
				case "inputvar":
					args = readArray("inputvar", parser);
					o.addText("input variables: ");
					for (String arg : args) o.addYellowText(arg + ",");
					break;
				case "context":
					context = readText("context", parser);

					break;
				default:
					skip(name, parser, o);
					break;
			}
		}
		if (workflowName == null)  {
			;
			o.addCriticalText("Error reading startblock. Workflowname missing");
			throw new XmlPullParserException("Parameter missing");
		}
		checkForNull("block_ID",id,"workflowname",workflowName);

		return new StartBlock(id,args,workflowName,context);
	}

	/*


		/**
	 * Creates a LayoutBlock. LayoutBlocks are used to set the direction of the layout 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private LayoutBlock readBlockLayout(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_layout...");
		String layout=null,align=null,label=null,id=null;
		parser.require(XmlPullParser.START_TAG, null,"block_layout");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "layout":
					layout = readText("layout", parser);

					break;
				case "align":
					align = readText("align", parser);

					break;
				case "label":
					label = readText("label", parser);


					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id,"layout",layout,"align",align,"label",label);
		return new LayoutBlock(id,label,layout,align);
	}

	/**
	 * Creates a PageDefinitionBlock. Pages are the templates for a given page. Defines layout etc. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private PageDefineBlock readPageDefineBlock(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_define_page...");
		String pageType=null,label="",id=null,gpsPriority="low";
		boolean hasGPS=false,goBackAllowed=true,hasSatNav = false;
		parser.require(XmlPullParser.START_TAG, null,"block_define_page");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "type":
					pageType = readText("type", parser);
					if (pageType != null && pageType.length()==0)
						pageType = null;
					break;
				case "gps_on":
					hasGPS = readText("gps_on", parser).equals("true");
					break;
				case "gps_priority":
					gpsPriority = readText("gps_priority", parser);
					break;
				case "allow_OS_page_back":
					goBackAllowed = readText("allow_OS_page_back", parser).equals("true");
					break;
				case "label":
					label = readText("label", parser);
					o.addText("Parsing workflow " + label);
					break;
				default:
					skip(name, parser, o);
					break;
			}
		}
		checkForNull("block_ID",id,"type",pageType,"label",label);
		return new PageDefineBlock(id,"root", pageType,label,hasGPS,gpsPriority,goBackAllowed);
	}


	/**
	 * Creates a PageDefinitionBlock. Pages are the templates for a given page. Defines layout etc. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private ContainerDefineBlock readContainerDefineBlock(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_define_container...");
		String containerType=null,containerName="",id=null;
		parser.require(XmlPullParser.START_TAG, null,"block_define_container");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "name":
					containerName = readText("name", parser);
					break;
				case "type":
					containerType = readText("type", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}
		}
		checkForNull("block_ID",id,"name",containerName,"container_type",containerType);
		return new ContainerDefineBlock(id,containerName, containerType);
	}
	/**
	 * Creates a AddRuleBlock. Adds a rule to a variable or object. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private RuleBlock readBlockAddRule(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addText("Parsing block: block_add_rule...");
		String target=null, condition=null, myScope = null,action=null, errorMsg=null,myname=null,id=null;
		parser.require(XmlPullParser.START_TAG, null,"block_add_rule");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			switch (name) {
				case "block_ID":
					id = readText("block_ID", parser);
					break;
				case "target":
					target = readText("target", parser);
					break;
				case "condition":
					condition = readText("condition", parser);
					break;
				case "action":
					action = readText("action", parser);
					break;
				case "errorMsg":
					errorMsg = readText("errorMsg", parser);
					break;
				case "name":
					myname = readText("name", parser);
					break;
				case "scope":
					myScope = readText("scope", parser);
					break;
				default:
					skip(name, parser, o);
					break;
			}

		}
		checkForNull("block_ID",id,"name",myname,"target",target,"condition",condition,"action",action,"errorMsg",errorMsg);
		return new RuleBlock(id,myname,target,condition,action,errorMsg,myScope);
	}

	private void checkForNull(String...pars) {

		boolean lbl = false;
		String lab=null;
		for (String par:pars) {
			lbl = !lbl;
			if (lbl) {
				lab = par;
				continue;
			} else if (par==null) {
				;
				o.addYellowText("Parameter "+lab+" was NULL");

			}
		}
	}



	// Read symbol from tag.
	private String readSymbol(String tag,XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null,tag);
		String text = readText(parser);
		parser.require(XmlPullParser.END_TAG, null,tag);
		//Check that it does not start with a number.
		if (text!=null) {
			if (text.length()>0 && Character.isDigit(text.charAt(0))) {
				;
				o.addCriticalText("XML: EXCEPTION - Symbol started with integer");
				throw new XmlPullParserException("Symbol cannot start with integer");
			}
		} else {
			;
			o.addCriticalText("XML: EXCEPTION - Symbol was NULL");
			throw new XmlPullParserException("Symbol cannot be null");
		}
		return text;
	}


	public List<Workflow> getEssence() {
		return (List<Workflow>)essence;
	}

	@Override
	protected Type getEssenceType() {
		// This creates a specific "type token" that tells Gson to expect
		// a JSON array and to convert its elements into Workflow objects.
		return new TypeToken<List<Workflow>>(){}.getType();
	}

	@Override
	public void setEssence() {
		essence=bundle;
	}
//	@Override
//	public LoadResult thawSynchronously() {
//		List<Workflow> workflows = new ArrayList<>();
//		Path dir = Paths.get(frozenPath).getParent();
//		String glob = Paths.get(frozenPath).getFileName().toString() + "*";
//
//		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
//			Log.d("blaha","TFound "+stream.toString());
//			int i=1;
//			for (Path entry : stream) {
//				Log.d("blaha","Thawing "+entry.toString());
//				if (i==9)
//					Log.d("anchir","Found 9 files");
//				String rawJsonContent = getFileContentAsString(entry.toString());
//				Workflow wf = Tools.readObjectFromFileAsJson(entry.toString(), Workflow.class);
//				if (wf != null) {
//					workflows.add(wf);
//				}
//				i++;
//			}
//		} catch (IOException e) {
//			// Handle exceptions, e.g., directory not found, read errors
//			e.printStackTrace();
//			return new LoadResult(this, ErrorCode.IOError);
//		}
//		if (workflows.size() > 0) {
//			essence = workflows;
//			return new LoadResult(this, ErrorCode.thawed);
//		} else {
//			Log.d("eep","no workflows");
//			return new LoadResult(this, ErrorCode.thawFailed);
//		}
//
//	}
//	@Override
//	public void freeze(int _c) {
//		setEssence();
//		int i=0;
//		for(Workflow wf : bundle) {
//			i++;
//			Tools.writeObjectToFileAsJson(wf, frozenPath + i);
//			Log.d("blah", "Wrote workflow " + frozenPath + i + " to disk " + wf.getName() + " blocks " + wfp(wf.getBlocks()));
//		}
//	}
//
//	private String wfp(List<Block> blocks) {
//		StringBuilder sb = new StringBuilder();
//		sb.append("[");
//		for (Block b:blocks) {
//			sb.append(b.getClass());
//		}
//		sb.append("]");
//		return sb.toString();
//	}


}
