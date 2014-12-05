/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import org.apache.log4j.Logger;
import org.dspace.core.Constants;

import javax.servlet.http.HttpServletRequest;
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

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.I18nUtil;


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
         */
        public static String encodeBitstreamName(String stringIn, String encoding) throws java.io.UnsupportedEncodingException {
            // FIXME: This should be moved elsewhere, as it is used outside the UI
            String stringOut = java.net.URLEncoder.encode(stringIn, encoding);
            return stringOut;
        }

        /** Version of encodeBitstreamName with one parameter, uses default encoding
         * <P>
         * @param stringIn
         *                input string to encode
         * @return the encoded string
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
     * reading input-forms.xml
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
     * @return A list of the respective "displayed-values"
     */

    public static List<String> getControlledVocabulariesDisplayValueLocalized(
            Item item, Metadatum[] values, String schema, String element,
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

        DCInputSet inputSet = inputsReader.getInputs(col_handle);

        // Replace the values of Metadatum[] with the correct ones in case of
        // controlled vocabularies
        String currentField = schema + "." + element
                + (qualifier == null ? "" : "." + qualifier);

        if (inputSet != null)
        {

            int pageNums = inputSet.getNumberPages();

            for (int p = 0; p < pageNums; p++)
            {

                DCInput[] inputs = inputSet.getPageRows(p, false, false);

                if (inputs != null)
                {

                    for (int i = 0; i < inputs.length; i++)
                    {
                        String inputField = inputs[i].getSchema()
                                + "."
                                + inputs[i].getElement()
                                + (inputs[i].getQualifier() == null ? "" : "."
                                        + inputs[i].getQualifier());
                        if (currentField.equals(inputField))
                        {

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

        if (myInputsFound)
        {

            for (int j = 0; j < values.length; j++)
            {

                String pairsName = myInputs.getPairsType();
                String stored_value = values[j].value;
                String displayVal = myInputs.getDisplayString(pairsName,
                        stored_value);

                if (displayVal != null && !"".equals(displayVal))
                {

                    toReturn.add(displayVal);
                }

            }
        }

        return toReturn;
    }
}
