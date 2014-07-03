
package info.freelibrary.djatoka;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import info.freelibrary.djatoka.iiif.Constants;
import info.freelibrary.djatoka.util.CacheUtils;
import info.freelibrary.util.XMLBundleControl;
import info.freelibrary.util.XMLResourceBundle;

public class TileCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(TileCache.class);

    private static final XMLResourceBundle BUNDLE = (XMLResourceBundle) ResourceBundle.getBundle(
            "FreeLib-Djatoka_Messages", new XMLBundleControl());

    private TileCache() {
    }

    /**
     * @param args
     */
    public static void main(final String[] args) throws IOException {
        String[] ids;

        if (args.length == 2 || args.length == 3) {
            String server = "http://localhost:8888/";
            final File csvFile = new File(args[0]);
            CSVReader csvReader;
            int index;

            if (!csvFile.exists()) {
                LOGGER.error(BUNDLE.get("TC_FILE_NOT_FOUND") + csvFile);
                printUsageAndExit();
            }

            // Make sure format of supplied server URL is what we expect
            if (args.length == 3) {
                if (!args[2].startsWith("http://")) {
                    args[2] = "http://" + args[2];
                }

                if (!args[2].endsWith("/")) {
                    args[2] = args[2] + "/";
                }

                server = args[2];
            }

            if (!isLive(server)) {
                LOGGER.error(BUNDLE.get("TC_SERVER_404"), server);
                printUsageAndExit();
            }

            try {
                csvReader = new CSVReader(new FileReader(csvFile));
                index = Integer.parseInt(args[1]) - 1; // columns 1-based

                while ((ids = csvReader.readNext()) != null) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(BUNDLE.get("TC_CACHE_ID"), ids[index]);
                    }

                    cacheImage(server, ids[index]);
                }
            } catch (final NumberFormatException details) {
                LOGGER.error(details.getMessage());
                printUsageAndExit();
            }

        } else {
            printUsageAndExit();
        }

    }

    private static void cacheImage(final String aServer, final String aID) {
        String urlString;

        try {
            final String id = URLEncoder.encode(aID, "UTF-8");
            final String baseURL = aServer + "view/image/" + id;
            final URL url = new URL(baseURL + "/info.xml");
            final HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            final int status = uc.getResponseCode();

            // Helps add a little more detail to logging statements
            urlString = url.toString();

            if (status == 200) {
                final String ns = Constants.IIIF_NS;
                final Document xml = new Builder().build(uc.getInputStream());
                final Element info = xml.getRootElement();
                final Element elem = info.getFirstChildElement("identifier", ns);
                final Element hElem = info.getFirstChildElement("height", ns);
                final Element wElem = info.getFirstChildElement("width", ns);
                final String idValue = elem.getValue();

                try {
                    final int height = Integer.parseInt(hElem.getValue());
                    final int width = Integer.parseInt(wElem.getValue());
                    Iterator<String> tileIterator;
                    List<String> tiles;

                    if (idValue.equals(aID) && height > 0 && width > 0) {
                        if (idValue.startsWith("/") && LOGGER.isWarnEnabled()) {
                            LOGGER.warn(BUNDLE.get("TC_SLASH_ID"), aID);
                        }

                        tiles = CacheUtils.getCachingQueries(height, width);
                        tileIterator = tiles.iterator();

                        while (tileIterator.hasNext()) {
                            cacheTile(baseURL + tileIterator.next());
                        }
                    } else if (LOGGER.isErrorEnabled()) {
                        LOGGER.error(BUNDLE.get("TC_ID_404"), aID);
                    }
                } catch (final NumberFormatException nfe) {
                    LOGGER.error(BUNDLE.get("TC_INVALID_DIMS"), aID);
                }
            } else {
                LOGGER.error(BUNDLE.get("TC_SERVER_STATUS_CODE"), status, urlString);
            }
        } catch (IOException | ParsingException details) {
            LOGGER.error(details.getMessage());
        }
    }

    private static void cacheTile(final String aURL) {
        try {
            final URL url = new URL(aURL);
            final HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            final int status = uc.getResponseCode();

            if (status == 200) {
                final int contentLength = uc.getContentLength();

                if (contentLength == -1) {
                    // LOGGER.debug("Tile '{}' content length: -1", aURL);
                }
            } else if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Problem caching '{}' tile (status: {})", aURL, status);
            }
        } catch (final Exception details) {
            LOGGER.error(details.getMessage(), details);
        }
    }

    private static boolean isLive(final String aServer) {
        try {
            final URL url = new URL(aServer + "health");
            final HttpURLConnection uc = (HttpURLConnection) url.openConnection();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(BUNDLE.get("TC_STATUS_CHECK"), url);
            }

            if (uc.getResponseCode() == 200) {
                final Document xml = new Builder().build(uc.getInputStream());
                final Element response = xml.getRootElement();
                final Element health = response.getFirstChildElement("health");
                final String status = health.getValue();

                if (status.equals("dying") || status.equals("sick")) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(BUNDLE.get("TC_SERVER_STATUS"), status);
                    }

                    return true;
                } else if (status.equals("ok")) {
                    return true;
                } else {
                    LOGGER.error(BUNDLE.get("TC_UNEXPECTED_STATUS"), status);
                }
            }
        } catch (final UnknownHostException details) {
            LOGGER.error(BUNDLE.get("TC_UNKNOWN_HOST"), details.getMessage());
        } catch (final Exception details) {
            LOGGER.error(details.getMessage());
        }

        return false;
    }

    private static void printUsageAndExit() {
        final String eol = System.getProperty("line.separator");
        final StringBuilder sb = new StringBuilder(eol);

        sb.append(BUNDLE.get("TC_USAGE_1")).append(BUNDLE.get("TC_USAGE_EXEC"));
        sb.append(BUNDLE.get("TC_ARGS_OPT_1")).append(eol);
        sb.append(BUNDLE.get("TC_USAGE_2")).append(BUNDLE.get("TC_USAGE_EXEC"));
        sb.append(BUNDLE.get("TC_ARGS_OPT_2"));

        System.out.println(sb.toString());
        System.exit(1);
    }
}
