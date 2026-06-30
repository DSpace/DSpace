/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.service.CrisLayoutTabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller is used to manage metadatasecurity association in tabs.
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@RestController
@RequestMapping("/api/layout/tabs" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT + "/securitymetadata")
public class CrisLayoutTabMetadatasecurityRestController {

    private static Logger log = LogManager.getLogger(CrisLayoutTabMetadatasecurityRestController.class);

    @Autowired
    private Utils utils;
    @Autowired
    private CrisLayoutTabService tabService;
    @Autowired
    private MetadataFieldService mfService;

    @RequestMapping(
            method = RequestMethod.POST,
            consumes = {"text/uri-list"})
    @PreAuthorize("hasAuthority('ADMIN')")
    public void addMetadatasecurity(
            @PathVariable(name = "id", required = true) Integer idTab,
            @PathVariable(required = false) Integer position,
            HttpServletResponse response, HttpServletRequest request) {
        Context context = ContextUtil.obtainContext(request);
        log.info("Start method for add metadatasecurity association to tab with tabId <" + idTab + ">");
        List<String> links = utils.getStringListFromRequest(request);
        CrisLayoutTab tab = null;
        try {
            tab = tabService.find(context, idTab);
            if (tab == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (links == null || links.isEmpty()) {
                response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
                return;
            }
            Set<MetadataField> metadataFields = getMedatafieldList(context, links);
            if (metadataFields != null && !metadataFields.isEmpty()) {
                tab.addMetadataSecurityFields(metadataFields);
                tabService.update(context, tab);
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                context.commit();
            } else {
                response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
                return;
            }
        } catch (Exception e) {
            log.error("An error occured in method for add securitymetadata relation in tabs, idTab <" + idTab + ">", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(
            method = RequestMethod.DELETE,
            value = "/{mf_id:\\d+}" )
    @PreAuthorize("hasAuthority('ADMIN')")
    public void removeMetadatasecurity(
            @PathVariable(name = "id", required = true) Integer idTab,
            @PathVariable(name = "mf_id", required = true) Integer metadatafieldId,
            HttpServletResponse response, HttpServletRequest request) {
        Context context = ContextUtil.obtainContext(request);
        log.info("Start method for remove metadatasecurity association to tab with tabId <" + idTab + ">");
        CrisLayoutTab tab = null;
        try {
            tab = tabService.find(context, idTab);
            if (tab == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Set<MetadataField> fields = tab.getMetadataSecurityFields();
            if (fields != null && !fields.isEmpty()) {
                boolean found = false;
                for (Iterator<MetadataField> it = fields.iterator();it.hasNext(); ) {
                    MetadataField mf = it.next();
                    if (mf.getID().equals(metadatafieldId)) {
                        it.remove();
                        found = true;
                    }
                }
                if (found) {
                    tabService.update(context, tab);
                    context.commit();
                }
            }
        } catch (Exception e) {
            log.error("An error occured in method for remove"
                    + "securitymetadata relation in tabs, idTab <" + idTab + ">", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private Set<MetadataField> getMedatafieldList(Context ctx, List<String> links) throws Exception {
        Set<MetadataField> metadataFields = new HashSet<>();
        for (String link: links) {
            if (link.endsWith("/")) {
                link = link.substring(0, link.length() - 1);
            }
            String metadataFieldId = link.substring(link.lastIndexOf('/') + 1);
            Integer iMetadataFieldId = null;
            try {
                iMetadataFieldId = Integer.decode(metadataFieldId);
            } catch (Exception e) {
                log.error("Error to decode the metadataFieldId for string <" + metadataFieldId + ">", e);
            }
            if (iMetadataFieldId != null) {
                MetadataField mf = mfService.find(ctx, iMetadataFieldId);
                if (mf != null) {
                    metadataFields.add(mf);
                } else {
                    log.warn("WARNING! The MetadataField with id <" + iMetadataFieldId + "> not found!");
                }
            }
        }
        return metadataFields;
    }
}
