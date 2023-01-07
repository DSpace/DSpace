/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/* Created for LINDAT/CLARIN */
package org.dspace.authenticate.clarin;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * Helper class for request headers.
 * Class is copied from UFAL/CLARIN-DSPACE (https://github.com/ufal/clarin-dspace) and modified by
 * @author Milan Majchrak (milan.majchrak at dataquest dot sk)
 */
public class Headers {

    // variables
    //

    private HashMap<String, List<String>> headers_ = new HashMap<String, List<String>>();
    private String header_separator_ = null;


    // ctors
    //
    public Headers(HttpServletRequest request, String header_separator ) {
        initialise(request, header_separator, null);
    }

    public Headers(String shibHeaders, String header_separator ) {
        initialise(shibHeaders, header_separator);
    }

    public Headers(HttpServletRequest request, String header_separator, List<String> interesting ) {
        initialise(request, header_separator, interesting);
    }

    public void initialise(HttpServletRequest request, String header_separator, List<String> interesting) {
        header_separator_ = header_separator;
        //
        Enumeration e_keys = request.getHeaderNames();
        while (e_keys.hasMoreElements()) {
            String key = (String) e_keys.nextElement();
            if ( interesting != null && !interesting.contains(key) ) {
                continue;
            }

            List<String> vals = new ArrayList<String>();
            Enumeration e_vals = request.getHeaders(key);
            while (e_vals.hasMoreElements()) {
                String values = (String)e_vals.nextElement();
                vals.addAll( header2values(values) );
            }

            // make it case-insensitive
            headers_.put(key.toLowerCase(), vals);
        }
    }

    public void initialise(String shibHeaders, String header_separator) {
        header_separator_ = header_separator;
        //
        for (String line : shibHeaders.split("\n")) {
            String key = " ";
            try {
                String key_value[] = line.split("=");
                key = key_value[0].trim();
                headers_.put(key, List.of(key_value[1]));
            } catch (Exception ignore) {
                //
            }
        }
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (String header : headers_.keySet()) {
            ret.append(header).append(" = ").append(headers_.get(header).toString()).append("\n");
        }
        return ret.toString();
    }

    //
    //

    public Map<String, List<String>> get() {
        return headers_;
    }

    public List<String> get(String key) {
        return headers_.get(key.toLowerCase());
    }

    // helper methods (few things are copied from ShibAuthenetication.java)
    //

    private String unescape(String value) {
        return value.replaceAll("\\\\" + header_separator_, header_separator_);
    }


    private List<String> header2values(String header) {
        // Shibboleth attributes are separated by semicolons (and semicolons are
        // escaped with a backslash). So here we will scan through the string and
        // split on any unescaped semicolons.
        List<String> values = new ArrayList<String>();

        if ( header == null ) {
            return values;
        }

        int idx = 0;
        do {
            idx = header.indexOf(header_separator_,idx);

            if ( idx == 0 ) {
                // if the string starts with a semicolon just remove it. This will
                // prevent an endless loop in an error condition.
                header = header.substring(1,header.length());

            } else if (idx > 0 && header.charAt(idx - 1) == '\\' ) {
                // found an escaped semicolon; move on
                idx++;
            } else if ( idx > 0) {
                // First extract the value and store it on the list.
                String value = header.substring(0, idx);
                value = unescape(value);
                values.add(value);
                // Next, remove the value from the string and continue to scan.
                header = header.substring(idx + 1, header.length());
                idx = 0;
            }
        } while (idx >= 0);

        // The last attribute will still be left on the values string, put it
        // into the list.
        if (header.length() > 0) {
            header = unescape(header);
            values.add(header);
        }

        return values;
    }
}
