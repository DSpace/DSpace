/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.rmi.dgc.VMID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.coverity.security.Escape;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Utility functions for DSpace.
 *
 * @author Peter Breton
 */
public final class Utils {
    /**
     * log4j logger
     */
    private static final Logger log = LogManager.getLogger(Utils.class);

    private static final Pattern DURATION_PATTERN = Pattern
        .compile("(\\d+)([smhdwy])");

    private static final long MS_IN_SECOND = 1000L;

    private static final long MS_IN_MINUTE = 60000L;

    private static final long MS_IN_HOUR = 3600000L;

    private static final long MS_IN_DAY = 86400000L;

    private static final long MS_IN_WEEK = 604800000L;

    private static final long MS_IN_YEAR = 31536000000L;

    private static int counter = 0;

    private static final Random random = new Random();

    private static final VMID vmid = new VMID();

    // for parseISO8601Date
    private static final SimpleDateFormat[] parseFmt = {
        // first try at parsing, has milliseconds (note General time zone)
        new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss.SSSz"),

        // second try at parsing, no milliseconds (note General time zone)
        new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssz"),

        // finally, try without any timezone (defaults to current TZ)
        new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss.SSS"),

        new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss"),

        new SimpleDateFormat("yyyy'-'MM'-'dd")
    };

    // for formatISO8601Date
    // output canonical format (note RFC22 time zone, easier to hack)
    private static final SimpleDateFormat outFmtSecond
            = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssZ");

    // output format with millsecond precision
    private static final SimpleDateFormat outFmtMillisec
            = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss.SSSZ");

    private static final Calendar outCal = GregorianCalendar.getInstance();

    /**
     * Private constructor
     */
    private Utils() { }

    /**
     * Return an MD5 checksum for data in hex format.
     *
     * @param data The data to checksum.
     * @return MD5 checksum for the data in hex format.
     */
    public static String getMD5(String data) {
        return getMD5(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Return an MD5 checksum for data in hex format.
     *
     * @param data The data to checksum.
     * @return MD5 checksum for the data in hex format.
     */
    public static String getMD5(byte[] data) {
        return toHex(getMD5Bytes(data));
    }

    /**
     * Return an MD5 checksum for data as a byte array.
     *
     * @param data The data to checksum.
     * @return MD5 checksum for the data as a byte array.
     */
    public static byte[] getMD5Bytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            return digest.digest(data);
        } catch (NoSuchAlgorithmException nsae) {
            // ignore
        }

        // Should never happen
        return null;
    }

    /**
     * Return a hex representation of the byte array
     *
     * @param data The data to transform.
     * @return A hex representation of the data.
     */
    public static String toHex(byte[] data) {
        if ((data == null) || (data.length == 0)) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        // This is far from the most efficient way to do things...
        for (byte datum : data) {
            int low = datum & 0x0F;
            int high = datum & 0xF0;

            result.append(Integer.toHexString(high).charAt(0));
            result.append(Integer.toHexString(low));
        }

        return result.toString();
    }

    /**
     * Generate a unique key. The key is a long (length 38 to 40) sequence of
     * digits.
     *
     * @return A unique key as a long sequence of base-10 digits.
     */
    public static String generateKey() {
        return new BigInteger(generateBytesKey()).abs().toString();
    }

    /**
     * Generate a unique key. The key is a 32-character long sequence of hex
     * digits.
     *
     * @return A unique key as a long sequence of hex digits.
     */
    public static String generateHexKey() {
        return toHex(generateBytesKey());
    }

    /**
     * Generate a unique key as a byte array.
     *
     * @return A unique key as a byte array.
     */
    public static synchronized byte[] generateBytesKey() {
        byte[] junk = new byte[16];

        random.nextBytes(junk);
        String input = String.valueOf(vmid) + new Date() + Arrays.toString(junk) + counter++;

        return getMD5Bytes(input.getBytes(StandardCharsets.UTF_8));
    }

    // The following two methods are taken from the Jakarta IOUtil class.

    /**
     * Copy stream-data from source to destination. This method does not buffer,
     * flush or close the streams, as to do so would require making non-portable
     * assumptions about the streams' origin and further use. If you wish to
     * perform a buffered copy, use {@link #bufferedCopy}.
     *
     * @param input  The InputStream to obtain data from.
     * @param output The OutputStream to copy data to.
     * @throws IOException if IO error
     */
    public static void copy(final InputStream input, final OutputStream output)
        throws IOException {
        final int BUFFER_SIZE = 1024 * 4;
        final byte[] buffer = new byte[BUFFER_SIZE];

        while (true) {
            final int count = input.read(buffer, 0, BUFFER_SIZE);

            if (-1 == count) {
                break;
            }

            // write out those same bytes
            output.write(buffer, 0, count);
        }

        // needed to flush cache
        // output.flush();
    }

    /**
     * Copy stream-data from source to destination, with buffering. This is
     * equivalent to passing {@link #copy}a
     * <code>java.io.BufferedInputStream</code> and
     * <code>java.io.BufferedOutputStream</code> to {@link #copy}, and
     * flushing the output stream afterwards. The streams are not closed after
     * the copy.
     *
     * @param source      The InputStream to obtain data from.
     * @param destination The OutputStream to copy data to.
     * @throws IOException if IO error
     */
    public static void bufferedCopy(final InputStream source,
                                    final OutputStream destination) throws IOException {
        final BufferedInputStream input = new BufferedInputStream(source);
        final BufferedOutputStream output = new BufferedOutputStream(
            destination);
        copy(input, output);
        output.flush();
    }

    /**
     * Replace characters that could be interpreted as HTML codes with symbolic
     * references (entities). This function should be called before displaying
     * any metadata fields that could contain the characters {@code "<", ">", "&", "'"},
     * and double quotation marks. This will effectively disable HTML links
     * in metadata.
     *
     * @param value the metadata value to be scrubbed for display
     * @return the passed-in string, with html special characters replaced with
     * entities.
     */
    public static String addEntities(String value) {
        return Escape.html(value);
    }

    /**
     * Utility method to parse durations defined as {@code \d+[smhdwy]} (seconds,
     * minutes, hours, days, weeks, years)
     *
     * @param duration specified duration
     * @return number of milliseconds equivalent to duration.
     * @throws ParseException if the duration is of incorrect format
     */
    public static long parseDuration(String duration) throws ParseException {
        Matcher m = DURATION_PATTERN.matcher(duration.trim());
        if (!m.matches()) {
            throw new ParseException("'" + duration
                                         + "' is not a valid duration definition", 0);
        }

        String units = m.group(2);
        long multiplier;

        if ("s".equals(units)) {
            multiplier = MS_IN_SECOND;
        } else if ("m".equals(units)) {
            multiplier = MS_IN_MINUTE;
        } else if ("h".equals(units)) {
            multiplier = MS_IN_HOUR;
        } else if ("d".equals(units)) {
            multiplier = MS_IN_DAY;
        } else if ("w".equals(units)) {
            multiplier = MS_IN_WEEK;
        } else if ("y".equals(units)) {
            multiplier = MS_IN_YEAR;
        } else {
            throw new ParseException(units
                                         + " is not a valid time unit (must be 'y', "
                                         + "'w', 'd', 'h', 'm' or 's')", duration.indexOf(units));
        }

        long qint = Long.parseLong(m.group(1));

        return qint * multiplier;
    }

    /**
     * Translates timestamp from an ISO 8601-standard format, which
     * is commonly used in XML and RDF documents.
     * This method is synchronized because it depends on a non-reentrant
     * static DateFormat (more efficient than creating a new one each call).
     *
     * @param s the input string
     * @return Date object, or null if there is a problem translating.
     */
    public static synchronized Date parseISO8601Date(String s) {
        // attempt to normalize the timezone to something we can parse;
        // SimpleDateFormat can't handle "Z"
        char tzSign = s.charAt(s.length() - 6);
        if (s.endsWith("Z")) {
            s = s.substring(0, s.length() - 1) + "GMT+00:00";
        } else if ((tzSign == '-' || tzSign == '+') && s.length() > 10) {
            // check for trailing timezone
            s = s.substring(0, s.length() - 6) + "GMT" + s.substring(s.length() - 6);
        }

        // try to parse without milliseconds
        ParseException lastError = null;
        for (SimpleDateFormat simpleDateFormat : parseFmt) {
            try {
                return simpleDateFormat.parse(s);
            } catch (ParseException e) {
                lastError = e;
            }
        }
        if (lastError != null) {
            log.error("Error parsing date:", lastError);
        }
        return null;
    }

    /**
     * Convert a Date to String in the ISO 8601 standard format.
     * The RFC822 timezone is almost right, still need to insert ":".
     * This method is synchronized because it depends on a non-reentrant
     * static DateFormat (more efficient than creating a new one each call).
     *
     * @param d the input Date
     * @return String containing formatted date.
     */
    public static synchronized String formatISO8601Date(Date d) {
        String result;
        outCal.setTime(d);
        if (outCal.get(Calendar.MILLISECOND) == 0) {
            result = outFmtSecond.format(d);
        } else {
            result = outFmtMillisec.format(d);
        }
        int rl = result.length();
        return result.substring(0, rl - 2) + ":" + result.substring(rl - 2);
    }

    public static <E> java.util.Collection<E> emptyIfNull(java.util.Collection<E> collection) {
        return collection == null ? Collections.emptyList() : collection;
    }

    /**
     * Utility method to extract schema, element, qualifier from the metadata field key
     * Keep in mind that this method try to auto discover the common separator used in DSpace ("_" or ".")
     *
     * Return an array of token with size 3 which contains:
     * schema = tokens[0];
     * element = tokens[1];
     * qualifier = tokens[2]; //it can be empty string
     *
     * @param metadata (the field in the form dc.title or dc_title)
     * @return array of tokens
     */
    public static String[] tokenize(String metadata) {
        String separator = metadata.contains("_") ? "_" : ".";
        StringTokenizer dcf = new StringTokenizer(metadata, separator);

        String[] tokens = {"", "", ""};
        int i = 0;
        while (dcf.hasMoreTokens()) {
            tokens[i] = dcf.nextToken().trim();
            i++;
        }
        // Tokens contains:
        // schema = tokens[0];
        // element = tokens[1];
        // qualifier = tokens[2];
        return tokens;

    }

    /**
     * Make the metadata field key using the separator.
     *
     * @param schema
     * @param element
     * @param qualifier
     * @param separator (DSpace common separator are "_" or ".")
     * @return metadata field key
     */
    public static String standardize(String schema, String element, String qualifier, String separator) {
        if (StringUtils.isBlank(qualifier)) {
            return schema + separator + element;
        } else {
            return schema + separator + element + separator + qualifier;
        }
    }

    /**
     * Retrieve the baseurl from a given URL string
     * @param urlString URL string
     * @return baseurl (without any context path) or null (if URL was invalid)
     */
    public static String getBaseUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String baseUrl = url.getProtocol() + "://" + url.getHost();
            if (url.getPort() != -1) {
                baseUrl += (":" + url.getPort());
            }
            return baseUrl;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Retrieve the hostname from a given URI string
     * @param uriString URI string
     * @return hostname (without any www.) or null (if URI was invalid)
     */
    public static String getHostName(String uriString) {
        try {
            URI uri = new URI(uriString);
            String hostname = uri.getHost();
            // remove the "www." from hostname, if it exists
            if (hostname != null) {
                return hostname.startsWith("www.") ? hostname.substring(4) : hostname;
            }
            return null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Retrieve the IP address(es) of a given URI string.
     * <P>
     * At this time, DSpace only supports IPv4, so this method will only return IPv4 addresses.
     * @param uriString URI string
     * @return IP address(es) in a String array (or null if not found)
     */
    public static String[] getIPAddresses(String uriString) {
        String[] ipAddresses = null;

        // First, get the hostname
        String hostname = getHostName(uriString);

        if (StringUtils.isNotEmpty(hostname)) {
            try {
                // Then, get the list of all IPs for that hostname
                InetAddress[] inetAddresses = InetAddress.getAllByName(hostname);

                // Convert array of InetAddress objects to array of IP address Strings
                ipAddresses = Arrays.stream(inetAddresses)
                                    // Filter our array to ONLY include IPv4 addresses
                                    .filter((address) -> address instanceof Inet4Address)
                                    // Call getHostAddress() on each to get the IPv4 address as a string
                                    .map((address) -> ((Inet4Address) address).getHostAddress())
                                    .toArray(String[]::new);
            } catch (UnknownHostException ex) {
                return null;
            }
        }

        return ipAddresses;
    }


    /**
     * Replaces configuration placeholders within a String with the corresponding value
     * from DSpace's Configuration Service.
     * <P>
     * For example, given a String like "My DSpace is installed at ${dspace.dir}", this
     * method will replace "${dspace.dir}" with the configured value of that property.
     * @param string source string
     * @return string with any placeholders replaced with configured values.
     */
    public static String interpolateConfigsInString(String string) {
        ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();
        return StringSubstitutor.replace(string, config.getProperties());
    }
}
