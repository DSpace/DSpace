/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.matcher.DateMatcher.dateMatcher;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.RequestItemRepositoryIT;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Compare {@link RequestItem}s.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class RequestCopyMatcher {
    private RequestCopyMatcher() { }

    public static Matcher<? super Object> matchRequestCopy(RequestItem request) {
        return allOf(
                //hasJsonPath("$._links.bitstream", Matchers.not(Matchers.empty())),
                //hasJsonPath("$._links.item", Matchers.not(Matchers.empty())),
                hasJsonPath("$.allfiles", is(request.isAllfiles())),
                hasJsonPath("$.request_email", is(request.getReqEmail())),
                hasJsonPath("$.request_name", is(request.getReqName())),
                hasJsonPath("$.request_message", is(request.getReqMessage())),
                hasJsonPath("$.request_date", dateMatcher(request.getRequest_date())),
                hasJsonPath("$.accept_request", is(request.isAccept_request())),
                hasJsonPath("$.decision_date", dateMatcher(request.getDecision_date())),
                hasJsonPath("$.expires", dateMatcher(request.getExpires())),
                hasJsonPath("$.type", is(RequestItemRest.NAME)),
                hasJsonPath("$._links.self.href",
                        Matchers.containsString(RequestItemRepositoryIT.URI_ROOT))
        );
    }
}
