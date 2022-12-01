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
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.eperson.service.SubscriptionParameterService;
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
    @Autowired
    private DSpaceObjectUtils dspaceObjectUtil;
    @Autowired
    private ResourcePatch<Subscription> resourcePatch;


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
            Context context = obtainContext();
            EPerson ePerson = personService.findByIdOrLegacyId(context, id);
            if (context.getCurrentUser().equals(ePerson) || authorizeService.isAdmin(context, context.getCurrentUser())) {
                List<Subscription> subscriptionList = subscribeService.getSubscriptionsByEPerson(context, ePerson);
                return converter.toRestPage(subscriptionList, pageable, utils.obtainProjection());
            } else {
                throw new AuthorizeException("Only admin or e-person themselves can search for it's subscription");
            }
        } catch (SQLException | AuthorizeException sqlException) {
            throw new RuntimeException(sqlException.getMessage(), sqlException);
        }
    }
    @PreAuthorize("isAuthenticated()")
    @SearchRestMethod(name = "findByEPersonAndDso")
    public Page<SubscriptionRest> findByEPersonAndDso(Pageable pageable) throws Exception {
        try {
            Context context = obtainContext();
            HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
            String epersonId = req.getParameter("eperson_id");
            String dsoId = req.getParameter("dspace_object_id");
            DSpaceObject dSpaceObject = dspaceObjectUtil.findDSpaceObject(context, UUID.fromString(dsoId));
            EPerson ePerson = personService.findByIdOrLegacyId(context, epersonId);
            // dso must always be set
            if (dsoId == null || epersonId == null) {
                throw new UnprocessableEntityException("error parsing the body");
            }
            if (context.getCurrentUser().equals(ePerson) || authorizeService.isAdmin(context, context.getCurrentUser())) {
                List<Subscription> subscriptionList = subscribeService.getSubscriptionsByEPerson(context, ePerson);
                return converter.toRestPage(subscriptionList, pageable, utils.obtainProjection());
            } else {
                throw new AuthorizeException("Only admin or e-person themselves can search for it's subscription");
            }
        } catch (SQLException sqlException) {
            throw new SQLException(sqlException.getMessage(), sqlException);

        } catch (AuthorizeException authorizeException) {
            throw new AuthorizeException(authorizeException.getMessage());
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
            Subscription subscription = null;
            DSpaceObject dSpaceObject = dspaceObjectUtil.findDSpaceObject(context, UUID.fromString(dsoId));
            EPerson ePerson = personService.findByIdOrLegacyId(context, epersonId);
            List<SubscriptionParameterRest> subscriptionParameterList = subscriptionRest.getSubscriptionParameterList();
            if (subscriptionParameterList != null) {
                List<SubscriptionParameter> subscriptionParameters = new ArrayList<>();
                for (SubscriptionParameterRest subscriptionParameterRest : subscriptionParameterList) {
                    SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
                    subscriptionParameter.setName(subscriptionParameterRest.getName());
                    subscriptionParameter.setValue(subscriptionParameterRest.getValue());
                    subscriptionParameters.add(subscriptionParameter);
                }
                subscription = subscribeService.subscribe(context, ePerson,
                        dSpaceObject,
                        subscriptionParameters,
                        subscriptionRest.getType());
            }
            context.commit();
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
    public void patch(Context context, HttpServletRequest request, String apiCategory, String model, Integer id, Patch patch)
            throws UnprocessableEntityException, DSpaceBadRequestException {
        Subscription subscription = null;
        try {
            subscription = subscribeService.findById(context, id);
            if (subscription == null) {
                throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
            }
            resourcePatch.patch(context, subscription, patch.getOperations());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
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
