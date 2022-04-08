/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.net.URI;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.orcid.service.OrcidSynchronizationService;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that store ORCID infos and handles redirect.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@RequestMapping(value = "/api/" + RestModel.EPERSON + "/orcid")
@RestController
public class OrcidRestController {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidSynchronizationService orcidSynchronizationService;

    @GetMapping(value = "/{itemId}")
    public void linkProfileFromCode(HttpServletRequest request, HttpServletResponse response,
                                    @RequestParam(name = "code") String code,
                                    @PathVariable(name = "itemId") String itemId,
                                    @RequestParam(name = "url") String url) throws Exception {

        Context context = ContextUtil.obtainContext(request);

        ResearcherProfile profile = findResearcherProfile(context, UUIDUtils.fromString(itemId))
            .orElseThrow(() -> new ResourceNotFoundException("No profile found by item id: " + itemId));

        OrcidTokenResponseDTO token = orcidClient.getAccessToken(code);

        orcidSynchronizationService.linkProfile(context, profile.getItem(), token);

        context.complete();

        URI dspaceUiUrl = new URI(configurationService.getProperty("dspace.ui.url"));
        response.sendRedirect(dspaceUiUrl.resolve(url).toString());
    }

    private Optional<ResearcherProfile> findResearcherProfile(Context context, UUID itemId) throws SQLException {
        return Optional.ofNullable(itemService.find(context, itemId))
                       .map(ResearcherProfile::new);
    }

    public OrcidClient getOrcidClient() {
        return orcidClient;
    }

    public void setOrcidClient(OrcidClient orcidClient) {
        this.orcidClient = orcidClient;
    }

}
