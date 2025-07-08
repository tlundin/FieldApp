package com.teraim.fieldapp.dynamic.templates;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Cell;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table_Row_Recycle;
import com.teraim.fieldapp.ui.TableBodyAdapter;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import android.content.Intent;
import android.net.Uri;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import com.google.android.flexbox.FlexboxLayout;
public class PageWithTable extends Executor implements TableBodyAdapter.ScrollSyncManager,OnFilterSelectedListener {

    private HorizontalScrollView stickyHeaderScrollView;
    private LinearLayout stickyHeaderLinearLayout;
    private RecyclerView tableRecyclerView;

    private final List<Listable> masterTableRowsDataList = new ArrayList<>();
    private final List<Listable> displayedTableRowsDataList = new ArrayList<>();
    private TableBodyAdapter tableBodyAdapter;

    private boolean isProgrammaticScroll = false;

    private VariableConfiguration al;
    private GlobalState gs;
    private final Map<String,Set<String>> varIdMap = new HashMap<>();
    private String myVariator;
    private Map<String, Map<String, String>> allInstances;

    private ViewGroup my_root;
    private LinearLayout filterRow1, filterRow2;
    private FlexboxLayout filterRowTop;
    private CardView infoPanelCard;
    private TextView infoPanelText;

    private ImageButton infoPanelCloseButton;
    private String currentlyDisplayedInfoLabel = null;

    private transient WF_Context myContext;
    private LayoutInflater inflater;
    private int rowNumber = 0;

    private WF_Table_Row_Recycle headerRow;
    public static final String HEADER_ROW_ID = "TableHeader";
    private boolean tableTypeSimple=false;

    private List<ColumnDefinition> columnDefinitions = new ArrayList<>();
    private int currentlyFocusedColumn = -1;

    // Filter states
    private String activeAlphabeticalFilter = null; // Changed to String to hold one active filter
    private boolean filterHideRowsWithNoEntries = false;
    private List<ToggleButton> topFilterGroup = new ArrayList<>();
    private List<ToggleButton> alphabetFilterButtons = new ArrayList<>(); // To manage alpha buttons as a group
    private String activeTopFilter;

    private List<String> topButtonLabels;
    private List<String> availableFilterLabels;
    private List<String> availableColumnFilterLabels;

    List<String> masterFamilyFilters = new ArrayList<>();
    private PersistenceHelper globalPh;

    public static class ColumnDefinition {
        public String label, key, type, backgroundColor, textColor;
        public int width;
        public float textSizeSp = 16f;
        public boolean isVisible = true, isAggregate = false;
        ColumnDefinition(String label, String key, String type, int width, String backgroundColor, String textColor, boolean isAggregate) {
            this.label = label; this.key = key; this.type = type; this.width = width; this.backgroundColor = backgroundColor; this.textColor = textColor; this.isAggregate = isAggregate;
        }
    }

    public List<ColumnDefinition> getColumnDefinitions() {
        return columnDefinitions;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.inflater = inflater;
        View view = inflater.inflate(R.layout.template_page_with_table, container, false);

        my_root = view.findViewById(R.id.myRoot);
        filterRowTop = view.findViewById(R.id.filterRowTop);
        filterRow1 = view.findViewById(R.id.filterRow1);
        filterRow2 = view.findViewById(R.id.filterRow2);

        gs = GlobalState.getInstance();
        if (gs != null) {
            al = gs.getVariableConfiguration();
            globalPh = gs.getGlobalPreferences();
        } else {
            Log.e("PageWithTable", "GlobalState is null in onCreateView!");
        }

        topButtonLabels = new ArrayList<>();
        // Initialize topFilterGroup here if it's not done in constructor
        if (topFilterGroup == null) {
            topFilterGroup = new ArrayList<>();
        }
        // Initialize alphabetFilterButtons here if it's not done in constructor
        if (alphabetFilterButtons == null) {
            alphabetFilterButtons = new ArrayList<>();
        }


        if (globalPh != null) {
            String savedFiltersString = globalPh.get(PersistenceHelper.FILTER_BUTTON_LIST);
            if (savedFiltersString != null && !savedFiltersString.isEmpty()) {
                // Split the string back into a list
                String[] filtersArray = savedFiltersString.split(",");
                for (String filter : filtersArray) {
                    topButtonLabels.add(filter.trim());
                }
                Log.d("PageWithTable", "Loaded filters: " + savedFiltersString);
            } else {
                // If no saved filters, explicitly add "Växter", "Lavar", and "Mossor"
                topButtonLabels.add("P_Växter");
                topButtonLabels.add("P_Lavar");
                topButtonLabels.add("P_Mossor");
                Log.d("PageWithTable", "No saved filters, using default dynamic filters: Växter, Lavar, Mossor");
            }
        } else {
            topButtonLabels.add("P_Växter");
            Log.e("PageWithTable", "globalPh is null during initialization, using default dynamic filters.");
        }

        myContext = getCurrentContext();
        if (myContext == null) Log.e("PageWithTable", "myContext is null in onCreateView!");

        if (myContext != null && this.inflater != null) {
            View headerRowView = this.inflater.inflate(R.layout.header_table_row, null);
            headerRow = new WF_Table_Row_Recycle(this, HEADER_ROW_ID, headerRowView, myContext, true);
        } else {
            Log.e("PageWithTable", "Cannot initialize headerRow.");
        }
        if (myContext != null) myContext.addContainers(getContainers());

        availableFilterLabels = new ArrayList<>();
        // Assuming al.getTable() and al.getTable().getColumn("P_Familj") exist
        if (al != null && al.getTable() != null) {
            List<String> filterList = al.getTable().getColumn("P_Familj");
            if (filterList != null) {
                Set<String> uniqueEntries = filterList.stream()
                        .filter(entry -> !entry.isEmpty())
                        .collect(Collectors.toSet());
                System.out.println("Unique entries (using Streams): " + uniqueEntries);
                for (String entry : uniqueEntries) {
                    // Ensure "Växter", "Mossor", "Lavar" are not added to availableFilterLabels
                    // if they are considered "fixed" or "special" family filters.
                    // For now, assuming P_Familj might return these and they should be managed.
                    availableFilterLabels.add(entry);
                    masterFamilyFilters.add(entry);
                }
            }
        }


        availableColumnFilterLabels = new ArrayList<>();
        availableColumnFilterLabels.add("Växter");
        if (al != null) {
            List<String> allColumnHeaders = al.getTable().getColumnHeaders();
            if (allColumnHeaders != null) {
                for (String header : allColumnHeaders) {
                    if (header != null && header.startsWith("P_") && !header.equals("P_Familj")) {
                        availableColumnFilterLabels.add(header);
                    }
                }
            }
        }
        Log.d("PageWithTable", "Collected Column Filters (onCreateView): " + availableColumnFilterLabels.toString());


        setupFilterButtons(); // This will now use the class-level availableColumnFilterLabels

        stickyHeaderScrollView = view.findViewById(R.id.sticky_header_scroll_view);
        stickyHeaderLinearLayout = view.findViewById(R.id.sticky_header_linear_layout);
        tableRecyclerView = view.findViewById(R.id.table_recycler_view);

        infoPanelCard = view.findViewById(R.id.info_panel_card);
        infoPanelText = view.findViewById(R.id.info_panel_text);
        infoPanelCloseButton = view.findViewById(R.id.info_panel_close_button);
        if (infoPanelCloseButton != null) {
            infoPanelCloseButton.setOnClickListener(v -> hideInfoPanel());
        }

        setupRecyclerView();
        setupHeaderScrollListener();
        refreshHeaderUI();
        return view;
    }

    // --- The setupFilterButtons method will be updated next ---


    // Add this new method to your Fragment/Activity (if not already present)

    private void showFilterSelectionDialog() {
        // The collection of availableColumnFilterLabels is now done in onCreateView,
        // so we just use the class-level field here.
        // Log the collected column filters for now
        Log.d("PageWithTable", "Collected Column Filters: " + availableColumnFilterLabels.toString());

        FilterSelectionDialogFragment dialog = FilterSelectionDialogFragment.newInstance(
                new ArrayList<>(topButtonLabels), // Pass a copy of current top filters
                new ArrayList<>(availableFilterLabels), // Pass a copy of current available filters
                new ArrayList<>(availableColumnFilterLabels) // Pass a copy of the collected column filters
        );
        // If this class is a Fragment, use setTargetFragment
        dialog.setTargetFragment(this, 0);
        // If this class is an Activity, you might use an interface directly
        // dialog.setSelectionListener(this); // Assuming your Activity implements OnFilterSelectedListener
        dialog.show(getParentFragmentManager(), "FilterSelectionDialog");
    }
    @Override
    public void onFiltersApplied(List<String> newTopFilters, List<String> newAvailableFiltersFromDialog) { // newAvailableFiltersFromDialog is currently empty
        // 1. Update the topButtonLabels with the new selection from the dialog
        this.topButtonLabels.clear();
        this.topButtonLabels.addAll(newTopFilters);

        // 2. Reconstruct availableFilterLabels based on the updated topButtonLabels
        // This is crucial because the dialog no longer manages initialAvailableFamilyFilters directly.
        this.availableFilterLabels.clear(); // Clear the old list

        // Use a Set for efficient lookup of filters currently in the top row
        Set<String> currentTopFiltersSet = new HashSet<>(this.topButtonLabels);

        // Re-populate availableFilterLabels with family filters that are NOT in currentTopFilters
        // You need a master list of all *possible* family filters here.
        // Example: If "Mossor" and "Lavar" are the only other family filters besides "Växter"
        // and "Växter" is now handled as a column filter in availableColumnFilterLabels.
        // Let's assume you have a master list of all potential family filters, e.g.,
        // private List<String> allMasterFamilyFilters = Arrays.asList("Mossor", "Lavar");
        // (You would initialize this list in your PageWithTable's constructor or onCreateView)

        // For this example, let's just add "Mossor" and "Lavar" if they are not in top filters.
        // In a real app, you'd iterate over your true master list of family filters.



        for (String familyFilter : masterFamilyFilters) {
            if (!currentTopFiltersSet.contains(familyFilter)) {
                this.availableFilterLabels.add(familyFilter);
            }
        }

        // Note: The availableColumnFilterLabels list (class field) remains unchanged as it's a master list.

        // 3. Save the updated topButtonLabels to persistence
        if (globalPh != null) {
            String filtersString = String.join(",", this.topButtonLabels);
            globalPh.put(PersistenceHelper.FILTER_BUTTON_LIST, filtersString);
            Log.d("PageWithTable", "Saved filters: " + filtersString);
        } else {
            Log.e("PageWithTable", "globalPh is null, cannot save filters.");
        }

        // 4. Re-setup the filter buttons to reflect changes in the UI
        setupFilterButtons();

        // 5. Explicitly apply filters after UI is re-setup
        applyRowFilters();
    }

    private ToggleButton createFilterToggleButton(Context context, String text, boolean useWeight) {
        ToggleButton tb = new ToggleButton(context);
        tb.setTextOn(text); tb.setTextOff(text); tb.setText(text); tb.setChecked(false);
        tb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        LinearLayout.LayoutParams params;
        if (useWeight) {
            params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        } else {
            params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
       // int marginInDp = 2;
        //int marginInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, marginInDp, getResources().getDisplayMetrics());
        //params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
        tb.setLayoutParams(params);
        //tb.setPadding(8, 4, 8, 4);
        return tb;
    }
    private void setupFilterButtons() {
        // Ensure these are cast correctly when initialized in your Fragment/Activity's onCreateView or onCreate
        // Example:
        // filterRowTop = view.findViewById(R.id.filterRowTop); // Cast as FlexboxLayout
        // filterRow1 = view.findViewById(R.id.filterRow1);     // Cast as LinearLayout
        // filterRow2 = view.findViewById(R.id.filterRow2);     // Cast as LinearLayout

        if (filterRowTop == null || filterRow1 == null || filterRow2 == null || getContext() == null) {
            Log.e("PageWithTable", "A filter row or Context is null, cannot create filter buttons.");
            return;
        }
        filterRowTop.removeAllViews(); filterRow1.removeAllViews(); filterRow2.removeAllViews();
        // activeAlphabeticalFilter = null; // This is now managed by its own listener
        topFilterGroup.clear(); alphabetFilterButtons.clear();

        // --- Add the permanent "ALL" filter button first ---
        ToggleButton allButton = createFilterToggleButton(getContext(), "ALL", false);
        allButton.setOnClickListener(v -> {
            // De-select all other top filter buttons
            for (ToggleButton otherButton : topFilterGroup) {
                if (otherButton != allButton) { // Ensure "ALL" button doesn't uncheck itself
                    otherButton.setChecked(false);
                }
            }
            allButton.setChecked(true); // Ensure "ALL" is checked
            activeTopFilter = "ALL"; // Set the active filter to "ALL"

            // --- FIX: Removed lines that incorrectly reset alphabetical and RR filters ---
            // activeAlphabeticalFilter = null;
            // for (ToggleButton alphaButton : alphabetFilterButtons) { alphaButton.setChecked(false); }
            // filterHideRowsWithNoEntries = false;

            applyRowFilters(); // Apply the filter
        });
        // Add to filterRowTop directly, NOT to topFilterGroup (as it's not managed by dialog)
        FlexboxLayout.LayoutParams allButtonParams = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        allButtonParams.setMargins(margin, margin, margin, margin);
        filterRowTop.addView(allButton, allButtonParams);


        // Top Row: Växter, Mossor, Lavar (now dynamically from topButtonLabels list)
        // These are the *dynamic* top filters, managed by the dialog
        ToggleButton firstDynamicTopToggleButton = null; // To store a reference for height calculation

        for (String label : topButtonLabels) { // Use the List<String> here
            // Skip "ALL" if it somehow made it into topButtonLabels (it shouldn't from dialog)
            if ("ALL".equals(label)) continue;

            ToggleButton tb = createFilterToggleButton(getContext(), label, false);
            tb.setOnClickListener(v -> {
                // De-select all other top filter buttons, including "ALL"
                for (ToggleButton otherButton : topFilterGroup) {
                    otherButton.setChecked(otherButton == tb);
                }
                allButton.setChecked(false); // De-select "ALL" when another filter is chosen
                tb.setChecked(true);
                activeTopFilter = label;

                // When a dynamic top filter changes, it should NOT affect alphabetical filters.
                // Removed: for (ToggleButton alphaButton : alphabetFilterButtons) { alphaButton.setChecked(false); }
                // Removed: activeAlphabeticalFilter = null;

                applyRowFilters();
            });
            FlexboxLayout.LayoutParams tbParams = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
            );
            tbParams.setMargins(margin, margin, margin, margin);
            filterRowTop.addView(tb, tbParams);
            topFilterGroup.add(tb); // Add to topFilterGroup for management by this class

            if (firstDynamicTopToggleButton == null) {
                firstDynamicTopToggleButton = tb;
            }
        }

        // Set initial active filter and checked state
        if (activeTopFilter == null) { // If no filter was previously active
            allButton.setChecked(true);
            activeTopFilter = "ALL";
        } else {
            // Check if the previously active filter is "ALL"
            if ("ALL".equals(activeTopFilter)) {
                allButton.setChecked(true);
            } else {
                // Check the corresponding dynamic button if it exists
                boolean foundActive = false;
                for (ToggleButton tb : topFilterGroup) {
                    if (tb.getText().toString().equals(activeTopFilter)) {
                        tb.setChecked(true);
                        foundActive = true;
                        break;
                    }
                }
                // Fallback: If the previously active filter is no longer in topButtonLabels,
                // default to "ALL" being selected.
                if (!foundActive) {
                    allButton.setChecked(true);
                    activeTopFilter = "ALL";
                }
            }
        }


        // --- Create and add the spacer ---
        View spacer = new View(getContext());
        FlexboxLayout.LayoutParams spacerParams = new FlexboxLayout.LayoutParams(
                0,
                0
        );
        spacerParams.setFlexGrow(1.0f);
        filterRowTop.addView(spacer, spacerParams);


        // --- Create the round button (FAB) programmatically and add it ---
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        final FloatingActionButton roundButton = (FloatingActionButton) inflater.inflate(R.layout.round_button_layout, null, false);

        roundButton.setOnClickListener(v -> {
            showFilterSelectionDialog();
        });

        filterRowTop.addView(roundButton);


        // Alphabetical Filters (4 groups)
        String[] alphaButtonLabels = {"A-F", "G-L", "M-R", "S-Ö"};
        for (int i = 0; i < alphaButtonLabels.length; i++) {
            final String label = alphaButtonLabels[i];
            ToggleButton tb = createFilterToggleButton(getContext(), label, true);
            tb.setOnClickListener(v -> {
                boolean isNowChecked = tb.isChecked();
                // Ensure only one alphabetical filter is active at a time
                for (ToggleButton otherButton : alphabetFilterButtons) {
                    if (otherButton != tb) {
                        otherButton.setChecked(false);
                    }
                }
                if (isNowChecked) {
                    activeAlphabeticalFilter = label;
                } else {
                    activeAlphabeticalFilter = null; // Deactivate if already checked and clicked again
                }
                // --- FIX: Do NOT affect top filters when alphabetical filter is clicked ---
                // Removed: allButton.setChecked(false);
                // Removed: for (ToggleButton topTb : topFilterGroup) { topTb.setChecked(false); }
                // Removed: activeTopFilter = null;

                applyRowFilters();
            });
            LinearLayout.LayoutParams tbParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            tbParams.setMargins(margin, margin, margin, margin);
            filterRow1.addView(tb, tbParams);
            alphabetFilterButtons.add(tb);
        }

        // RR button on the second row
        ToggleButton rrButton = createFilterToggleButton(getContext(), " RR ", false);
        rrButton.setOnClickListener(v -> {
            filterHideRowsWithNoEntries = rrButton.isChecked();
            // --- FIX: Do NOT affect alphabetical or top filters when RR button is clicked ---
            // Removed: allButton.setChecked(false);
            // Removed: for (ToggleButton topTb : topFilterGroup) { topTb.setChecked(false); }
            // Removed: activeTopFilter = null;
            applyRowFilters();
        });
        LinearLayout.LayoutParams rrParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rrParams.setMargins(margin, margin, margin, margin);
        filterRow2.addView(rrButton, rrParams);
    }


    public void onRowHeaderClicked(List<String> row) {
        if (al == null || infoPanelCard == null || infoPanelText == null) return;

        String rowLabel = al.getEntryLabel(row);
        String extraInfo = al.getDescription(row);
        String url = al.getUrl(row);

        // If the info panel is already visible for the same row, hide it.
        if (infoPanelCard.getVisibility() == View.VISIBLE && rowLabel != null && rowLabel.equals(currentlyDisplayedInfoLabel)) {
            hideInfoPanel();
            return;
        }

        // If no extra info and no URL, hide panel and show toast.
        if ((extraInfo == null || extraInfo.isEmpty()) && (url == null || url.isEmpty())) {
            hideInfoPanel();
            Toast.makeText(getContext(), "No extra information available.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Prepare the bold row header prefix as a Spanned object ---
        Spanned boldRowLabelSpanned = null;
        if (rowLabel != null && !rowLabel.isEmpty()) {
            // Use Html.fromHtml to parse the bold tag directly
            boldRowLabelSpanned = Html.fromHtml("<b>" + rowLabel + "</b>. ", Html.FROM_HTML_MODE_LEGACY);
        }

        // Get the main info text
        String infoText = (extraInfo != null) ? extraInfo : "";

        // Handle URL case
        if (url != null && !url.isEmpty()) {
            final String linkPlaceholder = " info";

            // Use SpannableStringBuilder to build the final text
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            int currentLength = 0; // Track the length of content added so far

            // Append bold prefix if it exists
            if (boldRowLabelSpanned != null) {
                ssb.append(boldRowLabelSpanned);
                currentLength += boldRowLabelSpanned.length();
            }

            // Append the main info text
            ssb.append(infoText);
            currentLength += infoText.length();

            // The linkPlaceholder will be appended after infoText.
            // Its start index is the current total length.
            final int linkStartIndex = currentLength;
            ssb.append(linkPlaceholder);
            // Its end index is the total length of the SpannableStringBuilder after appending.
            final int linkEndIndex = ssb.length();

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        getContext().startActivity(browserIntent);
                    } catch (Exception e) {
                        Log.e("PageWithTable", "Could not open URL: " + url, e);
                        Toast.makeText(getContext(), "Could not open link", Toast.LENGTH_SHORT).show();
                    }
                }
            };

            // Apply the span using the calculated indices
            // No need for the explicit bounds check here, as ssb.setSpan handles it with the correct indices.
            ssb.setSpan(clickableSpan, linkStartIndex, linkEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            infoPanelText.setText(ssb);
            infoPanelText.setMovementMethod(LinkMovementMethod.getInstance()); // Essential for clickable spans
        } else {
            // No URL, just display bold prefix followed by infoText
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            if (boldRowLabelSpanned != null) {
                ssb.append(boldRowLabelSpanned);
            }
            ssb.append(infoText);
            infoPanelText.setText(ssb);
            infoPanelText.setMovementMethod(null); // Remove movement method if no link
        }

        infoPanelCard.setVisibility(View.VISIBLE);
        currentlyDisplayedInfoLabel = rowLabel;
    }

    private void hideInfoPanel() {
        if (infoPanelCard != null) {
            infoPanelCard.setVisibility(View.GONE);
        }
        currentlyDisplayedInfoLabel = null;
    }

    public void applyRowFilters() {
        //Log.d("PageWithTable", "Applying row filters. Top: " + activeTopFilter + ", Alpha: " + activeAlphabeticalFilter + ", HideEmpty: " + filterHideRowsWithNoEntries);
        displayedTableRowsDataList.clear();

        // Create a Set for efficient lookup of column filters
        // This assumes availableColumnFilterLabels is a class-level field in PageWithTable
        Set<String> columnFiltersSet = new HashSet<>(availableColumnFilterLabels);

        for (Listable item : masterTableRowsDataList) {
            if (item instanceof WF_Table_Row_Recycle) {
                WF_Table_Row_Recycle rowWidget = (WF_Table_Row_Recycle) item;
                if (rowWidget.getRowData() == null) continue;

                boolean shouldDisplay = true; // Assume true by default for this row

                // Apply Top Row Filters (ALL, Växter, Family filters, or Column filters)
                if (al != null && activeTopFilter != null) {
                    if ("ALL".equals(activeTopFilter)) {
                        // If "ALL" is active, this means no top filter should be applied.
                        // 'shouldDisplay' remains true from the top filter's perspective.
                        // We simply proceed to the next filter checks (alphabetical, hide empty).
                    } else if ("Växter".equals(activeTopFilter)) {
                        // If "Växter" is active, display rows if there's no data in P_Mossor OR P_Lavar
                        String mossorValue = al.getColumn("P_Mossor", rowWidget.getRowData());
                        String lavarValue = al.getColumn("P_Lavar", rowWidget.getRowData());

                        boolean hasMossorData = (mossorValue != null && !mossorValue.isEmpty());
                        boolean hasLavarData = (lavarValue != null && !lavarValue.isEmpty());

                        // If it has data in Mossor OR Lavar, then it should NOT be displayed for "Växter" filter
                        if (hasMossorData || hasLavarData) {
                            shouldDisplay = false;
                        }
                    } else if (columnFiltersSet.contains(activeTopFilter)) {
                        // This is a COLUMN FILTER (e.g., "P_Fork")
                        // Check if the row has ANY non-null/non-empty entry in the column matching activeTopFilter
                        String columnValue = al.getColumn(activeTopFilter, rowWidget.getRowData());
                        if (columnValue == null || columnValue.isEmpty()) {
                            shouldDisplay = false; // Exclude if the column for this filter is empty
                        }
                    } else {
                        // This is a FAMILY FILTER (e.g., "Mossor", "Lavar", or any other custom family filter)
                        // Filter based on the "P_Familj" column containing the activeTopFilter value
                        String relevantColumnValue = al.getColumn("P_Familj", rowWidget.getRowData());
                        if (relevantColumnValue == null || !relevantColumnValue.contains(activeTopFilter)) {
                            shouldDisplay = false;
                        }
                    }
                }

                if (!shouldDisplay) {
                    continue; // Skip to the next item if the top filter already excluded it
                }

                // Alphabetical Filter (remains unchanged)
                String rowLabel = rowWidget.getLabel();
                if (activeAlphabeticalFilter != null) {
                    if (rowLabel == null || rowLabel.isEmpty()) {
                        shouldDisplay = false;
                    } else {
                        char firstChar = Character.toUpperCase(rowLabel.charAt(0));
                        boolean matchesAlpha = false;
                        if (activeAlphabeticalFilter.length() >= 3 && activeAlphabeticalFilter.charAt(1) == '-') { // e.g., "A-F"
                            char startRange = activeAlphabeticalFilter.charAt(0);
                            char endRange = activeAlphabeticalFilter.charAt(2);
                            if (firstChar >= startRange && firstChar <= endRange) {
                                matchesAlpha = true;
                            }
                        } else if (activeAlphabeticalFilter.equals("S-Ö")) {
                            // For S-Ö, check 'S' or Swedish special characters
                            if (firstChar >= 'S' || firstChar == 'Å' || firstChar == 'Ä' || firstChar == 'Ö') {
                                matchesAlpha = true;
                            }
                        }
                        if (!matchesAlpha) shouldDisplay = false;
                    }
                }

                if (!shouldDisplay) {
                    continue; // Skip to the next item if the alphabetical filter already excluded it
                }

                // Hide Rows With No Entries Filter (remains unchanged)
                if (filterHideRowsWithNoEntries) {
                    if (!rowWidget.hasAnyCheckedNonAggregateSimpleCell(columnDefinitions)) {
                        shouldDisplay = false;
                    }
                }

                // If all filters pass, add the row to the displayed list
                if (shouldDisplay) {
                    displayedTableRowsDataList.add(rowWidget);
                }
            }
        }

        Collections.sort(displayedTableRowsDataList, Comparator.comparing(o -> ((o != null && o.getLabel() != null) ? o.getLabel() : ""), String.CASE_INSENSITIVE_ORDER));
        if (tableBodyAdapter != null) {
            //Log.d("PageWithTable", "Notifying adapter. Displayed rows: " + displayedTableRowsDataList.size() + "/" + masterTableRowsDataList.size());
            tableBodyAdapter.notifyDataSetChanged();
        }
        refreshColumnVisibilitiesInUI();
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (gs == null) { gs = GlobalState.getInstance(); if (gs != null) al = gs.getVariableConfiguration(); }
        if (al == null && gs != null) al = gs.getVariableConfiguration();
        if (gs == null) Log.e("PageWithTable", "GlobalState is null in onViewCreated!");
        if (al == null) Log.e("PageWithTable", "VariableConfiguration (al) is null in onViewCreated!");
        if (wf != null) { Log.d("PageWithTable","Executing workflow!!"); run(); }
        else {
            Log.d("PageWithTable","No workflow found in onViewCreated. Loading dummy data.");
            addDummyData();
        }
    }

    @Override
    public void onResume() { super.onResume(); Activity activity = getActivity(); if (activity instanceof Start) ((Start) activity).setTopBarVisibility(false); }
    @Override
    public void onPause() { super.onPause(); Activity activity = getActivity(); if (activity instanceof Start) ((Start) activity).setTopBarVisibility(true); }

    private void setupRecyclerView() {
        tableBodyAdapter = new TableBodyAdapter(requireContext(), displayedTableRowsDataList, Collections.emptyList(), this);
        tableRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        tableRecyclerView.setAdapter(tableBodyAdapter);
    }
    private void setupHeaderScrollListener() {
        if (stickyHeaderScrollView != null) {
            stickyHeaderScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!isProgrammaticScroll) {
                    syncScroll(stickyHeaderScrollView, scrollX);
                }
            });
        }
    }
    private void refreshHeaderUI() {
        if (stickyHeaderLinearLayout == null || headerRow == null || headerRow.getWidget() == null) return;
        stickyHeaderLinearLayout.removeAllViews();
        View headerWidgetView = headerRow.getWidget();
        if (headerWidgetView.getParent() instanceof ViewGroup) {
            ((ViewGroup) headerWidgetView.getParent()).removeView(headerWidgetView);
        }
        stickyHeaderLinearLayout.addView(headerWidgetView);
    }

    public void onColumnHeaderClicked(int columnIndex) {
        Log.d("PageWithTable", "Column header clicked: " + columnIndex + ". Current focused: " + currentlyFocusedColumn);
        if (columnIndex < 0 || columnIndex >= columnDefinitions.size()) {
            Log.e("PageWithTable", "Invalid columnIndex: " + columnIndex + " for " + columnDefinitions.size() + " columns.");
            return;
        }

        ColumnDefinition clickedColDef = columnDefinitions.get(columnIndex);

        if (currentlyFocusedColumn == columnIndex) {
            for (ColumnDefinition def : columnDefinitions) {
                def.isVisible = true;
            }
            currentlyFocusedColumn = -1;
            Log.d("PageWithTable", "Showing all columns.");
        } else {
            Log.d("PageWithTable", "Processing click on column: " + clickedColDef.label + " (Index: " + columnIndex + ", IsAggregate: " + clickedColDef.isAggregate + ")");
            if (clickedColDef.isAggregate) {
                for (int i = 0; i < columnDefinitions.size(); i++) {
                    ColumnDefinition def = columnDefinitions.get(i);
                    def.isVisible = def.isAggregate || (i == columnIndex);
                    Log.d("PageWithTable", "  Loop Agg Click - Col " + i + " ("+def.label+"): isAgg=" + def.isAggregate + ", isClicked=" + (i == columnIndex) + " => isVisible=" + def.isVisible);
                }
                currentlyFocusedColumn = columnIndex;
                Log.d("PageWithTable", "Focusing aggregate column: " + columnIndex + ", other aggregates also visible.");
            } else { // Clicked a non-aggregate column to focus
                for (int i = 0; i < columnDefinitions.size(); i++) {
                    ColumnDefinition def = columnDefinitions.get(i);
                    def.isVisible = def.isAggregate || (i == columnIndex);
                    Log.d("PageWithTable", "  Loop Non-Agg Click - Col " + i + " ("+def.label+"): isAgg=" + def.isAggregate + ", isClicked=" + (i == columnIndex) + " => isVisible=" + def.isVisible);
                }
                currentlyFocusedColumn = columnIndex;
                Log.d("PageWithTable", "Focusing non-aggregate column: " + columnIndex + ", aggregates also visible.");
            }
        }

        if (headerRow != null) {
            headerRow.updateHeaderAppearance(columnDefinitions, currentlyFocusedColumn);
        }
        refreshColumnVisibilitiesInUI();
        if (stickyHeaderLinearLayout != null) stickyHeaderLinearLayout.requestLayout();
        if (stickyHeaderScrollView != null) stickyHeaderScrollView.requestLayout();
    }

    private void refreshColumnVisibilitiesInUI() { for (Listable dataRow : displayedTableRowsDataList) if (dataRow instanceof WF_Table_Row_Recycle) ((WF_Table_Row_Recycle)dataRow).updateCellVisibilities(columnDefinitions); }

    private void addColumnInternal(ColumnDefinition colDef, int columnIndex) {
        if (myContext == null || headerRow == null) { Log.e("PageWithTable", "Context or HeaderRow null in addColumnInternal."); return; }
        Map<String, String> colHash = Tools.copyKeyHash(myContext.getKeyHash());
        if (myVariator != null && colDef.key != null) colHash.put(myVariator, colDef.key);

        // Pass colDef.width (which is the fixed width for aggregates, or 0 for weighted normal columns)
        // WF_Table_Row_Recycle.addHeaderCell will use definition.isAggregate to decide layout params.
        headerRow.addHeaderCell(colDef.label, null, null, colDef.width, columnIndex, colDef);

        String cellBgColor = colDef.isAggregate ? colDef.backgroundColor : null;
        String cellTextColor = colDef.isAggregate ? colDef.textColor : null;
        if ("simple".equals(colDef.type)) { cellBgColor = null; cellTextColor = null;}

        for (Listable item : masterTableRowsDataList) {
            if (item instanceof WF_Table_Row_Recycle) {
                // Pass colDef.width. WF_Table_Row_Recycle.addCell will use definition.isAggregate to decide layout params.
                ((WF_Table_Row_Recycle)item).addCell(colDef.label, colDef.key, colHash, colDef.type,
                        colDef.width,
                        cellBgColor, cellTextColor,
                        colDef.isAggregate? WF_Cell.CellType.Aggregate:WF_Cell.CellType.Normal);
            }
        }
    }
    public void addColumns(List<String> labels,
                           List<String> columnKeyL, String type, String widthS,
                           String backgroundColor, String textColor) {
        Log.d("PageWithTable", "addColumns called. Type: " + type);
        columnDefinitions.clear();
        if (headerRow != null) headerRow.clearCells();
        for (Listable item : masterTableRowsDataList) if (item instanceof WF_Table_Row_Recycle) ((WF_Table_Row_Recycle)item).clearCells();

        if (labels != null && columnKeyL != null && labels.size() == columnKeyL.size()) {
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i) != null ? labels.get(i) : "";
                String key = columnKeyL.get(i);
                // For normal columns that should be weighted, pass width 0.
                // The actual width from widthS is now disregarded for normal columns.
                int parsedWidth = 0; // Default for weighted columns
                // If you had a way to distinguish if widthS was meant for a fixed width normal column, logic would go here.
                // For now, all non-aggregate columns added via addColumns will be weighted.
                ColumnDefinition colDef = new ColumnDefinition(label, key, type, parsedWidth, backgroundColor, textColor, false);
                columnDefinitions.add(colDef);
                addColumnInternal(colDef, i);
            }
        } else { Log.w("PageWithTable", "Labels/keys null or mismatch in addColumns."); }
        if (type!=null && type.equals("simple")) tableTypeSimple=true;
        currentlyFocusedColumn = -1;
        if (headerRow != null) headerRow.updateHeaderAppearance(columnDefinitions, currentlyFocusedColumn);
        applyRowFilters();
        refreshHeaderUI();
    }
    public void addVariableToEveryCell(String variableSuffix,
                                       boolean displayOut, String format, boolean isVisible,
                                       boolean showHistorical, String initialValue) {
        if (o == null && gs != null) { o = gs.getLogger(); }
        if (o == null) { Log.e("PageWithTable", "'o' (logger/output) is not defined in addVariableToEveryCell."); }

        for (Listable wft : masterTableRowsDataList) {
            Set<String> varIds = varIdMap.get(wft.getLabel());
            String varGrId=null;
            if (varIds==null) { Log.e("vortex","No varIds for "+wft.getLabel()); continue; }
            else { for (String varGr:varIds) { if (varGr.endsWith(variableSuffix)) { varGrId = varGr; break; } } }
            if (varGrId==null) { Log.e("vortex","no var for suffix: "+variableSuffix +" for row "+wft.getLabel()); if (o != null) { o.addText(""); o.addCriticalText("Could not add var with suffix: "+variableSuffix); } continue; }
            if (tableTypeSimple && gs != null && gs.getVariableConfiguration() != null) {
                List<String> rowDef = gs.getVariableConfiguration().getCompleteVariableDefinition(varGrId);
                if (rowDef != null) {
                    Variable.DataType type = gs.getVariableConfiguration().getnumType(rowDef);
                    if (type != Variable.DataType.bool) {
                        Log.e("vortex", "Non-boolean var in simple column.");
                        if (o != null) o.addCriticalText("Var [" + variableSuffix+ "] not Boolean.");
                        return;
                    }
                }
            }
            Map<String, String> valueMap = (allInstances != null) ? allInstances.get(varGrId) : null;
            int dataCellIndex = 0; // Index for accessing cells in wft.getCells() which only contains data cells
            for (int colDefIndex = 0; colDefIndex < columnDefinitions.size(); colDefIndex++) {
                ColumnDefinition colDef = columnDefinitions.get(colDefIndex);
                if (colDef.isAggregate) { continue; } // Skip aggregate columns for this variable assignment
                List<WF_Cell> cells = ((WF_Table_Row_Recycle) wft).getCells();
                if (cells != null && dataCellIndex < cells.size()) {
                    WF_Cell cell = cells.get(dataCellIndex);
                    String colKey = colDef.key;
                    String prefetchValue = (valueMap!=null) ? valueMap.get(colKey) : null;
                    cell.addVariable(varGrId, displayOut, format, isVisible, showHistorical,prefetchValue);
                } else {
                    Log.w("PageWithTable", "Data cell index OOB or cells null in addVarToCell. Row: "+wft.getLabel()+", DataCellIdx: "+dataCellIndex+", ColDefIdx: "+colDefIndex);
                    break;
                }
                dataCellIndex++;
            }
        }
        applyRowFilters();
    }
    public void setTableName(String name) {
        if (headerRow != null) {
            TextView headerTV = headerRow.getWidget().findViewById(R.id.headerT);
            headerTV.setText(name);
        }
    }

    public enum AggregateFunction { /* ... */ AND, OR, COUNT, SUM, MIN, MAX, aggF, AVG }
    private class AggregateColumn implements EventListener {

        AggregateColumn(String label, Expressor.EvalExpr expressionE, String format, AggregateFunction aggregationFunction, boolean isLogical,List<Listable>rows) {
            myCells=new ArrayList<View>();
            myRows = rows;
            this.expressionE=expressionE;
            this.format=format;
            myContext.registerEventListener(this , Event.EventType.onSave);
            aggF = aggregationFunction;
            this.label=label;
            this.isLogical=isLogical;
        }

        final boolean isLogical;
        final Expressor.EvalExpr expressionE;
        final List<View>myCells;
        final List<Listable>myRows;
        final String format;
        final AggregateFunction aggF;
        final String label;

        public List<View> getMyCells() {
            return myCells;
        }

        void addCell(View aggView) {
            myCells.add(aggView);
        }



        @Override
        public void onEvent(Event e) {
            if (e.getType()==Event.EventType.onSave) {
                Log.d("vortex","caught onSave in aggregate_column!");
                if (myCells!=null) {
                    //loop over mycells (or over rows...doesnt matter. Equal number)
                    TextView tv=null; CheckBox cb = null;
                    for(int i=0;i<myCells.size();i++) {
                        if (!isLogical)
                            tv = (TextView)myCells.get(i);
                        else
                            cb = (CheckBox)myCells.get(i);
                        WF_Table_Row_Recycle row = (WF_Table_Row_Recycle) myRows.get(i);
                        //if (aggregationFunction.equals(AgAND)
                        Set<Variable> vars;

                        boolean completeResB = true;
                        Integer completeRes = 0;
                        if (aggF== AggregateFunction.MIN || aggF== AggregateFunction.MAX)
                            completeRes=null;

                        boolean done=false;
                        //Aggregate over all cells in a row.
                        for (WF_Cell cell:row.getCells()) {
                            vars=cell.getAssociatedVariables();
                            //Log.d("vortex","cell class: "+cell.getClass().getName()+" cell type "+cell.getType());
                            if (cell.getType().equals(WF_Cell.CellType.Aggregate)) {
                                //Log.d("vortex", "skipping aggregate");
                                continue;
                            }
                            //Evaluate expression with given variables as context.
                            //Log.d("vortex","Cell has these variables: ");
                            //for (Variable v:vars)
                            //Log.d("vortex",v.getId());
                            if (isLogical) {
                                Boolean result = Expressor.analyzeBooleanExpression(expressionE, vars);

                                switch (aggF) {
                                    case AND:
                                        if (result ==null || !result) {
                                            completeResB=false;
                                            done=true;
                                        }
                                        break;
                                    case OR:
                                        if (result!=null && result) {
                                            completeResB=true;
                                            done= true;
                                        } else
                                            completeResB=false;
                                        break;
                                }
                            }
                            else {
                                String result = Expressor.analyze(expressionE, vars);
                                if (!Tools.isNumeric(result)) {
                                    //Log.e("vortex", "couldnt use " + result + " for " + aggF + ". Not numeric");
                                    continue;
                                }
                                //Numeric result.
                                int res = Integer.parseInt(result);
                                Log.e("vortex", "got numeric "+res);
                                switch (aggF) {

                                    case SUM:
                                    case AVG:
                                        completeRes+=res;
                                        break;
                                    case COUNT:
                                        completeRes++;
                                        break;
                                    case MIN:
                                        if (completeRes==null || completeRes>res)
                                            completeRes=res;
                                        break;
                                    case MAX:
                                        if (completeRes==null || completeRes<res)
                                            completeRes=res;
                                        break;
                                }
                            }
                            if (done) {
                                //Log.d("vortex","I am done..exiting");
                                break;
                            }
                        }
                        //Here we are done for row.
                        if (isLogical) {
                            cb.setChecked(completeResB);
                        } else {
                            if (completeRes==null) {
                                Log.e("vortex","no result..completeRes is null");
                                tv.setText("");
                            } else {
                                if (aggF== AggregateFunction.AVG) {
                                    int size = row.getCells().size();
                                    completeRes=completeRes/size;
                                }
                                tv.setText(completeRes.toString());
                            }
                        }
                    }
                }
            }
        }

        @Override
        public String getName() {
            return "Aggregate_column";
        }

    }

    public void addAggregateColumn(String label, Expressor.EvalExpr expressionE,
                                   String aggregationFunction, String format, String widthStr,
                                   boolean isDisplayed, String backgroundColor, String textColor) {
        Log.d("PageWithTable", "addAggregateColumn - Label: " + label);
        if (label == null) label = "";
        String colKey = "agg_" + label.replaceAll("\\s+", "_").toLowerCase();
        String type = "aggregate";
        int parsedWidth = 200; // Default fixed width for aggregate columns
        if (widthStr != null && !widthStr.isEmpty()) {
            try {
                parsedWidth = Integer.parseInt(widthStr.replaceAll("[^0-9]", ""));
                if (parsedWidth <= 0) parsedWidth = 200;
            } catch (NumberFormatException e) { Log.w("PageWithTable", "Could not parse width for aggregate: " + widthStr); }
        }

        ColumnDefinition colDef = new ColumnDefinition(label, colKey, type, parsedWidth, backgroundColor, textColor, true);
        colDef.isVisible = true;
        if (!isDisplayed) colDef.isVisible = false;
        columnDefinitions.add(colDef);
        addColumnInternal(colDef, columnDefinitions.size() - 1);

        if (headerRow != null) headerRow.updateHeaderAppearance(columnDefinitions, currentlyFocusedColumn);
        applyRowFilters();
        refreshHeaderUI();

        AggregateFunction aggF;
        try {
            if (aggregationFunction == null) throw new IllegalArgumentException("Aggregation function is null");
            aggF = AggregateFunction.valueOf(aggregationFunction.toUpperCase());
        } catch (Exception e) {
            if (o != null) {o.addText(""); o.addCriticalText("Agg func '"+aggregationFunction+"' invalid.");}
            else {Log.e("PageWithTable", "Agg func '"+aggregationFunction+"' invalid and logger 'o' is null.");}
            return;
        }
        boolean isLogical = (aggF== AggregateFunction.AND || aggF == AggregateFunction.OR);
        AggregateColumn aggColInstance = new AggregateColumn(label, expressionE, format, aggF, isLogical, masterTableRowsDataList);

        for (Listable item : masterTableRowsDataList) {
            if (item instanceof WF_Table_Row_Recycle) {
                WF_Table_Row_Recycle wft = (WF_Table_Row_Recycle) item;
                View aggCellView;
                // Use colDef.width (which is parsedWidth) for aggregate cells
                if (!isLogical) {
                    TextView tv = wft.addAggregateTextCell(backgroundColor,textColor); // Colors applied for aggregate
                    tv.setLayoutParams(new TableRow.LayoutParams(colDef.width, TableRow.LayoutParams.WRAP_CONTENT));
                    aggCellView = tv;
                } else {
                    CheckBox cb = wft.addAggregateLogicalCell(backgroundColor,textColor); // Colors applied for aggregate
                    TableRow.LayoutParams params = new TableRow.LayoutParams(colDef.width, TableRow.LayoutParams.WRAP_CONTENT);
                    params.gravity = android.view.Gravity.CENTER;
                    cb.setLayoutParams(params);
                    aggCellView = cb;
                }
                aggColInstance.addCell(aggCellView);
                if (aggCellView!=null) {
                    aggCellView.setVisibility(colDef.isVisible ? View.VISIBLE : View.GONE);
                }
            }
        }
        if (myContext != null) {
            myContext.registerEvent(new WF_Event_OnSave("PageWithTable_AggColAdded_" + label));
        }
    }

    public void addText(List<String> rowData) {
        if (rowData == null) { Log.w("PageWithTable", "addText null data"); return; }
        if (inflater == null || myContext == null) { Log.e("PageWithTable", "inflater or myContext null in addText"); return; }

        WF_Table_Row_Recycle rowWidget = new WF_Table_Row_Recycle(this, (rowNumber++) + "", inflater.inflate(R.layout.table_row, null), myContext, true);
        rowWidget.addEntryField(rowData);
        Map<String, String> baseColHash = Tools.copyKeyHash(myContext.getKeyHash());
        int dataCellsAddedToRowWidget = 0;

        for (int i = 0; i < columnDefinitions.size(); i++) {
            ColumnDefinition colDef = columnDefinitions.get(i);
            if (colDef.isAggregate) continue;
            Map<String, String> colHash = new HashMap<>(baseColHash);
            if (myVariator != null && colDef.key != null) colHash.put(myVariator, colDef.key);

            // For normal data cells, pass colDef.width (which is 0 for weighted)
            // and null for colors to let XML/theme handle it.
            rowWidget.addCell(colDef.label, colDef.key, colHash, colDef.type,
                    colDef.width, // Pass the width (0 for weighted, specific for fixed if any non-agg fixed cols existed)
                    null, null, WF_Cell.CellType.Normal);
            List<WF_Cell> cellsInRowWidget = rowWidget.getCells();
            if (cellsInRowWidget != null && dataCellsAddedToRowWidget < cellsInRowWidget.size()) {
                WF_Cell cell = cellsInRowWidget.get(dataCellsAddedToRowWidget);
                if (cell != null && cell.getWidget() != null) {
                    cell.getWidget().setVisibility(colDef.isVisible ? View.VISIBLE : View.GONE);
                }
            }
            dataCellsAddedToRowWidget++;
        }
        if (myContext != null && myContext.getEventListeners(Event.EventType.onSave) != null) {
            for (Object listener : myContext.getEventListeners(Event.EventType.onSave)) {
                if (listener instanceof AggregateColumn) {
                    AggregateColumn aggCol = (AggregateColumn) listener;
                    ColumnDefinition aggColDef = null;
                    for(ColumnDefinition cd : columnDefinitions) if (cd.isAggregate && cd.label.equals(aggCol.label)) { aggColDef = cd; break; }
                    if (aggColDef != null) {
                        View aggCellView;
                        // Use aggColDef.width for aggregate cells in data rows
                        if (!aggCol.isLogical) {
                            TextView tv = rowWidget.addAggregateTextCell(aggColDef.backgroundColor, aggColDef.textColor);
                            tv.setLayoutParams(new TableRow.LayoutParams(aggColDef.width, TableRow.LayoutParams.WRAP_CONTENT));
                            aggCellView = tv;
                        } else {
                            CheckBox cb = rowWidget.addAggregateLogicalCell(aggColDef.backgroundColor, aggColDef.textColor);
                            TableRow.LayoutParams params = new TableRow.LayoutParams(aggColDef.width, TableRow.LayoutParams.WRAP_CONTENT);
                            params.gravity = android.view.Gravity.CENTER; cb.setLayoutParams(params); aggCellView = cb;
                        }
                        aggCol.addCell(aggCellView);
                        if (aggCellView!=null) aggCellView.setVisibility(aggColDef.isVisible ? View.VISIBLE : View.GONE);
                    }
                }
            }
        }
        masterTableRowsDataList.add(rowWidget);
        applyRowFilters();
    }
    public void addRows(List<List<String>> rows,String variatorColumn, String selectionPattern) {
        if (gs == null || al == null) { Log.e("PageWithTable", "gs or al not initialized!"); return; }
        if (myContext == null) { myContext = getCurrentContext(); if (myContext == null) { Log.e("PageWithTable", "myContext still null!"); return; } }
        this.myVariator = variatorColumn;
        if (gs.getDb() != null && myContext.getKeyHash() != null) {
            this.allInstances = gs.getDb().preFetchValues(myContext.getKeyHash(), selectionPattern, myVariator);
        } else { this.allInstances = new HashMap<>(); }
        Map<String,List<String>> uRows = new HashMap<>();
        for (List<String> row : rows) {
            String key = al.getEntryLabel(row);
            if (uRows.get(key) == null) uRows.put(key, row);
            Set<String> s = varIdMap.get(key);
            if (s == null) { s = new HashSet<>(); varIdMap.put(key, s); }
            s.add(al.getVarName(row));
        }
        for (String rowKey : uRows.keySet()) {
            addText(uRows.get(rowKey));
        }
    }

    @Override
    public void syncScroll(HorizontalScrollView source, int scrollX) {
        if (isProgrammaticScroll) return;
        isProgrammaticScroll = true;
        if (stickyHeaderScrollView != null && source != stickyHeaderScrollView) {
            stickyHeaderScrollView.scrollTo(scrollX, 0);
        }
        if (tableRecyclerView != null) {
            for (int i = 0; i < tableRecyclerView.getChildCount(); i++) {
                View child = tableRecyclerView.getChildAt(i);
                RecyclerView.ViewHolder rawViewHolder = tableRecyclerView.getChildViewHolder(child);
                if (rawViewHolder instanceof TableBodyAdapter.RowViewHolder) {
                    TableBodyAdapter.RowViewHolder viewHolder = (TableBodyAdapter.RowViewHolder) rawViewHolder;
                    if (viewHolder.rowScrollView != source) {
                        viewHolder.rowScrollView.scrollTo(scrollX, 0);
                    }
                }
            }
        }
        isProgrammaticScroll = false;
    }

    @Override
    public void addScrollListener(HorizontalScrollView scrollView) {
        if (scrollView != null) {
            scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!isProgrammaticScroll && v == scrollView) {
                    syncScroll(scrollView, scrollX);
                }
            });
        }
    }

    @Override
    public void removeScrollListener(HorizontalScrollView scrollView) {
        if (scrollView != null) {
            scrollView.setOnScrollChangeListener(null);
        }
    }

    private void addDummyData() {
        List<String> initialLabels = new ArrayList<>();
        initialLabels.add("Apple");
        initialLabels.add("Banana");
        initialLabels.add("Cherry (Simple)");
        initialLabels.add("Date");

        List<String> initialKeys = new ArrayList<>();
        initialKeys.add("keyA");
        initialKeys.add("keyB");
        initialKeys.add("keySimpleC");
        initialKeys.add("keyD");

        addColumns(Arrays.asList(initialLabels.get(0), initialLabels.get(1)),
                Arrays.asList(initialKeys.get(0), initialKeys.get(1)),
                "text", "180", "#E0E0E0", "#000000");
        addColumns(Collections.singletonList(initialLabels.get(2)),
                Collections.singletonList(initialKeys.get(2)),
                "simple", "120", null, null); // A simple column
        addColumns(Collections.singletonList(initialLabels.get(3)),
                Collections.singletonList(initialKeys.get(3)),
                "text", "150", "#E0E0E0", "#000000");


        addAggregateColumn("Agg Sum", null, "SUM", "#,##0", "100", true, "#D1C4E9", "#4A148C");

        List<List<String>> dummyRowsToAdd = new ArrayList<>();
        dummyRowsToAdd.add(Arrays.asList("Apple Pie", "10", "true", "Fruit A", "50"));
        dummyRowsToAdd.add(Arrays.asList("Banana Bread", "20", "false", "Fruit B", "30"));
        dummyRowsToAdd.add(Arrays.asList("Cherry Tart", "30", "true", "Fruit C", "20"));
        dummyRowsToAdd.add(Arrays.asList("Date Squares", "40", "false", "Fruit D", "60"));
        dummyRowsToAdd.add(Arrays.asList("Elderberry Jam", "50", "true", "Fruit E", "10"));
        dummyRowsToAdd.add(Arrays.asList("Fig Newton", "60", "true", "Fruit F", "90"));

        for(List<String> row : dummyRowsToAdd) {
            addText(row); // addText now adds to master and calls applyRowFilters
        }
        // applyRowFilters(); // Initial filter application after all data is added
    }

    @Override
    protected List<WF_Container> getContainers() {
        ArrayList<WF_Container> ret = new ArrayList<WF_Container>();
        if (my_root != null) {
            WF_Container root = new WF_Container("root",my_root, null);
            ret.add(root);
            ret.add(new WF_Container("filter_panel",filterRow2,root));
            ret.add(new WF_Container("table_panel",tableRecyclerView,root));
        } else {
            Log.w("PageWithTable", "my_root is null in getContainers.");
        }
        return ret;
    }

    @Override
    public boolean execute(String function, String target) {
        return false;
    }

    public void setSelectedColumnIndex(int myHeaderIndex) { /* TODO */ }
    public int getSelectedColumnIndex() { return currentlyFocusedColumn; }

}
