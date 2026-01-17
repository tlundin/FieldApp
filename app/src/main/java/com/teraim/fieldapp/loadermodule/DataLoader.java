package com.teraim.fieldapp.loadermodule;

import android.util.JsonReader;
import android.util.Log;
import android.util.MalformedJsonException;
import android.util.Xml;

import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.loadermodule.configurations.CI_ConfigurationModule;
import com.teraim.fieldapp.loadermodule.configurations.Dependant_Configuration_Missing;
import com.teraim.fieldapp.utils.Tools;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * A stateless utility class for loading and parsing configuration modules from the web.
 * Replaces the old Loader and WebLoader AsyncTask implementation.
 * IMPORTANT: All public methods in this class are synchronous (blocking) and must be
 * called from a background thread.
 */
public final class DataLoader {

    // Private constructor to prevent instantiation of this utility class.
    private DataLoader() {}

    /**
     * Loads, parses, and freezes a configuration module from a given URL.
     * This is a synchronous, blocking call and MUST be run on a background thread.
     *
     * @param module The ConfigurationModule to be processed.
     * @return A LoadResult indicating the outcome of the operation.
     */
    public static LoadResult loadAndParseAndFreeze(ConfigurationModule module) {
        // --- Retry Logic Setup ---
        final int MAX_ATTEMPTS = 3;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            URL url = null;
            try {
                url = new URL(module.getURL());
                Log.d("vortex", "Trying to open connection (Attempt " + attempt + "): " + url);
                URLConnection ucon = url.openConnection();
                ucon.setConnectTimeout(5000);
                try (InputStream in = ucon.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {

                    // Get Version, Read, Parse, Freeze... (your existing logic)
                    float version = getVersion(reader, module);
                    StringBuilder sb = new StringBuilder();
                    LoadResult readResult = read(reader, module, version, sb);

                    // If read was successful, parse and then freeze the module.
                    if (readResult.errCode == ErrorCode.loaded) {
                        LoadResult parseResult = parse(module);
                        if (parseResult.errCode == ErrorCode.parsed) {
                            //module.state.postValue(ConfigurationModule.State.FREEZING);
                            LoadResult freezeResult = freeze(module);
                            // --- SUCCESS! ---
                            // If everything worked, return the result and exit the loop.
                            return freezeResult;
                        } else {
                            return parseResult; // Return non-recoverable parse error immediately.
                        }
                    }
                }
            } catch (IOException e) {
                Log.w("vortex", "Attempt " + attempt + " failed with IOException: " + e.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    // If this was the last attempt, return the error.
                    return handleIOException(e, module, url);
                }
                // Before retrying, wait for a moment.
                try {
                    // Exponential backoff: 1s, 2s, 4s...
                    long delay = (long) (1000 * Math.pow(2, attempt - 1));
                    Log.d("vortex", "Waiting for " + delay + "ms before retrying.");
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new LoadResult(module, ErrorCode.Aborted, "Retry delay was interrupted.");
                }
            } catch (Exception e) {
                // Catch other non-recoverable exceptions and return immediately.
                Log.e("vortex", "A non-recoverable error occurred.", e);
                // This could be XmlPullParserException, JSONException, etc.
                return new LoadResult(module, ErrorCode.ParseError, e.getMessage());
        } // End of for loop

        // This line is reached only if all retries failed with a recoverable error.
        return new LoadResult(module, ErrorCode.IOError, "All " + MAX_ATTEMPTS + " attempts failed.");
    }

    private static LoadResult handleIOException(IOException e, ConfigurationModule module, URL url) {
        if (e instanceof UnknownHostException) {
            return new LoadResult(module, ErrorCode.HostNotFound, "Server not found: " + (url != null ? url.getHost() : ""));
        } else if (e instanceof MalformedJsonException) {
            return new LoadResult(module, ErrorCode.ParseError, "Malformed JSON: " + e.getMessage());
        } else if (e instanceof FileNotFoundException) {
            return new LoadResult(module, ErrorCode.notFound);
        } else if (e instanceof java.net.SocketTimeoutException) {
            return new LoadResult(module, ErrorCode.socket_timeout);
        } else {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return new LoadResult(module, ErrorCode.IOError, sw.toString());
        }
    }

    // The getVersion, parseVersionFromHeader, read, and parse methods below do not need changes.
    // ... (rest of the class is the same until the freeze method) ...
    private static float getVersion(BufferedReader reader, ConfigurationModule module) throws IOException {
        float version = -1;
        reader.mark(500); // Mark the start of the stream to allow reset
        try {
            if (module.isBundle) {
                reader.readLine(); // Skip first line
                String headerRow2 = reader.readLine();
                version = parseVersionFromHeader(null, headerRow2);
            } else if (module.hasSimpleVersion) {
                String headerRow1 = reader.readLine();
                if (headerRow1 == null) {
                    throw new IOException("Cannot read data from an empty file.");
                }
                version = parseVersionFromHeader(headerRow1, null);
            }
        } finally {
            // Reset reader to the beginning unless a version was found for simple versioning
            if (version == -1 || module.isBundle) {
                reader.reset();
            }
        }
        return version;
    }
    private static float parseVersionFromHeader(String h1, String h2) {
        if (h2 != null) {
            int p = h2.indexOf("app_version");
            if (p > 0) {
                p = h2.indexOf("version", p + 11);
                String vNo = h2.substring(p + 9, h2.indexOf('\"', p + 9));
                if (Tools.isVersionNumber(vNo)) return Float.parseFloat(vNo);
            }
        } else if (h1 != null) {
            String[] header = h1.split(",");
            if (header.length >= 2) {
                String potVer = header[1].trim();
                if (Tools.isVersionNumber(potVer)) return Float.parseFloat(potVer);
            }
        }
        return -1;
    }
    private static LoadResult read(BufferedReader reader, ConfigurationModule m, float newVersion, StringBuilder sb) throws IOException {
        if (newVersion != -1)
            m.setNewVersion(newVersion);
        String line;
        int rowC = 0;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
            rowC++;
        }
        m.setRawData(sb.toString(), rowC);
        return new LoadResult(m, ErrorCode.loaded);
    }
    private static LoadResult parse(ConfigurationModule m) throws IOException, XmlPullParserException, JSONException, Dependant_Configuration_Missing {
        switch (m.fileFormat) {
            case csv:
            case ini:
            case jgw:
                return parseCSV((CI_ConfigurationModule) m);
            case json:
                return parseJSON((JSONConfigurationModule) m);
            case txt:
                return parseCSV((CI_ConfigurationModule) m);
            case xml:
            default:
                return parseXML((XMLConfigurationModule) m);
        }
    }
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_SECONDS = 1;
    private static LoadResult parseCSV(CI_ConfigurationModule m) throws IOException {
        LoadResult lr;
        Log.d("vortex","parse csv");
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                lr = m.prepare();
                if (lr != null)
                    return lr;
                else
                    break;
            } catch (Dependant_Configuration_Missing e) {
                if (attempt == MAX_RETRIES)
                    return new LoadResult(m, ErrorCode.reloadDependant, e.getDependendant());
                System.out.println("Waiting for " + RETRY_DELAY_SECONDS + " second(s) before retrying...");
                try {
                    TimeUnit.SECONDS.sleep(RETRY_DELAY_SECONDS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    // And exit, wrapping the interruption in a RuntimeException.
                    throw new RuntimeException("Preparation was interrupted.", interruptedException);
                }

            }
        }


        String rawData = m.getRawData();
        if (rawData == null) return new LoadResult(m, ErrorCode.noData);

        String[] myRows = rawData.split("\\n");
        int rowC = 1;
        for (String row : myRows) {
            LoadResult loadR = m.parse(row, rowC);
            if (loadR != null) {
                return loadR; // Parsing failed for a row
            }
            rowC++;
        }
        m.finalizeMe();
        return new LoadResult(m, ErrorCode.parsed);
    }
    private static LoadResult parseXML(XMLConfigurationModule m) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(new StringReader(m.getRawData()));

        LoadResult lr = m.prepare(parser);
        if (lr != null) return lr;

        while (m.parse(parser) == null) {
            // Loop until parse returns a result (either success or failure)
        }
        return m.parse(parser); // Return the final result
    }
    private static LoadResult parseJSON(JSONConfigurationModule m) throws IOException, JSONException {
        // Since the source is a StringReader, we should still ensure the reader is closed properly.
        try (JsonReader parser = new JsonReader(new StringReader(m.getRawData()))) {
            LoadResult lr = m.prepare(parser);
            if (lr != null) {
                return lr;
            }

            // Corrected loop logic
            LoadResult parseResult;
            while (true) {
                // Call parse() only ONCE per loop iteration
                parseResult = m.parse(parser);
                // If the result is not null, we are done. Break the loop.
                if (parseResult != null) {
                    break;
                }
            }
            // Return the result you already obtained.
            return parseResult;
        }
    }


    /**
     * "Freezes" the configuration module after it has been parsed.
     */
    private static LoadResult freeze(ConfigurationModule m) throws IOException {
        Log.d("abba", "Freeze called for " + m.getLabel());
        if (m.freezeSteps > 0) {
            for (int i = 0; i < m.freezeSteps; i++) {
                m.freeze(i);
            }
        } else {
            m.freeze(-1);
        }

        if (m.newVersion != -1) {
            m.setFrozenVersion(m.newVersion);
        }

        if (m.getEssence() != null || m.isDatabaseModule) {
            // CORRECTED: Use postValue from a background thread.
            m.state.postValue(ConfigurationModule.ModuleLoadState.FROZEN);
            return new LoadResult(m, ErrorCode.frozen);
        } else {
            // This enum value does not exist in ConfigurationModule.State. Assuming ERROR or a new state.
            // For now, let's use ERROR. If you have NO_DATA state, you can use that.
            // CORRECTED: Use postValue from a background thread.
            m.state.postValue(ConfigurationModule.ModuleLoadState.ERROR); // Assuming NO_DATA state doesn't exist.
            return new LoadResult(m, ErrorCode.noData);
        }
    }
}