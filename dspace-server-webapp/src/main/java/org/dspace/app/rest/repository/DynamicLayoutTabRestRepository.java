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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.DynamicLayoutTabConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.DynamicLayoutBoxRest;
import org.dspace.app.rest.model.DynamicLayoutMetadataConfigurationRest;
import org.dspace.app.rest.model.DynamicLayoutTabRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.layout.DynamicLayoutBoxTypes;
import org.dspace.layout.DynamicLayoutTab;
import org.dspace.layout.service.DynamicLayoutTabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
/**
 * This is the repository responsible to manage DynamicLayoutTab Rest object
 *
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
@Component(DynamicLayoutTabRest.CATEGORY + "." + DynamicLayoutTabRest.NAME_PLURAL)
public class DynamicLayoutTabRestRepository extends DSpaceRestRepository<DynamicLayoutTabRest, Integer>
    implements ReloadableEntityObjectRepository<DynamicLayoutTab, Integer> {

    public static final String SCOPE_ITEM_ATTRIBUTE = "cris-layout-tab.scope-item";

    @Autowired
    private DynamicLayoutTabService service;

    @Autowired
    private DynamicLayoutTabConverter tabConverter;

    @Autowired
    private ResourcePatch<DynamicLayoutTab> resourcePatch;

    @Autowired
    private ItemService itemService;

    @Override
    @PreAuthorize("permitAll")
    public DynamicLayoutTabRest findOne(Context context, Integer id) {
        DynamicLayoutTab tab = null;
        try {
            tab = service.findAndEagerlyFetch(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if ( tab == null ) {
            return null;
        }
        return converter.toRest(tab, utils.obtainProjection());
    }

    @Override
    public Page<DynamicLayoutTabRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @SearchRestMethod(name = "findByItem")
    public Page<DynamicLayoutTabRest> findByItem(
        @Parameter(value = "uuid", required = true) String itemUuid, Pageable pageable) throws SQLException {
        Context context = obtainContext();
        List<DynamicLayoutTab> tabList = service.findByItem(context, itemUuid);
        getRequestService().getCurrentRequest().setAttribute(SCOPE_ITEM_ATTRIBUTE, itemUuid);
        Page<DynamicLayoutTabRest> restTabs = converter.toRestPage(tabList, pageable, utils.obtainProjection());
        restTabs = filterTabWithoutRows(pageable, restTabs);
        restTabs = filterFieldsWithAdvancedAttachmentRenderType(context, restTabs, itemUuid);
        return utils.getPage(restTabs.toList(), restTabs.getPageable());
    }

    @SearchRestMethod(name = "findByEntityType")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<DynamicLayoutTabRest> findByEntityType(
            @Parameter(value = "type", required = true) String type, Pageable pageable) {
        Context context = obtainContext();
        List<DynamicLayoutTab> tabList = null;
        Long totalRow = null;
        try {
            totalRow = service.countByEntityType(context, type);
            tabList = service.findByEntityType(
                context,
                type,
                null,
                pageable.getPageSize(),
                (pageable.getPageNumber() * pageable.getPageSize()) );
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (tabList == null) {
            return null;
        }
        return converter.toRestPage(tabList, pageable, totalRow, utils.obtainProjection());
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#getDomainClass()
     */
    @Override
    public Class<DynamicLayoutTabRest> getDomainClass() {
        return DynamicLayoutTabRest.class;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.ReloadableEntityObjectRepository#findDomainObjectByPk
     * (org.dspace.core.Context, java.io.Serializable)
     */
    @Override
    public DynamicLayoutTab findDomainObjectByPk(Context context, Integer id) throws SQLException {
        return service.find(context, id);
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.ReloadableEntityObjectRepository#getPKClass()
     */
    @Override
    public Class<Integer> getPKClass() {
        return Integer.class;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected DynamicLayoutTabRest createAndReturn(Context context) {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        DynamicLayoutTabRest tabRest = null;
        try {
            tabRest = mapper.readValue(req.getInputStream(), DynamicLayoutTabRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("error parsing the body", e1);
        }

        DynamicLayoutTab tab = tabConverter.toModel(context, tabRest);
        try {
            service.create(context, tab);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRest(tab, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public void patch(
            Context context, HttpServletRequest request, String apiCategory, String model, Integer id,
            Patch patch)
            throws UnprocessableEntityException, DSpaceBadRequestException {
        DynamicLayoutTab tab = null;
        try {
            tab = service.find(context, id);
            if (tab == null) {
                throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
            }
            resourcePatch.patch(obtainContext(), tab, patch.getOperations());
            service.update(context, tab);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, Integer id) throws AuthorizeException {
        try {
            DynamicLayoutTab tab = service.find(context, id);
            if (tab == null) {
                throw new ResourceNotFoundException("DynamicLayoutTab with id: " + id + " not found");
            }
            service.delete(context, tab);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Page<DynamicLayoutTabRest> filterTabWithoutRows(Pageable pageable, Page<DynamicLayoutTabRest> restTabs) {
        List<DynamicLayoutTabRest> listOfTabs =
            restTabs.filter(tab -> CollectionUtils.isNotEmpty(tab.getRows())).toList();
        return utils.getPage(listOfTabs, pageable);
    }

    private Page<DynamicLayoutTabRest> filterFieldsWithAdvancedAttachmentRenderType(Context context,
                                                                                 Page<DynamicLayoutTabRest> restTabs,
                                                                                 String itemUuid) throws SQLException {
        List<DynamicLayoutTabRest> listOfTabs = restTabs.toList();
        Item item = itemService.find(context, UUID.fromString(itemUuid));

        if (hasOriginalBitstream(item)) {
            return restTabs;
        }

        // Get DynamicLayoutBoxRest with boxType=METADATA
        List<DynamicLayoutBoxRest> boxes = findBoxesByType(listOfTabs, DynamicLayoutBoxTypes.METADATA.name());

        boxes.forEach(box -> {

            DynamicLayoutMetadataConfigurationRest boxConfiguration =
                ((DynamicLayoutMetadataConfigurationRest) box.getConfiguration());


            // filter fields with rendering not equal to 'advancedattachment'
            boxConfiguration
                .getRows()
                .forEach(row -> row.getCells()
                    .forEach(cell -> cell.setFields(
                        cell.getFields()
                        .stream()
                        .filter(field -> !"advancedattachment".equals(field.getRendering()))
                        .collect(Collectors.toList()))));

            // remove cells that contain empty fields
            boxConfiguration
                .getRows()
                .forEach(row -> row.getCells().removeIf(cell -> cell.getFields().isEmpty()));

            // remove rows that contain empty cells
            boxConfiguration
                .getRows().removeIf(row -> row.getCells().isEmpty());
        });

        return utils.getPage(listOfTabs, restTabs.getPageable());
    }

    private List<DynamicLayoutBoxRest> findBoxesByType(List<DynamicLayoutTabRest> tabs, String type) {

        return tabs.stream()
                   .flatMap(t -> t.getRows()
                        .stream()
                        .flatMap(r -> r.getCells()
                             .stream()
                             .flatMap(c -> c.getBoxes()
                                  .stream()
                                  .filter(b -> b.getBoxType()
                                       .equals(type)))))
                   .collect(Collectors.toList());
    }

    private boolean hasOriginalBitstream(Item item) {
        List<Bundle> bundles =  item.getBundles(Constants.DEFAULT_BUNDLE_NAME);
        return !bundles.isEmpty() && !bundles.get(0).getBitstreams().isEmpty();
    }

}
