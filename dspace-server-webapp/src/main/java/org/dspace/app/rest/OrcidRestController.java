/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that store ORCID infos and handles redirect.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@RequestMapping(value = "/api/" + RestModel.CRIS + "/orcid")
@RestController
public class OrcidRestController {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private ItemService itemService;

    @RequestMapping(value = "/{itemId}", method = RequestMethod.GET)
    public void retrieveOrcidFromCode(HttpServletRequest request, HttpServletResponse response,
        @RequestParam(name = "code", required = false) String code, @PathVariable(name = "itemId") String itemId,
        @RequestParam(name = "url") String url) throws Exception {

        if (StringUtils.isNotEmpty(code)) {

            Context context = ContextUtil.obtainContext(request);
            Item item = itemService.findByIdOrLegacyId(context, itemId);

            OrcidTokenResponseDTO token = orcidClient.getAccessToken(code);

            itemService.addMetadata(context, item, "person", "identifier", "orcid", null, token.getOrcid());
            itemService.addMetadata(context, item, "cris", "orcid", "access-token", null, token.getAccessToken());
            itemService.addMetadata(context, item, "cris", "orcid", "refresh-token", null, token.getRefreshToken());
            String[] scopes = StringUtils.isEmpty(token.getScope()) ? new String[] {} : token.getScope().split(" ");
            for (String scope : scopes) {
                itemService.addMetadata(context, item, "cris", "orcid", "scope", null, scope);
            }

            context.complete();

        }

        String dspaceUiUrl = configurationService.getProperty("dspace.ui.url");
        response.sendRedirect(dspaceUiUrl + url);
    }
}
