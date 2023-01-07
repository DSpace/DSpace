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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shibboleth authentication header abstraction for DSpace
 *
 * Parses all headers in ctor.
 * Class is copied from UFAL/CLARIN-DSPACE (https://github.com/ufal/clarin-dspace) and modified by
 * @author Milan Majchrak (milan.majchrak at dataquest dot sk)
 */
public class ShibHeaders {
    // constants
    //
    private static final String header_separator_ = ";";

    // variables
    //
    private static final Logger log = LogManager.getLogger(ShibHeaders.class);

    private Headers headers_ = null;

    // ctor
    //

    public ShibHeaders(HttpServletRequest request, String[] interesting) {
        initialise(request, Arrays.asList(interesting));
    }
    public ShibHeaders(HttpServletRequest request, String interesting) {
        initialise(request, Arrays.asList(interesting));
    }
    public ShibHeaders(HttpServletRequest request) {
        initialise(request, null);
    }

    public ShibHeaders(String shibHeaders) {
        initialise(shibHeaders);
    }

    // inits
    //

    public void initialise(HttpServletRequest request, List<String> interesting) {
        headers_ = new Headers(request, header_separator_, interesting);
    }

    public void initialise(String shibHeaders) {
        headers_ = new Headers(shibHeaders, header_separator_);
    }

    //
    //

    public String get_idp() {
        return get_single("Shib-Identity-Provider");
    }

    // list like interface (few things are copied from ShibAuthenetication.java)
    //

    /**
     * Find a particular Shibboleth header value and return the all values.
     * The header name uses a bit of fuzzy logic, so it will first try case
     * sensitive, then it will try lowercase, and finally it will try uppercase.
     */
    public List<String> get(String key) {
        List<String> values = headers_.get(key);
        if (values != null && values.isEmpty()) {
            values = null;
        }
        return values;
    }

    /**
     * Find a particular Shibboleth header value and return the first value.
     * 
     * Shibboleth attributes may contain multiple values separated by a
     * semicolon. This method will return the first value in the attribute. If
     * you need multiple values use findMultipleHeaders instead.
     */
    public String get_single(String name) {
        List<String> values = get(name);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return null;
    }

    /**
     * Get keys which starts with prefix.
     * @return
     */
    public List<String> get_prefix_keys(String prefix) {
        List<String> keys = new ArrayList<String>();
        for (String k : headers_.get().keySet()) {
            if (k.toLowerCase().startsWith(prefix)) {
                keys.add(k);
            }
        }
        return keys;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for ( Map.Entry<String, List<String>> i : headers_.get().entrySet() ) {
            if (StringUtils.equals("cookie", i.getKey())) {
                continue;
            }
            sb.append(String.format("%s=%s\n",
                    i.getKey(), StringUtils.join(i.getValue().toArray(), ",") ));
        }
        return sb.toString();
    }

    //
    //

    public void log_headers() {
        for ( Map.Entry<String, List<String>> i : headers_.get().entrySet() ) {
            log.debug(String.format("header:%s=%s",
                    i.getKey(), StringUtils.join(i.getValue().toArray(), ",") ));
        }
    }
}
