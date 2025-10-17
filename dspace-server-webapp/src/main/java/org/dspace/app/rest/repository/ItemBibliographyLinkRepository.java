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

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.model.BibliographyRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MetadataValueList;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.disseminate.CSLBibliography;
import org.dspace.disseminate.CSLBibliographyGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "relationships" subresource of an individual item.
 */
@Component(ItemRest.CATEGORY + "." + ItemRest.PLURAL_NAME + "." + ItemRest.BIBLIOGRAPHY)
public class ItemBibliographyLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    private CSLBibliographyGenerator cSLBibliographyGenerator;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemConverter itemConverter;

    @PreAuthorize("hasPermission(#itemId, 'ITEM', 'READ')")
    public BibliographyRest getItemBibliography(@Nullable HttpServletRequest request,
                                                UUID itemId,
                                                @Nullable Pageable optionalPageable,
                                                Projection projection) {
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, itemId);
            if (item == null) {
                throw new ResourceNotFoundException("No such item: " + itemId);
            }
            MetadataValueList metadataValues = itemConverter.getPermissionFilteredMetadata(context, item);

            BibliographyRest citationRest = new BibliographyRest();
            try {
                List<CSLBibliography> bibliographies = cSLBibliographyGenerator.getBibliographies(metadataValues,
                        CSLBibliographyGenerator.OutputFormat.TEXT);
                citationRest.addBibliographies(bibliographies);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return citationRest;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
