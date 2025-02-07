/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.dspace.app.util.Util.getSourceVersion;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.RootRest;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class read the core configuration properties and constructs a RootRest instance to return
 */
@Component
public class RootConverter {

    @Autowired
    private ConfigurationService configurationService;

    public RootRest convert(HttpServletRequest request) {
        RootRest rootRest = new RootRest();
        rootRest.setDspaceName(configurationService.getProperty("dspace.name"));
        rootRest.setDspaceUI(configurationService.getProperty("dspace.ui.url"));
        String requestUrl = request.getRequestURL().toString();
        String dspaceUrl = configurationService.getProperty("dspace.server.url");
        String dspaceSSRUrl = configurationService.getProperty("dspace.server.ssr.url", dspaceUrl);
        if (!dspaceUrl.equals(dspaceSSRUrl) && requestUrl.startsWith(dspaceSSRUrl)) {
            rootRest.setDspaceServer(dspaceSSRUrl);
        } else {
            rootRest.setDspaceServer(dspaceUrl);
        }
        rootRest.setDspaceVersion("DSpace " + getSourceVersion());
        return rootRest;
    }


}
