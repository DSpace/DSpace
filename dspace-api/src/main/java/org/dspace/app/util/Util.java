/*
 * Util.java
 *
 * Version: $Revision: $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.dspace.core.Constants;


/**
 * Miscellaneous utility methods
 * 
 * @author Robert Tansley
 * @author Mark Diggory
 * @version $Revision: $
 */
public class Util {

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
	 *		  input string to encode
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
	  * @param in The number to covnert
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

}
