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
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.app.rest.model.OrcidTokenRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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
    private OrcidQueueService orcidQueueService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ItemService itemService;

    @RequestMapping(value = "/{itemId}", method = RequestMethod.GET)
    public void retrieveOrcidFromCode(HttpServletRequest request, HttpServletResponse response,
        @RequestParam(name = "code", required = false) String code, @PathVariable(name = "itemId") String itemId,
        @RequestParam(name = "url") String url) throws Exception {

        if (StringUtils.isNotEmpty(code)) {

            Context context = ContextUtil.obtainContext(request);
            Item item = itemService.findByIdOrLegacyId(context, itemId);

            OrcidTokenRest token = retrieveOrcidToken(code);

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

    private OrcidTokenRest retrieveOrcidToken(String code) {
        String tokenUrl = configurationService.getProperty("orcid-api.token-url");
        String clientId = configurationService.getProperty("orcid-api.application-client-id");
        String clientSecret = configurationService.getProperty("orcid-api.application-client-secret");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", "authorization_code");
        map.add("code", code);

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(map, headers);
        return restTemplate.postForObject(tokenUrl, tokenRequest, OrcidTokenRest.class);
    }
}
