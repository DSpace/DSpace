/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.CrisLayoutTab2Box;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.layout.service.CrisLayoutTabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller is used to manage boxes association in tabs.
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@RestController
@RequestMapping("/api/layout/tabs" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT + "/boxes")
public class CrisLayoutTabBoxesRestController {

    private static final Logger log = Logger.getLogger(CrisLayoutTabBoxesRestController.class);

    @Autowired
    private Utils utils;
    @Autowired
    private CrisLayoutBoxService boxService;
    @Autowired
    private CrisLayoutTabService tabService;

    @RequestMapping(
            method = RequestMethod.POST,
            consumes = {"text/uri-list"},
            value = {"/{position:\\d+}", ""} )
    @PreAuthorize("hasAuthority('ADMIN')")
    public void addBox(
            @PathVariable(name = "id", required = true) Integer idTab,
            @PathVariable(required = false) Integer position,
            HttpServletResponse response, HttpServletRequest request) {
        Context context = ContextUtil.obtainContext(request);
        log.info("Start method for add box association to tab with tabId <" + idTab + ">");
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
            List<CrisLayoutBox> boxes = getBoxList(context, links);
            if (boxes != null && !boxes.isEmpty()) {
                int idx = 0;
                for (CrisLayoutBox box: boxes) {
                    if (!tab.getEntity().getID().equals(box.getEntitytype().getID())) {
                        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
                        return;
                    }
                    tab.addBox(box, position == null ? null : position + idx++);
                }
                tabService.update(context, tab);
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                context.commit();
            }
        } catch (Exception e) {
            log.error("An error occured in method for add box relation in tabs, idTab <" + idTab + ">", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(
            method = RequestMethod.DELETE,
            value = "/{boxId:\\d+}" )
    @PreAuthorize("hasAuthority('ADMIN')")
    public void removeBox(
            @PathVariable(name = "id", required = true) Integer idTab,
            @PathVariable(required = true) Integer boxId,
            HttpServletResponse response, HttpServletRequest request) {
        Context context = ContextUtil.obtainContext(request);
        log.info("Start method for remove box association to tab with tabId <" + idTab + ">");
        CrisLayoutTab tab = null;
        try {
            tab = tabService.find(context, idTab);
            if (tab == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            List<CrisLayoutTab2Box> tab2box = tab.getTab2Box();
            if (tab2box != null ) {
                for (CrisLayoutTab2Box t2b: tab2box) {
                    if (t2b.getId().getCrisLayoutBoxId() == boxId) {
                        tab.removeBox(t2b.getId().getCrisLayoutBoxId());
                        context.commit();
                        break;
                    }
                }
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (Exception e) {
            log.error("An error occured in method for remove box relation in tabs, idTab <" + idTab + ">", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private List<CrisLayoutBox> getBoxList(Context ctx, List<String> links) throws Exception {
        List<CrisLayoutBox> boxes = new ArrayList<>();
        for (String link: links) {
            if (link.endsWith("/")) {
                link = link.substring(0, link.length() - 1);
            }
            String boxId = link.substring(link.lastIndexOf('/') + 1);
            Integer iBoxId = null;
            try {
                iBoxId = Integer.decode(boxId);
            } catch (Exception e) {
                log.error("Error to decode the boxId for string <" + boxId + ">", e);
            }
            if (iBoxId != null) {
                CrisLayoutBox box = boxService.find(ctx, iBoxId);
                if (box != null) {
                    boxes.add(box);
                } else {
                    log.warn("WARNING! The box with id <" + iBoxId + "> not found!");
                }
            }
        }
        return boxes;
    }
}
