/**
 A RESTful web service on top of DSpace.
 The contents of this file are subject to the license and copyright
 detailed in the LICENSE and NOTICE files at the root of the source
 tree and available online at
 http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.MapVerifier;
import org.restlet.data.Method;

public class LaxMapVerifier extends MapVerifier {

    @Override
    public int verify(Request request, Response response) {
        if (request.getMethod() == Method.GET || request.getMethod() == Method.OPTIONS) {
            return RESULT_VALID;
        }
        else {
            return super.verify(request, response);
        }
    }
}