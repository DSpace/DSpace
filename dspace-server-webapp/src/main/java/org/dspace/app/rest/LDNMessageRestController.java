/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_URN_UUID;

import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.LDNMessageEntityRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest Controller for requesting the reprocessing of LDNMessageEntity
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
@RestController
@RequestMapping("/api/" + LDNMessageEntityRest.CATEGORY + "/"
    + LDNMessageEntityRest.NAME_PLURALS + REGEX_REQUESTMAPPING_IDENTIFIER_AS_URN_UUID + "/enqueueretry")
public class LDNMessageRestController implements InitializingBean {

    private static final Logger log = LogManager.getLogger(LDNMessageRestController.class);

    @Autowired
    private ConverterService converterService;

    @Autowired
    private LDNMessageService ldnMessageService;

    @Autowired
    private Utils utils;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Override
    public void afterPropertiesSet() {
        discoverableEndpointsService.register(this,
            List.of(Link.of("/api/" + LDNMessageEntityRest.CATEGORY + "/"
                + LDNMessageEntityRest.NAME_PLURALS + "/{id}/enqueueretry",
                "enqueueretry")));
    }

    @PostMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> findByItem(@PathVariable("id") String id)
        throws SQLException, AuthorizeException, JsonProcessingException {

        Context context = ContextUtil.obtainCurrentRequestContext();
        LDNMessageEntity ldnMessageEntity = ldnMessageService.find(context, id);
        if (ldnMessageEntity == null) {
            throw new ResourceNotFoundException("No such item: " + id);
        }
        ldnMessageEntity.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_QUEUED_FOR_RETRY);
        ldnMessageService.update(context, ldnMessageEntity);

        LDNMessageEntityRest resultRequestStatusRests = converterService.toRest(
            ldnMessageEntity, utils.obtainProjection());

        context.complete();
        String result = new ObjectMapper()
            .writerWithDefaultPrettyPrinter().writeValueAsString(resultRequestStatusRests);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
