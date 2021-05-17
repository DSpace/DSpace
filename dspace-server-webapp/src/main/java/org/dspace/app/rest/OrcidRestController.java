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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.orcid.service.OrcidSynchronizationService;
import org.dspace.app.orcid.service.OrcidWebhookService;
import org.dspace.app.orcid.webhook.OrcidWebhookAction;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that store ORCID infos and handles redirect.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@RequestMapping(value = "/api/" + RestModel.CRIS + "/orcid")
@RestController
public class OrcidRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidRestController.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidSynchronizationService orcidSynchronizationService;

    @Autowired
    private OrcidWebhookService orcidWebhookService;

    @Autowired(required = false)
    private List<OrcidWebhookAction> orcidWebhookActions;

    @PostConstruct
    private void postConstruct() {
        if (orcidWebhookActions == null) {
            orcidWebhookActions = Collections.emptyList();
        }
    }

    @GetMapping(value = "/{itemId}")
    public void linkProfileFromCode(HttpServletRequest request, HttpServletResponse response,
        @RequestParam(name = "code") String code, @PathVariable(name = "itemId") String itemId,
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

    @PostMapping(value = "/{orcid}/webhook/{token}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void webhook(HttpServletRequest request, @PathVariable(name = "orcid") String orcid,
        @PathVariable(name = "token") String token) {

        String storedToken = configurationService.getProperty("orcid.webhook.registration-token");
        if (!StringUtils.equals(token, storedToken)) {
            LOGGER.warn("Received a webhook callback with a wrong token: " + token);
            return;
        }

        Context context = ContextUtil.obtainContext(request);

        try {
            context.turnOffAuthorisationSystem();
            performWebhookActions(orcid, context);
        } catch (Exception ex) {
            LOGGER.error("An error occurs while processing the webhook call from ORCID", ex);
        } finally {
            context.restoreAuthSystemState();
        }

        try {
            context.complete();
        } catch (SQLException e) {
            LOGGER.error("An error occurs closing the DSpace context", e);
        }

    }

    private void performWebhookActions(String orcid, Context context) throws SQLException, AuthorizeException {
        Iterator<Item> iterator = orcidSynchronizationService.findProfilesByOrcid(context, orcid);
        if (IteratorUtils.isEmpty(iterator)) {
            LOGGER.warn("Received a webhook call from ORCID with an id not associated with any profile: " + orcid);
            orcidWebhookService.unregister(context, orcid);
            return;
        }

        while (iterator.hasNext()) {
            Item profile = iterator.next();
            orcidWebhookActions.forEach(plugin -> plugin.perform(context, profile, orcid));
            itemService.update(context, profile);
        }
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
