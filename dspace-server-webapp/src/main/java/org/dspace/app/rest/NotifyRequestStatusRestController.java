/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.model.NotifyRequestStatus;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.NotifyRequestStatusRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Rest Controller for NotifyRequestStatus targeting items
 *
 * @author Francesco Bacchelli (francesco.bacchelli at 4science dot it)
 */
@RestController
@RequestMapping("/api/" + NotifyRequestStatusRest.CATEGORY + "/" + NotifyRequestStatusRest.PLURAL_NAME +
    REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID)
public class NotifyRequestStatusRestController implements InitializingBean {

    private static final Logger log = LogManager.getLogger(NotifyRequestStatusRestController.class);

    @Autowired
    private ConverterService converterService;

    @Autowired
    private Utils utils;

    @Autowired
    private LDNMessageService ldnMessageService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Override
    public void afterPropertiesSet() {
        discoverableEndpointsService.register(this,
            List.of(Link.of("/api/" + NotifyRequestStatusRest.CATEGORY + "/" + NotifyRequestStatusRest.NAME,
                NotifyRequestStatusRest.NAME)));
    }

    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public ResponseEntity<String> findByItem(@PathVariable UUID uuid)
        throws SQLException, AuthorizeException, JsonProcessingException {

        Context context = ContextUtil.obtainCurrentRequestContext();
        Item item = itemService.find(context, uuid);
        if (item == null) {
            throw new ResourceNotFoundException("No such item: " + uuid);
        }
        EPerson currentUser = context.getCurrentUser();
        if (!currentUser.equals(item.getSubmitter()) && !authorizeService.isAdmin(context)) {
            throw new AuthorizeException("User unauthorized");
        }
        NotifyRequestStatus resultRequests = ldnMessageService.findRequestsByItem(context, item);
        NotifyRequestStatusRest resultRequestStatusRests = converterService.toRest(
            resultRequests, utils.obtainProjection());

        context.complete();
        String result = new ObjectMapper()
            .writerWithDefaultPrettyPrinter().writeValueAsString(resultRequestStatusRests);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
