/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.dspace.authorize.ResourcePolicy.TYPE_SUBMISSION;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.exception.ResourceAlreadyExistsException;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.MissingParameterException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.SupervisionOrderRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.supervision.SupervisionOrder;
import org.dspace.supervision.enumeration.SupervisionOrderType;
import org.dspace.supervision.service.SupervisionOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage SupervisionOrderRest object
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
@Component(SupervisionOrderRest.CATEGORY + "." + SupervisionOrderRest.NAME)
public class SupervisionOrderRestRepository extends DSpaceRestRepository<SupervisionOrderRest, Integer> {

    private static final Logger log =
        org.apache.logging.log4j.LogManager.getLogger(SupervisionOrderRestRepository.class);

    @Autowired
    private SupervisionOrderService supervisionOrderService;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthorizeService authorizeService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public SupervisionOrderRest findOne(Context context, Integer id) {
        try {
            SupervisionOrder supervisionOrder = supervisionOrderService.find(context, id);
            if (Objects.isNull(supervisionOrder)) {
                throw new ResourceNotFoundException("Couldn't find supervision order for id: " + id);
            }
            return converterService.toRest(supervisionOrder, utils.obtainProjection());
        } catch (SQLException e) {
            log.error("Something went wrong with getting supervision order with id:" + id, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public Page<SupervisionOrderRest> findAll(Context context, Pageable pageable) {
        try {
            List<SupervisionOrder> supervisionOrders = supervisionOrderService.findAll(context);
            return converterService.toRestPage(supervisionOrders, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            log.error("Something went wrong with getting supervision orders", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public SupervisionOrderRest createAndReturn(Context context) throws AuthorizeException, SQLException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        SupervisionOrder supervisionOrder;
        String itemId = req.getParameter("uuid");
        String groupId = req.getParameter("group");
        String type = req.getParameter("type");

        validateParameters(itemId, groupId, type);

        Item item = itemService.find(context, UUID.fromString(itemId));
        if (item == null) {
            throw new UnprocessableEntityException("Item with uuid: " + itemId + " not found");
        }

        if (item.isArchived() || item.isWithdrawn()) {
            throw new UnprocessableEntityException("An archived Item with uuid: " + itemId + " can't be supervised");
        }

        Group group = groupService.find(context, UUID.fromString(groupId));
        if (group == null) {
            throw new UnprocessableEntityException("Group with uuid: " + groupId + " not found");
        }

        supervisionOrder = supervisionOrderService.findByItemAndGroup(context, item, group);
        if (Objects.nonNull(supervisionOrder)) {
            throw new ResourceAlreadyExistsException(
                "A supervision order already exists with itemId <" + itemId + "> and groupId <" + groupId + ">");
        }
        supervisionOrder = supervisionOrderService.create(context, item, group);
        addGroupPoliciesToItem(context, item, group, type);
        return converterService.toRest(supervisionOrder, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    protected void delete(Context context, Integer id)
        throws AuthorizeException, RepositoryMethodNotImplementedException {

        try {
            SupervisionOrder supervisionOrder = supervisionOrderService.find(context, id);
            if (Objects.isNull(supervisionOrder)) {
                throw new ResourceNotFoundException(
                    SupervisionOrderRest.CATEGORY + "." + SupervisionOrderRest.NAME +
                        " with id: " + id + " not found"
                );
            }
            removeGroupPoliciesToItem(context, supervisionOrder.getItem(), supervisionOrder.getGroup());
            supervisionOrderService.delete(context, supervisionOrder);

        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "byItem")
    public Page<SupervisionOrderRest> findByItem(@Parameter(value = "uuid", required = true) String itemId,
                                                 Pageable pageable) {
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, UUID.fromString(itemId));
            if (Objects.isNull(item)) {
                throw new ResourceNotFoundException("no item is found for the uuid < " + itemId + " >");
            }
            return converterService.toRestPage(supervisionOrderService.findByItem(context, item),
                pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<SupervisionOrderRest> getDomainClass() {
        return SupervisionOrderRest.class;
    }

    private void validateParameters(String itemId, String groupId, String type) {
        if (Objects.isNull(itemId)) {
            throw new MissingParameterException("Missing item (uuid) parameter");
        }

        if (Objects.isNull(groupId)) {
            throw new MissingParameterException("Missing group (uuid) parameter");
        }

        if (Objects.isNull(type)) {
            throw new MissingParameterException("Missing type parameter");
        } else if (SupervisionOrderType.invalid(type)) {
            throw new IllegalArgumentException("wrong type value, Type must be (" +
                                                   Arrays.stream(SupervisionOrderType.values())
                                                       .map(Enum::name)
                                                       .collect(Collectors.joining(" or ")) + ")");
        }

    }

    private void addGroupPoliciesToItem(Context context, Item item, Group group, String type)
        throws SQLException, AuthorizeException {

        if (StringUtils.isNotEmpty(type)) {
            if (type.equals("EDITOR")) {
                addGroupPolicyToItem(context, item, Constants.READ, group, TYPE_SUBMISSION);
                addGroupPolicyToItem(context, item, Constants.WRITE, group, TYPE_SUBMISSION);
                addGroupPolicyToItem(context, item, Constants.ADD, group, TYPE_SUBMISSION);
            } else if (type.equals("OBSERVER")) {
                addGroupPolicyToItem(context, item, Constants.READ, group, TYPE_SUBMISSION);
            }
        }
    }

    private void addGroupPolicyToItem(Context context, Item item, int action, Group group, String policyType)
        throws AuthorizeException, SQLException {
        authorizeService.addPolicy(context, item, action, group, policyType);
        List<Bundle> bundles = item.getBundles();
        for (Bundle bundle : bundles) {
            authorizeService.addPolicy(context, bundle, action, group, policyType);
            List<Bitstream> bits = bundle.getBitstreams();
            for (Bitstream bitstream : bits) {
                authorizeService.addPolicy(context, bitstream, action, group, policyType);
            }
        }
    }

    private void removeGroupPoliciesToItem(Context context, Item item, Group group)
        throws AuthorizeException, SQLException {
        authorizeService.removeGroupPolicies(context, item, group);
        List<Bundle> bundles = item.getBundles();
        for (Bundle bundle : bundles) {
            authorizeService.removeGroupPolicies(context, bundle, group);
            List<Bitstream> bits = bundle.getBitstreams();
            for (Bitstream bitstream : bits) {
                authorizeService.removeGroupPolicies(context, bitstream, group);
            }
        }
    }

}
