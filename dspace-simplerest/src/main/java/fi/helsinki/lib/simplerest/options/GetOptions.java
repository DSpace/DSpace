/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest.options;

import org.restlet.Response;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.util.Series;

/**
 *
 * @author moubarik
 */
public abstract class GetOptions {
    public static void allowAccess(Response resp){
        if(resp == null)
            return;
        Series<Header> responseHeaders = (Series<Header>) resp.getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if (responseHeaders == null) {
            responseHeaders = new Series(Header.class);
            resp.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS,
                    responseHeaders);
        }
        responseHeaders.add(new Header("Access-Control-Allow-Origin", "*"));
    }
}
