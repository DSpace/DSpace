/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Constants;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;


/**
 * Miscellaneous utility methods
 *
 * @author Robert Tansley
 * @author Mark Diggory
 * @version $Revision$
 */
public class Util {
        // cache for source version result
        private static String sourceVersion = null;

        private static Logger log = Logger.getLogger(Util.class);
        
        /**
         * Utility method to convert spaces in a string to HTML non-break space
         * elements.
         *
         * @param s
         *            string to change spaces in
         * @return the string passed in with spaces converted to HTML non-break
         *         spaces
         */
        public static String nonBreakSpace(String s) {
            StringBuffer newString = new StringBuffer();
        
            for (int i = 0; i < s.length(); i++)
            {
                char ch = s.charAt(i);
        
                if (ch == ' ')
                {
                    newString.append("&nbsp;");
                }
                else
                {
                    newString.append(ch);
                }
            }
        
            return newString.toString();
        }

        /**
         * Encode a bitstream name for inclusion in a URL in an HTML document. This
         * differs from the usual URL-encoding, since we want pathname separators to
         * be passed through verbatim; this is required so that relative paths in
         * bitstream names and HTML references work correctly.
         * <P>
         * If the link to a bitstream is generated with the pathname separators
         * escaped (e.g. "%2F" instead of "/") then the Web user agent perceives it
         * to be one pathname element, and relative URI paths within that document
         * containing ".." elements will be handled incorrectly.
         * <P>
         *
         * @param stringIn
         *            input string to encode
         * @param encoding
         *            character encoding, e.g. UTF-8
         * @return the encoded string
         * @throws java.io.UnsupportedEncodingException if encoding error
         */
        public static String encodeBitstreamName(String stringIn, String encoding) throws java.io.UnsupportedEncodingException {
            // FIXME: This should be moved elsewhere, as it is used outside the UI
            if (stringIn == null)
            {
                return "";
            }

            StringBuffer out = new StringBuffer();
        
            final String[] pctEncoding = { "%00", "%01", "%02", "%03", "%04",
                    "%05", "%06", "%07", "%08", "%09", "%0a", "%0b", "%0c", "%0d",
                    "%0e", "%0f", "%10", "%11", "%12", "%13", "%14", "%15", "%16",
                    "%17", "%18", "%19", "%1a", "%1b", "%1c", "%1d", "%1e", "%1f",
                    "%20", "%21", "%22", "%23", "%24", "%25", "%26", "%27", "%28",
                    "%29", "%2a", "%2b", "%2c", "%2d", "%2e", "%2f", "%30", "%31",
                    "%32", "%33", "%34", "%35", "%36", "%37", "%38", "%39", "%3a",
                    "%3b", "%3c", "%3d", "%3e", "%3f", "%40", "%41", "%42", "%43",
                    "%44", "%45", "%46", "%47", "%48", "%49", "%4a", "%4b", "%4c",
                    "%4d", "%4e", "%4f", "%50", "%51", "%52", "%53", "%54", "%55",
                    "%56", "%57", "%58", "%59", "%5a", "%5b", "%5c", "%5d", "%5e",
                    "%5f", "%60", "%61", "%62", "%63", "%64", "%65", "%66", "%67",
                    "%68", "%69", "%6a", "%6b", "%6c", "%6d", "%6e", "%6f", "%70",
                    "%71", "%72", "%73", "%74", "%75", "%76", "%77", "%78", "%79",
                    "%7a", "%7b", "%7c", "%7d", "%7e", "%7f", "%80", "%81", "%82",
                    "%83", "%84", "%85", "%86", "%87", "%88", "%89", "%8a", "%8b",
                    "%8c", "%8d", "%8e", "%8f", "%90", "%91", "%92", "%93", "%94",
                    "%95", "%96", "%97", "%98", "%99", "%9a", "%9b", "%9c", "%9d",
                    "%9e", "%9f", "%a0", "%a1", "%a2", "%a3", "%a4", "%a5", "%a6",
                    "%a7", "%a8", "%a9", "%aa", "%ab", "%ac", "%ad", "%ae", "%af",
                    "%b0", "%b1", "%b2", "%b3", "%b4", "%b5", "%b6", "%b7", "%b8",
                    "%b9", "%ba", "%bb", "%bc", "%bd", "%be", "%bf", "%c0", "%c1",
                    "%c2", "%c3", "%c4", "%c5", "%c6", "%c7", "%c8", "%c9", "%ca",
                    "%cb", "%cc", "%cd", "%ce", "%cf", "%d0", "%d1", "%d2", "%d3",
                    "%d4", "%d5", "%d6", "%d7", "%d8", "%d9", "%da", "%db", "%dc",
                    "%dd", "%de", "%df", "%e0", "%e1", "%e2", "%e3", "%e4", "%e5",
                    "%e6", "%e7", "%e8", "%e9", "%ea", "%eb", "%ec", "%ed", "%ee",
                    "%ef", "%f0", "%f1", "%f2", "%f3", "%f4", "%f5", "%f6", "%f7",
                    "%f8", "%f9", "%fa", "%fb", "%fc", "%fd", "%fe", "%ff" };
        
            byte[] bytes = stringIn.getBytes(encoding);
        
            for (int i = 0; i < bytes.length; i++)
            {
                // Any unreserved char or "/" goes through unencoded
                if ((bytes[i] >= 'A' && bytes[i] <= 'Z')
                        || (bytes[i] >= 'a' && bytes[i] <= 'z')
                        || (bytes[i] >= '0' && bytes[i] <= '9') || bytes[i] == '-'
                        || bytes[i] == '.' || bytes[i] == '_' || bytes[i] == '~'
                        || bytes[i] == '/')
                {
                    out.append((char) bytes[i]);
                }
                else if (bytes[i] >= 0)
                {
                    // encode other chars (byte code < 128)
                    out.append(pctEncoding[bytes[i]]);
                }
                else
                {
                    // encode other chars (byte code > 127, so it appears as
                    // negative in Java signed byte data type)
                    out.append(pctEncoding[256 + bytes[i]]);
                }
            }
            log.debug("encoded \"" + stringIn + "\" to \"" + out.toString() + "\"");
        
            return out.toString();
        }

        /** Version of encodeBitstreamName with one parameter, uses default encoding
         * <P>
         * @param stringIn
         *                input string to encode
         * @return the encoded string
         * @throws java.io.UnsupportedEncodingException if encoding error
         */
        public static String encodeBitstreamName(String stringIn) throws java.io.UnsupportedEncodingException {
                return encodeBitstreamName(stringIn, Constants.DEFAULT_ENCODING);
         }

        /**
          * Formats the file size. Examples:
          *
          *  - 50 = 50B
          *  - 1024 = 1KB
          *  - 1,024,000 = 1MB etc
          *
          *  The numbers are formatted using java Locales
          *
          * @param in The number to convert
          * @return the file size as a String
          */
        public static String formatFileSize(double in) {
             // Work out the size of the file, and format appropriatly
             // FIXME: When full i18n support is available, use the user's Locale
             // rather than the default Locale.
             NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
             DecimalFormat df = (DecimalFormat)nf;
             df.applyPattern("###,###.##");
             if (in < 1024)
             {
                 df.applyPattern("0");
                 return df.format(in) +  " " + "B";
             }
             else if (in < 1024000)
             {
                 in = in / 1024;
                 return df.format(in) + " " + "kB";
             }
             else if (in < 1024000000)
             {
                 in = in / 1024000;
                 return df.format(in) + " " + "MB";
             }
             else
             {
                 in = in / 1024000000;
                 return df.format(in) + " " + "GB";
             }
         }

    /**
     * Obtain a parameter from the given request as an int. <code>-1</code> is
     * returned if the parameter is garbled or does not exist.
     *
     * @param request
     *            the HTTP request
     * @param param
     *            the name of the parameter
     *
     * @return the integer value of the parameter, or -1
     */
    public static int getIntParameter(HttpServletRequest request, String param)
    {
        String val = request.getParameter(param);

        try
        {
            return Integer.parseInt(val.trim());
        }
        catch (Exception e)
        {
            // Problem with parameter
            return -1;
        }
    }

    /**
     * Obtain a parameter from the given request as a UUID. <code>null</code> is
     * returned if the parameter is garbled or does not exist.
     *
     * @param request
     *            the HTTP request
     * @param param
     *            the name of the parameter
     *
     * @return the integer value of the parameter, or -1
     */
    public static UUID getUUIDParameter(HttpServletRequest request, String param)
    {
        String val = request.getParameter(param);
        if (StringUtils.isEmpty(val))
        {
            return null;
        }

        try
        {
            return UUID.fromString(val.trim());
        }
        catch (Exception e)
        {
            // at least log this error to make debugging easier
            // do not silently return null only.
            log.warn("Unable to recoginze UUID from String \"" 
                    + val + "\". Will return null.", e);
            // Problem with parameter
            return null;
        }
    }
    
    /**
     * Obtain a List of UUID parameters from the given request as an UUID. null
     * is returned if parameter doesn't exist. <code>null</code> is returned in
     * position of the list if that particular value is garbled.
     *
     * @param request
     *            the HTTP request
     * @param param
     *            the name of the parameter
     *
     * @return list of UUID or null
     */
    public static List<UUID> getUUIDParameters(HttpServletRequest request,
            String param)
    {
        String[] request_values = request.getParameterValues(param);

        if (request_values == null)
        {
            return null;
        }

        List<UUID> return_values = new ArrayList<UUID>(request_values.length);

        for (String s : request_values)
        {
            try
            {
                return_values.add(UUID.fromString(s.trim()));
            }
            catch (Exception e)
            {
                // Problem with parameter, stuff null in the list
            	return_values.add(null);
            }
        }

        return return_values;
    }


    /**
     * Obtain an array of int parameters from the given request as an int. null
     * is returned if parameter doesn't exist. <code>-1</code> is returned in
     * array locations if that particular value is garbled.
     *
     * @param request
     *            the HTTP request
     * @param param
     *            the name of the parameter
     *
     * @return array of integers or null
     */
    public static int[] getIntParameters(HttpServletRequest request,
            String param)
    {
        String[] request_values = request.getParameterValues(param);

        if (request_values == null)
        {
            return null;
        }

        int[] return_values = new int[request_values.length];

        for (int x = 0; x < return_values.length; x++)
        {
            try
            {
                return_values[x] = Integer.parseInt(request_values[x]);
            }
            catch (Exception e)
            {
                // Problem with parameter, stuff -1 in this slot
                return_values[x] = -1;
            }
        }

        return return_values;
    }

    /**
     * Obtain a parameter from the given request as a boolean.
     * <code>false</code> is returned if the parameter is garbled or does not
     * exist.
     *
     * @param request
     *            the HTTP request
     * @param param
     *            the name of the parameter
     *
     * @return the integer value of the parameter, or -1
     */
    public static boolean getBoolParameter(HttpServletRequest request,
            String param)
    {
        return ((request.getParameter(param) != null) && request.getParameter(
                param).equals("true"));
    }

    /**
     * Get the button the user pressed on a submitted form. All buttons should
     * start with the text <code>submit</code> for this to work. A default
     * should be supplied, since often the browser will submit a form with no
     * submit button pressed if the user presses enter.
     *
     * @param request
     *            the HTTP request
     * @param def
     *            the default button
     *
     * @return the button pressed
     */
    public static String getSubmitButton(HttpServletRequest request, String def)
    {
        Enumeration e = request.getParameterNames();

        while (e.hasMoreElements())
        {
            String parameterName = (String) e.nextElement();

            if (parameterName.startsWith("submit"))
            {
                return parameterName;
            }
        }

        return def;
    }

    /**
     * Gets Maven version string of the source that built this instance.
     * @return string containing version, e.g. "1.5.2"; ends in "-SNAPSHOT" for development versions.
     */
    public static String getSourceVersion()
    {
        if (sourceVersion == null)
        {
            Properties constants = new Properties();

            InputStream cis = null;
            try
            {
                cis = Util.class.getResourceAsStream("/META-INF/maven/org.dspace/dspace-api/pom.properties");
                constants.load(cis);
            }
            catch(Exception e)
            {
                log.error(e.getMessage(),e);
            }
            finally
            {
                if (cis != null)
                {
                    try
                    {
                        cis.close();
                    }
                    catch (IOException e)
                    {
                        log.error("Unable to close input stream", e);
                    }
                }
            }

            sourceVersion = constants.getProperty("version", "none");
        }
        return sourceVersion;
    }

    /**
     * Get a list of all the respective "displayed-value(s)" from the given
     * "stored-value(s)" for a specific metadata field of a DSpace Item, by
     * reading submission-forms.xml
     * 
     * @param item
     *            The Dspace Item
     * @param values
     *            A Metadatum[] array of the specific "stored-value(s)"
     * @param schema
     *            A String with the schema name of the metadata field
     * @param element
     *            A String with the element name of the metadata field
     * @param qualifier
     *            A String with the qualifier name of the metadata field
     * @param locale locale
     * @return A list of the respective "displayed-values"
     * @throws SQLException if database error
     * @throws DCInputsReaderException if reader error
     */

    public static List<String> getControlledVocabulariesDisplayValueLocalized(
            Item item, List<MetadataValue> values, String schema, String element,
            String qualifier, Locale locale) throws SQLException,
            DCInputsReaderException
    {
        List<String> toReturn = new ArrayList<String>();
        DCInput myInputs = null;
        boolean myInputsFound = false;
        String formFileName = I18nUtil.getInputFormsFileName(locale);
        String col_handle = "";

        Collection collection = item.getOwningCollection();

        if (collection == null)
        {
            // set an empty handle so to get the default input set
            col_handle = "";
        }
        else
        {
            col_handle = collection.getHandle();
        }

        // Read the input form file for the specific collection
        DCInputsReader inputsReader = new DCInputsReader(formFileName);

        List<DCInputSet> inputSets = inputsReader.getInputsByCollectionHandle(col_handle);

		for (DCInputSet inputSet : inputSets) {
			// Replace the values of Metadatum[] with the correct ones in case
			// of
			// controlled vocabularies			
			String currentField = Utils.standardize(schema, element, qualifier, ".");

			if (inputSet != null) {

				int fieldsNums = inputSet.getNumberFields();

				for (int p = 0; p < fieldsNums; p++) {

					DCInput[] inputs = inputSet.getFields();

					if (inputs != null) {

						for (int i = 0; i < inputs.length; i++) {
							String inputField = Utils.standardize(inputs[i].getSchema(), inputs[i].getElement(),
									inputs[i].getQualifier(), ".");
							if (currentField.equals(inputField)) {

								myInputs = inputs[i];
								myInputsFound = true;
								break;

							}
						}
					}
					if (myInputsFound)
						break;
				}
			}

			if (myInputsFound) {

				for (MetadataValue value : values) {

					String pairsName = myInputs.getPairsType();
					String stored_value = value.getValue();
					String displayVal = myInputs.getDisplayString(pairsName, stored_value);

					if (displayVal != null && !"".equals(displayVal)) {

						toReturn.add(displayVal);
					}

				}
			}
		}
        return toReturn;
    }
}
