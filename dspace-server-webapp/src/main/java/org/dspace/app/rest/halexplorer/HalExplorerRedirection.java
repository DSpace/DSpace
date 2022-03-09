/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.halexplorer;


import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * This Controller ensures that HAL Explorer URLs are redirected to the /api path (of our REST API)
 * and always respond with HAL format.
 * <P>
 * It overrides the default Hal Explorer redirects defined by Spring Data Rest in
 * https://github.com/spring-projects/spring-data-rest/blob/main/spring-data-rest-hal-explorer/src/main/java/org/springframework/data/rest/webmvc/halexplorer/HalExplorer.java
 * (As this HalExplorer class in Spring Data Rest is private, we cannot override it directly at this time.)
 */
@Controller
@RequestMapping("/")
public class HalExplorerRedirection {

    static final String EXPLORER = "/explorer";
    static final String INDEX = "/index.html";

    @GetMapping()
    public RedirectView index(HttpServletRequest request) {
        return getRedirectView(request, false);
    }

    /**
     * Redirects to the actual {@code index.html}.
     *
     * @return
     */
    @GetMapping(EXPLORER)
    public RedirectView explorer(HttpServletRequest request) {
        return getRedirectView(request, request.getRequestURI().endsWith(EXPLORER));
    }

    /**
     * Returns the View to redirect to to access the HAL explorer.
     *
     * @param request must not be {@literal null}.
     * @param explorerRelative
     * @return
     */
    private RedirectView getRedirectView(HttpServletRequest request, boolean explorerRelative) {
        String contextPath = request.getContextPath();
        // Get path of our "server" webapp. It may be a root (empty) path.
        String serverPath = contextPath.length() > 0 ? contextPath : "";

        StringBuilder redirectPath = new StringBuilder();
        redirectPath.append(serverPath);

        if (!explorerRelative) {
            redirectPath.append(EXPLORER);
        }

        redirectPath.append(INDEX);
        // At this point, the redirectPath will be [server-path]/explorer/index.html, which is the Hal Explorer

        // Finally, append default params on the redirect URL. For DSpace, we set these params by default:
        // - "key0=Accept&hval0=application/hal+json" => Ensures we default all requests to "Accept" HAL format
        //    (When not specified, the Hal Explorer includes an Accepts for HAL-FORMS, which we don't support)
        // - "uri=[server-path]/api" => Ensure we start at the "/api" endpoint (the root of our REST API)
        redirectPath.append(
            String.format("#hkey0=Accept&hval0=application/hal+json&uri=%s", serverPath + "/api"));

        return new RedirectView(redirectPath.toString());
    }
}