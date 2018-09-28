/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.rest.repository.RequestItemRepositoryIT;
import org.dspace.app.rest.model.RequestItemRest;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class RequestCopyMatcher {
    private RequestCopyMatcher() { }

    public static Matcher<? super Object> matchRequestCopy(RequestItem request) {
        return allOf(
                hasJsonPath("$.bitstream", Matchers.not(Matchers.empty())),
                hasJsonPath("$.item", Matchers.not(Matchers.empty())),
                hasJsonPath("$.allFiles", is(String.valueOf(request.isAllfiles()))),
                hasJsonPath("$.reqEmail", is(request.getReqEmail())),
                hasJsonPath("$.reqName", is(request.getReqName())),
                hasJsonPath("$.reqMessage", is(request.getReqMessage())),
                hasJsonPath("$.type", is(RequestItemRest.NAME)),
                hasJsonPath("$._embedded.schema", Matchers.not(Matchers.empty())),
                hasJsonPath("$._links.schema.href",
                        Matchers.containsString(RequestItemRepositoryIT.URI_ROOT)),
                hasJsonPath("$._links.self.href",
                        Matchers.containsString(RequestItemRepositoryIT.URI_ROOT))
        );
    }
}
