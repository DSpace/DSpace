/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.util.List;

import com.apicatalog.jsonld.StringUtils;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.springframework.stereotype.Component;

/**
 * Utility component for extracting workspace item identifiers from REST API URIs.
 * <p>
 * This bean is registered as {@code workflowSecurityUtils} so that it can be
 * referenced in Spring Security SpEL expressions (e.g.
 * {@code @PreAuthorize("hasPermission(@workflowSecurityUtils.parseIdFromUriList(#stringList), ...)")})
 * to resolve workspace item IDs before permission evaluation.
 * </p>
 * <p>
 * It is also injected into
 * {@link org.dspace.app.rest.submit.SubmissionService#createWorkflowItem} to
 * extract the workspace item ID from the URI provided in the request body.
 * </p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
@Component("workflowSecurityUtils")
public class WorkflowSecurityUtils {

    /**
     * Parse a workspace item integer ID from a full REST API URI.
     * <p>
     * The method expects a URI matching the pattern
     * {@code /api/submission/workspaceitems/{id}} and returns the trailing
     * numeric segment. If the URI is blank, does not match the expected
     * pattern, or contains a non-numeric ID, {@code null} is returned.
     * </p>
     *
     * @param requestUri the full REST API URI containing the workspace item ID
     *                   (e.g. {@code "/api/submission/workspaceitems/42"})
     * @return the parsed workspace item ID, or {@code null} if the URI is
     *         invalid or cannot be parsed
     */
    public Integer parseIdFromUri(String requestUri) {
        if (StringUtils.isBlank(requestUri)) {
            return null;
        }

        String regex = "/api/" + WorkspaceItemRest.CATEGORY + "/" + WorkspaceItemRest.PLURAL_NAME + "/";
        String[] split = requestUri.split(regex, 2);

        if (split.length != 2) {
            return null;
        }

        try {
            return Integer.parseInt(split[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse a workspace item ID from the first element of a URI list.
     * <p>
     * This convenience method is designed for use in Spring Security SpEL
     * expressions where the request body is bound as a {@code List<String>}.
     * It delegates to {@link #parseIdFromUri(String)} using the first element
     * of the list.
     * </p>
     *
     * @param uriList the list of URIs from the request body; only the first
     *                element is used
     * @return the parsed workspace item ID, or {@code null} if the list is
     *         {@code null}, empty, or the first URI cannot be parsed
     * @see #parseIdFromUri(String)
     */
    public Integer parseIdFromUriList(List<String> uriList) {
        if (uriList == null || uriList.isEmpty()) {
            return null;
        }
        return parseIdFromUri(uriList.getFirst());
    }
}