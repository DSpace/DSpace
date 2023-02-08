/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.IdentifierRest;
import org.dspace.app.rest.model.IdentifiersRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.service.DOIService;
import org.dspace.identifier.service.IdentifierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for the identifier of an Item
 */
@Component(ItemRest.CATEGORY + "." + ItemRest.NAME + "." + ItemRest.IDENTIFIERS)
public class ItemIdentifierLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {
    @Autowired
    ItemService itemService;

    @Autowired
    IdentifierService identifierService;

    @Autowired
    DOIService doiService;
    @Autowired
    HandleService handleService;

    @PreAuthorize("hasPermission(#itemId, 'ITEM', 'READ')")
    public IdentifiersRest getIdentifiers(@Nullable HttpServletRequest request,
                                          UUID itemId,
                                          @Nullable Pageable optionalPageable,
                                          Projection projection) throws SQLException {
        Context context = ContextUtil.obtainCurrentRequestContext();
        Item item = itemService.find(context, itemId);
        if (item == null) {
            throw new ResourceNotFoundException("Could not find item with id " + itemId);
        }
        IdentifiersRest identifiersRest = new IdentifiersRest();
        List<IdentifierRest> identifierRestList = new ArrayList<>();
        DOI doi = doiService.findDOIByDSpaceObject(context, item);
        String handle = HandleServiceFactory.getInstance().getHandleService().findHandle(context, item);
        try {
            if (doi != null) {
                String doiUrl = doiService.DOIToExternalForm(doi.getDoi());
                identifierRestList.add(new IdentifierRest(
                        doiUrl, "doi", DOIIdentifierProvider.statusText[doi.getStatus()]));
            }
            if (handle != null) {
                identifierRestList.add(new IdentifierRest(handleService.getCanonicalForm(handle), "handle", null));
            }
        } catch (IdentifierException e) {
            throw new IllegalStateException("Failed to register identifier: " + e.getMessage());
        }
        identifiersRest.setIdentifiers(identifierRestList);
        return identifiersRest;
    }
}
