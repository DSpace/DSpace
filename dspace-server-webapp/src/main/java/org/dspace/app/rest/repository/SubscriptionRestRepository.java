/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.dspace.app.rest.model.SubscriptionRest.CATEGORY;
import static org.dspace.app.rest.model.SubscriptionRest.NAME;
import static org.dspace.core.Constants.COLLECTION;
import static org.dspace.core.Constants.COMMUNITY;
import static org.dspace.core.Constants.READ;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.SubscriptionParameterRest;
import org.dspace.app.rest.model.SubscriptionRest;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.FrequencyType;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.subscriptions.SubscriptionEmailNotificationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage SubscriptionRest object
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
@Component(SubscriptionRest.CATEGORY + "." + SubscriptionRest.NAME)
public class SubscriptionRestRepository extends DSpaceRestRepository<SubscriptionRest, Integer>
                                         implements InitializingBean {

    @Autowired
    private ConverterService converter;
    @Autowired
    private EPersonService ePersonService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private SubscribeService subscribeService;
    @Autowired
    private DSpaceObjectUtils dspaceObjectUtil;
    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;
    @Autowired
    private SubscriptionEmailNotificationService subscriptionEmailNotificationService;

    @Override
    @PreAuthorize("hasPermission(#id, 'subscription', 'READ')")
    public SubscriptionRest findOne(Context context, Integer id) {
        Subscription subscription = null;
        try {
            subscription = subscribeService.findById(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return Objects.isNull(subscription) ? null : converter.toRest(subscription, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<SubscriptionRest> findAll(Context context, Pageable pageable) {
        try {
            List<Subscription> subscriptionList = subscribeService.findAll(context, null,
                                                           Math.toIntExact(pageable.getPageSize()),
                                                           Math.toIntExact(pageable.getOffset()));
            Long total = subscribeService.countAll(context);
            return converter.toRestPage(subscriptionList, pageable, total,  utils.obtainProjection());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "findByEPerson")
    @PreAuthorize("hasPermission(#epersonId, 'EPERSON', 'READ')")
    public Page<SubscriptionRest> findSubscriptionsByEPerson(@Parameter(value = "uuid", required = true) UUID epersonId,
                                                              Pageable pageable) throws Exception {
        Long total = null;
        List<Subscription> subscriptions = null;
        try {
            Context context = obtainContext();
            EPerson ePerson = ePersonService.find(context, epersonId);
            subscriptions = subscribeService.findSubscriptionsByEPerson(context, ePerson,
                                                 Math.toIntExact(pageable.getPageSize()),
                                                 Math.toIntExact(pageable.getOffset()));
            total = subscribeService.countSubscriptionsByEPerson(context, ePerson);
        } catch (SQLException e) {
            throw new SQLException(e.getMessage(), e);
        }
        return converter.toRestPage(subscriptions, pageable, total, utils.obtainProjection());
    }

    @SearchRestMethod(name = "findByEPersonAndDso")
    @PreAuthorize("hasPermission(#epersonId, 'EPERSON', 'READ')")
    public Page<SubscriptionRest> findByEPersonAndDso(@Parameter(value = "eperson_id", required = true) UUID epersonId,
                                                      @Parameter(value = "resource",required = true) UUID dsoId,
                                                       Pageable pageable) throws Exception {
        Long total = null;
        List<Subscription> subscriptions = null;
        try {
            Context context = obtainContext();
            DSpaceObject dSpaceObject = dspaceObjectUtil.findDSpaceObject(context, dsoId);
            EPerson ePerson = ePersonService.find(context, epersonId);
            subscriptions = subscribeService.findSubscriptionsByEPersonAndDso(context, ePerson, dSpaceObject,
                                                                    Math.toIntExact(pageable.getPageSize()),
                                                                    Math.toIntExact(pageable.getOffset()));
            total = subscribeService.countByEPersonAndDSO(context, ePerson, dSpaceObject);
        } catch (SQLException e) {
            throw new SQLException(e.getMessage(), e);
        }
        return converter.toRestPage(subscriptions, pageable, total, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    protected SubscriptionRest createAndReturn(Context context) throws SQLException, AuthorizeException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        String epersonId = req.getParameter("eperson_id");
        String dsoId = req.getParameter("resource");

        if (StringUtils.isBlank(dsoId) || StringUtils.isBlank(epersonId)) {
            throw new UnprocessableEntityException("Both eperson than DSpaceObject uuids must be provieded!");
        }

        try {
            DSpaceObject dSpaceObject = dspaceObjectUtil.findDSpaceObject(context, UUID.fromString(dsoId));
            EPerson ePerson = ePersonService.findByIdOrLegacyId(context, epersonId);
            if (Objects.isNull(ePerson) || Objects.isNull(dSpaceObject)) {
                throw new DSpaceBadRequestException("Id of person or dspace object must represents reals ids");
            }

            // user must have read permissions to dSpaceObject object
            if (!authorizeService.authorizeActionBoolean(context, ePerson, dSpaceObject, READ, true)) {
                throw new AuthorizeException("The user has not READ rights on this DSO");
            }

            // if user is Admin don't make this control,
            // otherwise make this control because normal user can only subscribe with their own ID of user.
            if (!authorizeService.isAdmin(context)) {
                if (!ePerson.equals(context.getCurrentUser())) {
                    throw new AuthorizeException("Only administrator can subscribe for other persons");
                }
            }

            if (dSpaceObject.getType() == COMMUNITY || dSpaceObject.getType() == COLLECTION) {
                Subscription subscription = null;
                ServletInputStream input = req.getInputStream();
                SubscriptionRest subscriptionRest = new ObjectMapper().readValue(input, SubscriptionRest.class);
                List<SubscriptionParameterRest> subscriptionParameterList = subscriptionRest
                        .getSubscriptionParameterList();
                if (CollectionUtils.isNotEmpty(subscriptionParameterList)) {
                    List<SubscriptionParameter> subscriptionParameters = new ArrayList<>();
                    validateParameters(subscriptionRest, subscriptionParameterList, subscriptionParameters);
                    subscription = subscribeService.subscribe(context, ePerson, dSpaceObject, subscriptionParameters,
                                                              subscriptionRest.getSubscriptionType());
                }
                context.commit();
                return converter.toRest(subscription, utils.obtainProjection());
            } else {
                throw new DSpaceBadRequestException(
                        "Currently subscription is supported only for Community and Collection");
            }
        } catch (SQLException sqlException) {
            throw new SQLException(sqlException.getMessage(), sqlException);
        } catch (IOException ioException) {
            throw new UnprocessableEntityException("error parsing the body");
        }
    }

    private void validateParameters(SubscriptionRest subscriptionRest,
                                    List<SubscriptionParameterRest> subscriptionParameterList,
                                    List<SubscriptionParameter> subscriptionParameters) {
        for (SubscriptionParameterRest subscriptionParameterRest : subscriptionParameterList) {
            SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
            var name = subscriptionParameterRest.getName();
            var value = subscriptionParameterRest.getValue();
            if (!StringUtils.equals("frequency", name) || !FrequencyType.isSupportedFrequencyType(value)) {
                throw new UnprocessableEntityException("Provided SubscriptionParameter name:" + name +
                                                       " or value: " + value + " is not supported!");
            }
            subscriptionParameter.setName(name);
            subscriptionParameter.setValue(value);
            subscriptionParameters.add(subscriptionParameter);
        }

        var type = subscriptionRest.getSubscriptionType();
        if (!subscriptionEmailNotificationService.getSupportedSubscriptionTypes().contains(type)) {
            throw new UnprocessableEntityException("Provided subscriptionType:" + type + "  is not supported!" +
                    " Must be one of: " + subscriptionEmailNotificationService.getSupportedSubscriptionTypes());
        }
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'subscription', 'WRITE')")
    protected SubscriptionRest put(Context context, HttpServletRequest request, String apiCategory, String model,
                                   Integer id, JsonNode jsonNode) throws SQLException {

        SubscriptionRest subscriptionRest;
        try {
            subscriptionRest = new ObjectMapper().readValue(jsonNode.toString(), SubscriptionRest.class);
        } catch (IOException e) {
            throw new UnprocessableEntityException("Error parsing subscription json: " + e.getMessage(), e);
        }

        Subscription subscription = subscribeService.findById(context, id);
        if (Objects.isNull(subscription)) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }

        if (id.equals(subscription.getID())) {
            List<SubscriptionParameter> subscriptionParameters = new ArrayList<>();
            List<SubscriptionParameterRest> subscriptionParameterList = subscriptionRest.getSubscriptionParameterList();
            validateParameters(subscriptionRest, subscriptionParameterList, subscriptionParameters);
            subscription = subscribeService.updateSubscription(context, id, subscriptionRest.getSubscriptionType(),
                                                               subscriptionParameters);
            context.commit();
            return converter.toRest(subscription, utils.obtainProjection());
        } else {
            throw new IllegalArgumentException("The id in the Json and the id in the url do not match: " + id + ", "
                                               + subscription.getID());
        }
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'subscription', 'DELETE')")
    protected void delete(Context context, Integer id) {
        try {
            Subscription subscription = subscribeService.findById(context, id);
            if (Objects.isNull(subscription)) {
                throw new ResourceNotFoundException(CATEGORY + "." + NAME + " with id: " + id + " not found");
            }
            subscribeService.deleteSubscription(context, subscription);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete Subscription with id = " + id, e);
        }
    }

    @Override
    public Class<SubscriptionRest> getDomainClass() {
        return SubscriptionRest.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService.register(this, Arrays.asList(Link.of("/api/" + SubscriptionRest.CATEGORY +
                       "/" + SubscriptionRest.NAME_PLURAL + "/search", SubscriptionRest.NAME_PLURAL + "-search")));
    }

}