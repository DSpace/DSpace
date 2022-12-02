/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.SubscriptionParameterRest;
import org.dspace.app.rest.model.SubscriptionRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.SubscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage SubscriptionRest object
 *
 * @author Alba Aliu at atis.al
 */
@Component(SubscriptionRest.CATEGORY + "." + SubscriptionRest.NAME)
public class SubscriptionRestRepository extends DSpaceRestRepository<SubscriptionRest, Integer>
        implements LinkRestRepository {

    @Autowired
    private ConverterService converter;
    @Autowired
    private EPersonService personService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private SubscribeService subscribeService;
    @Autowired
    private DSpaceObjectUtils dspaceObjectUtil;
    @Autowired
    private ResourcePatch<Subscription> resourcePatch;

    @Override
    @PreAuthorize("isAuthenticated()")
    public SubscriptionRest findOne(Context context, Integer id) {
        try {
            Subscription subscription = subscribeService.findById(context, id);
            if (Objects.isNull(subscription)) {
                throw new ResourceNotFoundException("The subscription for ID: " + id + " could not be found");
            }
            return converter.toRest(subscription, utils.obtainProjection());
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException.getMessage(), sqlException);
        } catch (AuthorizeException authorizeException) {
            throw new RuntimeException(authorizeException.getMessage());
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<SubscriptionRest> findAll(Context context, Pageable pageable) {
        try {
            HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
            String resourceType = req.getParameter("resourceType");
            List<Subscription> subscriptionList = subscribeService.findAll(context, resourceType,
                    pageable.getPageSize(), Math.toIntExact(pageable.getOffset()));
            Long total = subscribeService.countAll(context);
            return converter.toRestPage(subscriptionList, pageable, total,  utils.obtainProjection());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @SearchRestMethod(name = "findByEPerson")
    public Page<SubscriptionRest> findAllSubscriptionsByEPerson(String id, Pageable pageable) throws Exception {
        try {
            Context context = obtainContext();
            EPerson ePerson = personService.findByIdOrLegacyId(context, id);
            if (context.getCurrentUser().equals(ePerson)
                    || authorizeService.isAdmin(context, context.getCurrentUser())) {
                List<Subscription> subscriptionList = subscribeService.getSubscriptionsByEPerson(context,
                        ePerson, pageable.getPageSize(), Math.toIntExact(pageable.getOffset()));
                Long total = subscribeService.countAllByEPerson(context, ePerson);
                return converter.toRestPage(subscriptionList, pageable, total, utils.obtainProjection());
            } else {
                throw new AuthorizeException("Only admin or e-person themselves can search for it's subscription");
            }
        } catch (SQLException sqlException) {
            throw new SQLException(sqlException.getMessage(), sqlException);

        } catch (AuthorizeException authorizeException) {
            throw new AuthorizeException(authorizeException.getMessage());
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
            if (context.getCurrentUser().equals(ePerson)
                    || authorizeService.isAdmin(context, context.getCurrentUser())) {
                List<Subscription> subscriptionList =
                        subscribeService.getSubscriptionsByEPersonAndDso(context, ePerson, dSpaceObject,
                                pageable.getPageSize(), Math.toIntExact(pageable.getOffset()));
                Long total = subscribeService.countAllByEPersonAndDSO(context, ePerson, dSpaceObject);
                return converter.toRestPage(subscriptionList, pageable, total,
                        utils.obtainProjection());
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
    protected SubscriptionRest createAndReturn(Context context) throws SQLException, AuthorizeException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        String epersonId = req.getParameter("eperson_id");
        String dsoId = req.getParameter("dspace_object_id");
        // dso must always be set
        if (dsoId == null || epersonId == null) {
            throw new UnprocessableEntityException("error parsing the body");
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            DSpaceObject dSpaceObject = dspaceObjectUtil.findDSpaceObject(context, UUID.fromString(dsoId));
            EPerson ePerson = personService.findByIdOrLegacyId(context, epersonId);
            if (ePerson == null || dSpaceObject == null) {
                throw new BadRequestException("Id of person or dspace object must represents reals ids");
            }
            // user must have read permissions to dataspace object
            if (!authorizeService.authorizeActionBoolean(context, ePerson, dSpaceObject,  Constants.READ, true)) {
                throw new AuthorizeException("The user has not READ rights on this DSO");
            }
            // if user is admin do not make this control,
            // otherwise make this control because normal user can only subscribe with their own ID of user.
            if (!authorizeService.isAdmin(context)) {
                if (!ePerson.equals(context.getCurrentUser())) {
                    throw new AuthorizeException("Only administrator can subscribe for other persons");
                }
            }
            ServletInputStream input = req.getInputStream();
            SubscriptionRest subscriptionRest = mapper.readValue(input, SubscriptionRest.class);
            Subscription subscription = null;
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
                        subscriptionRest.getSubscriptionType());
            }
            context.commit();
            return converter.toRest(subscription, utils.obtainProjection());
        } catch (SQLException sqlException) {
            throw new SQLException(sqlException.getMessage(), sqlException);

        } catch (AuthorizeException authorizeException) {
            throw new AuthorizeException(authorizeException.getMessage());
        } catch (IOException ioException) {
            throw new UnprocessableEntityException("error parsing the body");
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    protected SubscriptionRest put(Context context, HttpServletRequest request, String apiCategory, String model,
                                   Integer id, JsonNode jsonNode) throws SQLException, AuthorizeException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        String epersonId = req.getParameter("eperson_id");
        String dsoId = req.getParameter("dspace_object_id");
        SubscriptionRest subscriptionRest = null;
        DSpaceObject dSpaceObject = null;
        EPerson ePerson = null;
        try {
            subscriptionRest = new ObjectMapper().readValue(jsonNode.toString(), SubscriptionRest.class);
        } catch (IOException e) {
            throw new UnprocessableEntityException("Error parsing subscription json: " + e.getMessage());
        }
        String notFoundException = "ResourceNotFoundException:" + apiCategory + "." + model
                + " with id: " + id + " not found";
        Subscription subscription;
        try {
            subscription = subscribeService.findById(context, id);
            if (subscription == null) {
                throw new ResourceNotFoundException(notFoundException);
            }
            dSpaceObject = dspaceObjectUtil.findDSpaceObject(context, UUID.fromString(dsoId));
            ePerson = personService.findByIdOrLegacyId(context, epersonId);
            if (dSpaceObject == null || ePerson == null) {
                throw new ResourceNotFoundException(notFoundException);
            }
        } catch (SQLException e) {
            throw new ResourceNotFoundException(notFoundException);
        } catch (AuthorizeException e) {
            throw new AuthorizeException(e.getMessage());
        }
        if (id.equals(subscription.getID())) {
            List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
            for (SubscriptionParameterRest subscriptionParameterRest :
                    subscriptionRest.getSubscriptionParameterList()) {
                SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
                subscriptionParameter.setSubscription(subscription);
                subscriptionParameter.setValue(subscriptionParameterRest.getValue());
                subscriptionParameter.setName(subscriptionParameterRest.getName());
                subscriptionParameterList.add(subscriptionParameter);
            }
            subscription = subscribeService.updateSubscription(context, id, ePerson,
                    dSpaceObject, subscriptionParameterList, subscriptionRest.getSubscriptionType());
            context.commit();
            return converter.toRest(subscription, utils.obtainProjection());
        } else {
            throw new IllegalArgumentException("The id in the Json and the id in the url do not match: "
                    + id + ", "
                    + subscription.getID());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void patch(Context context,HttpServletRequest request,String category, String model, Integer id, Patch patch)
            throws UnprocessableEntityException, DSpaceBadRequestException, AuthorizeException {
        try {
            Subscription subscription = subscribeService.findById(context, id);
            if (subscription == null) {
                throw new ResourceNotFoundException(category + "." + model + " with id: " + id + " not found");
            }
            if (!authorizeService.isAdmin(context) || subscription.getePerson().equals(context.getCurrentUser())) {
                throw new AuthorizeException("Only admin or e-person themselves can edit the subscription");
            }
            resourcePatch.patch(context, subscription, patch.getOperations());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (AuthorizeException authorizeException) {
            throw new AuthorizeException(authorizeException.getMessage());
        } catch (RuntimeException runtimeException) {
            throw new RuntimeException(runtimeException.getMessage());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void delete(Context context, Integer id) throws AuthorizeException {
        try {
            subscribeService.deleteSubscription(context, id);
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException.getMessage(), sqlException);
        } catch (AuthorizeException authorizeException) {
            throw new AuthorizeException(authorizeException.getMessage());
        }
    }

    @Override
    public Class<SubscriptionRest> getDomainClass() {
        return SubscriptionRest.class;
    }

}