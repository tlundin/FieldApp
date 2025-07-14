package com.teraim.fieldapp.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.teraim.fieldapp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MapNeedlePreference extends Preference {

    private int selectedValue = 0; // Stores the selected individual icon index (0 to 11)

    private int[] fullImageSetResourceIds;
    private List<Bitmap> allIndividualNeedleIcons; // A combined list of all 12 individual needles

    public MapNeedlePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public MapNeedlePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public MapNeedlePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MapNeedlePreference(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MapNeedlePreference);

            int imageSetResourcesArrayId = a.getResourceId(R.styleable.MapNeedlePreference_imageSetResources, 0);
            if (imageSetResourcesArrayId != 0) {
                TypedArray ta = context.getResources().obtainTypedArray(imageSetResourcesArrayId);
                fullImageSetResourceIds = new int[ta.length()];
                for (int i = 0; i < ta.length(); i++) {
                    fullImageSetResourceIds[i] = ta.getResourceId(i, 0);
                }
                ta.recycle();
            } else {
                Log.e("MapNeedlePref", "imageSetResources attribute not found or invalid. Check configmenu.xml and arrays.xml");
            }
            a.recycle();
        }

        allIndividualNeedleIcons = new ArrayList<>();
        if (fullImageSetResourceIds != null) {
            for (int resourceId : fullImageSetResourceIds) {
                if (resourceId != 0) {
                    allIndividualNeedleIcons.addAll(cropAllNeedlesFromSet(getContext(), resourceId));
                }
            }
            if (allIndividualNeedleIcons.isEmpty()) {
                Log.e("MapNeedlePref", "No individual icons were successfully cropped. Check source images and crop parameters.");
            }
        } else {
            Log.e("MapNeedlePref", "fullImageSetResourceIds is null, cannot load any icons.");
        }

        setWidgetLayoutResource(R.layout.preference_map_needle_widget);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ImageView previewImage = (ImageView) holder.findViewById(R.id.map_needle_preview);
        if (previewImage != null && allIndividualNeedleIcons != null && allIndividualNeedleIcons.size() > selectedValue && selectedValue >= 0) {
            Bitmap selectedBitmap = allIndividualNeedleIcons.get(selectedValue);
            if (selectedBitmap != null) {
                previewImage.setImageBitmap(selectedBitmap);
            } else {
                previewImage.setImageDrawable(null);
                Log.w("MapNeedlePref", "Selected individual bitmap at index " + selectedValue + " is null for preview.");
            }
        } else if (previewImage != null) {
            previewImage.setImageDrawable(null);
            Log.w("MapNeedlePref", "No valid individual icon to display in preview (allIndividualNeedleIcons size: " + (allIndividualNeedleIcons != null ? allIndividualNeedleIcons.size() : "null") + ", selectedValue: " + selectedValue + ").");
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
        Log.d("MapNeedlePref", "MapNeedlePreference clicked!");
        showMapNeedleDialog();
    }

    private void showMapNeedleDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(getTitle());

            LayoutInflater inflater = LayoutInflater.from(getContext());
            View dialogView = inflater.inflate(R.layout.dialog_map_needle_selector, null);
            GridView gridView = dialogView.findViewById(R.id.map_needle_grid);

            if (allIndividualNeedleIcons == null || allIndividualNeedleIcons.isEmpty()) {
                Log.e("MapNeedlePref", "No individual map needle icons loaded. Cannot display dialog.");
                Toast.makeText(getContext(), "Map needle options not loaded. Please check images and configuration.", Toast.LENGTH_LONG).show();
                return;
            }

            final AllNeedleIconsAdapter adapter = new AllNeedleIconsAdapter(getContext(), allIndividualNeedleIcons);
            gridView.setAdapter(adapter);

            builder.setView(dialogView);

            final AlertDialog dialog = builder.create();
            dialog.show();

            gridView.setOnItemClickListener((parent, view, position, id) -> {
                selectedValue = position;
                persistInt(selectedValue);

                setSummary("Selected: Needle " + (selectedValue + 1));
                notifyChanged();
                dialog.dismiss();
            });
        } catch (Exception e) {
            Log.e("MapNeedlePref", "Error showing Map Needle selection dialog", e);
            Toast.makeText(getContext(), "Error loading map needle options. See log for details.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        if (defaultValue instanceof Integer) {
            selectedValue = getPersistedInt((Integer) defaultValue);
        } else if (defaultValue == null) {
            selectedValue = getPersistedInt(0);
            Log.w("MapNeedlePref", "defaultValue in onSetInitialValue was null. Using 0 as fallback.");
        } else {
            Log.e("MapNeedlePref", "Unexpected defaultValue type in onSetInitialValue: " + defaultValue.getClass().getName() + ". Using 0 as fallback.");
            selectedValue = getPersistedInt(0);
        }
        setSummary("Selected: Needle " + (selectedValue + 1));
    }

    @Override
    protected boolean persistInt(int value) {
        return super.persistInt(value);
    }

    /**
     * Crops all 6 individual map needles from a single image set.
     * Assumes the image has a fixed layout of 3 columns and 2 rows,
     * and the total image is 1024x1024 pixels.
     *
     * @param context The context to access resources.
     * @param imageSetResourceId The resource ID of the full image set.
     * @return A list of 6 cropped Bitmap icons.
     */
    public static List<Bitmap> cropAllNeedlesFromSet(Context context, int imageSetResourceId) {
        Bitmap fullBitmap = BitmapFactory.decodeResource(context.getResources(), imageSetResourceId);
        List<Bitmap> croppedIcons = new ArrayList<>();

        if (fullBitmap == null) {
            Log.e("MapNeedlePreference", "Bitmap for resource ID " + imageSetResourceId + " is null. Check if the drawable exists and is correctly named.");
            return croppedIcons;
        }

        int totalWidth = fullBitmap.getWidth();
        int totalHeight = fullBitmap.getHeight();

        // **CRITICAL FIX: Use the exact dimensions from your measurement**
        // If the total image is 1024x1024 and it's a 3x2 grid:
        final int IDEAL_ICON_WIDTH = 800 / 3; // Integer division will give 341
        final int IDEAL_ICON_HEIGHT = 800 / 2; // Will give 512

        final int NUM_COLUMNS = 3;
        final int NUM_ROWS = 2;

        Log.d("MapNeedlePreference", "Cropping from resource " + context.getResources().getResourceEntryName(imageSetResourceId) +
                ". Full image dims: " + totalWidth + "x" + totalHeight +
                ". Expected individual icon dims: " + IDEAL_ICON_WIDTH + "x" + IDEAL_ICON_HEIGHT + " (approx. 341x512)");

        // Add a check to ensure the actual image dimensions match expectations.
        // If they don't, the cropping will be off.
        if (totalWidth != 800 || totalHeight != 800) {
            Log.e("MapNeedlePreference", "Mismatched image dimensions! Expected 1024x1024, but found " + totalWidth + "x" + totalHeight + " for resource " + context.getResources().getResourceEntryName(imageSetResourceId) + ". Cropping will likely be incorrect.");
            // You might consider returning early here or handling this more robustly.
        }


        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLUMNS; col++) {
                int x = col * IDEAL_ICON_WIDTH;
                int y = row * IDEAL_ICON_HEIGHT;

                // Adjust width for the last column to account for integer division truncation
                int currentIconWidth = IDEAL_ICON_WIDTH;
                if (col == NUM_COLUMNS - 1 && x + IDEAL_ICON_WIDTH > totalWidth) {
                    currentIconWidth = totalWidth - x; // Take the remaining width
                }

                // Adjust height for the last row (though not needed for 2 rows and 512 height)
                int currentIconHeight = IDEAL_ICON_HEIGHT;
                if (row == NUM_ROWS - 1 && y + IDEAL_ICON_HEIGHT > totalHeight) {
                    currentIconHeight = totalHeight - y;
                }

                if (x < 0 || y < 0 || x + currentIconWidth > totalWidth || y + currentIconHeight > totalHeight) {
                    Log.e("MapNeedlePreference", "ERROR: Calculated crop region out of bounds for icon at col " + col + ", row " + row + " in resource " + context.getResources().getResourceEntryName(imageSetResourceId) + ". Crop Region: X=" + x + ", Y=" + y + ", W=" + currentIconWidth + ", H=" + currentIconHeight + ". Image Dims:" + totalWidth + "x" + totalHeight);
                    // You might want to add a placeholder bitmap or skip this icon.
                    continue; // Skip this iteration if bounds are bad
                }

                try {
                    Bitmap croppedBitmap = Bitmap.createBitmap(fullBitmap, x, y, currentIconWidth, currentIconHeight);
                    croppedIcons.add(croppedBitmap);

                    // Debugging: Save cropped bitmap to verify
                    /*
                    try {
                        File cacheDir = context.getExternalCacheDir();
                        if (cacheDir != null) {
                            String resourceName = context.getResources().getResourceEntryName(imageSetResourceId);
                            File debugFile = new File(cacheDir, "debug_icon_" + resourceName + "_row" + row + "_col" + col + ".png");
                            FileOutputStream out = new FileOutputStream(debugFile);
                            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            out.flush();
                            out.close();
                            // Log.d("MapNeedlePreference", "Saved debug icon to: " + debugFile.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        Log.e("MapNeedlePreference", "Error saving debug bitmap for " + imageSetResourceId, e);
                    }
                    */

                } catch (IllegalArgumentException e) {
                    Log.e("MapNeedlePreference", "IllegalArgumentException during Bitmap.createBitmap for resource " + context.getResources().getResourceEntryName(imageSetResourceId) + " at col " + col + ", row " + row + ". Details: " + e.getMessage());
                    // This often happens if width or height passed to createBitmap is 0 or negative,
                    // or if the region is invalid.
                }
            }
        }
        // fullBitmap.recycle();
        return croppedIcons;
    }

    private static class AllNeedleIconsAdapter extends BaseAdapter {
        private final Context context;
        private final List<Bitmap> allIcons;
        private final LayoutInflater inflater;

        public AllNeedleIconsAdapter(Context context, List<Bitmap> allIcons) {
            this.context = context;
            this.allIcons = allIcons;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return allIcons.size();
        }

        @Override
        public Object getItem(int position) {
            return allIcons.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.grid_item_map_needle, parent, false);
            }

            ImageView imageView = convertView.findViewById(R.id.grid_item_image);
            TextView textView = convertView.findViewById(R.id.grid_item_text);

            Bitmap iconBitmap = allIcons.get(position);
            if (iconBitmap != null) {
                imageView.setImageBitmap(iconBitmap);
            } else {
                imageView.setImageDrawable(null);
                Log.w("AllNeedleIconsAdapter", "Bitmap at index " + position + " is null for display.");
            }

            textView.setText("Needle " + (position + 1));

            return convertView;
        }
    }
}