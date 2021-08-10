/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.SubscriptionRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.SubscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * This is the repository responsible to manage SubscriptionRest object
 *
 * @author Alba Aliu at atis.al
 */

@Component(SubscriptionRest.CATEGORY + "." + SubscriptionRest.NAME)
public class SubscriptionRestRepository extends DSpaceRestRepository<SubscriptionRest, Integer> {
    private static final Logger log = LogManager.getLogger();
    @Autowired
    AuthorizeService authorizeService;
    @Autowired
    SubscribeService subscribeService;
    @Autowired
    protected ConverterService converter;
    @Autowired
    protected EPersonService personService;
    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;


    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public SubscriptionRest findOne(Context context, Integer id) {
        try {
            Subscription subscription = subscribeService.findById(context, id);
            if (subscription == null) {
                throw new ResourceNotFoundException("The subscription for ID: " + id + " could not be found");
            }
            return converter.toRest(subscription, utils.obtainProjection());
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException.getMessage(), sqlException);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<SubscriptionRest> findAll(Context context, Pageable pageable) {
        try {
            List<Subscription> subscriptionList = subscribeService.findAll(context);
            return converter.toRestPage(subscriptionList, pageable, utils.obtainProjection());
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException.getMessage(), sqlException);
        }
    }

    @SearchRestMethod(name = "findByEPerson")
    public Page<SubscriptionRest> findAllByEPerson(Context context, String id) {
        try {
            EPerson ePerson = personService.findByNetid(context, id);
            if (context.getCurrentUser().equals(ePerson) || authorizeService.isAdmin(context, ePerson)) {
                List<Subscription> subscriptionList = subscribeService.getSubscriptions(context, ePerson);
                return converter.toRest(subscriptionList, utils.obtainProjection());
            } else {
                throw new AuthorizeException("Only admin or e-person themselves can search for it's subscription");
            }
        } catch (SQLException | AuthorizeException sqlException) {
            throw new RuntimeException(sqlException.getMessage(), sqlException);
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    protected SubscriptionRest createAndReturn(Context context) {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        SubscriptionRest subscriptionRest = null;
        DSpaceObject dSpaceObject = null;
        try {
            subscriptionRest = mapper.readValue(req.getInputStream(), SubscriptionRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("error parsing the body");
        }
        try {
//           EPerson ePerson = personService.findByNetid(context, subscriptionRest.getePerson().getNetid()) ;
            Subscription subscription = subscribeService.subscribe(context, null,
                    dSpaceObject,
                    subscriptionRest.getSubscriptionParameterList(),
                    subscriptionRest.getType());
            return converter.toRest(subscription, utils.obtainProjection());
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    protected SubscriptionRest put(Context context, HttpServletRequest request, String apiCategory, String model,
                                   Integer id, JsonNode jsonNode) throws SQLException, AuthorizeException {
        SubscriptionRest subscriptionRest = null;
        try {
            subscriptionRest = new ObjectMapper().readValue(jsonNode.toString(), SubscriptionRest.class);
        } catch (IOException e) {
            throw new UnprocessableEntityException("Error parsing subscription json: " + e.getMessage());
        }
        Subscription subscription = null;
        String notFoundException = "ResourceNotFoundException:" + apiCategory + "." + model
                + " with id: " + id + " not found";
        try {
            subscription = subscribeService.findById(context, id);
            if (subscription == null) {
                throw new ResourceNotFoundException(notFoundException);
            }
        } catch (SQLException e) {
            throw new ResourceNotFoundException(notFoundException);
        }
        if (id.equals(subscriptionRest.getId())) {
            subscription = subscribeService.updateSubscription(context, subscriptionRest.getId(), null,
                    null, subscriptionRest.getSubscriptionParameterList(), subscriptionRest.getType());
            return converter.toRest(subscription, utils.obtainProjection());
        } else {
            throw new IllegalArgumentException("The id in the Json and the id in the url do not match: "
                    + id + ", "
                    + subscriptionRest.getId());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void delete(Context context, Integer id) {
        try {
            subscribeService.deleteSubscription(context, id);
        } catch (SQLException | AuthorizeException sqlException) {
            throw new RuntimeException(sqlException.getMessage(), sqlException);
        }
    }

    @Override
    public Class<SubscriptionRest> getDomainClass() {
        return SubscriptionRest.class;
    }

}
