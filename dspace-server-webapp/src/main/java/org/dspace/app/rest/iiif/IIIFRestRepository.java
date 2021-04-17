/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.rest.iiif.service.AnnotationListService;
import org.dspace.app.rest.iiif.service.CanvasLookupService;
import org.dspace.app.rest.iiif.service.ManifestService;
import org.dspace.app.rest.iiif.service.SearchService;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Repository for IIIF Presentation and Search API requests.
 */
@Component
public class IIIFRestRepository {

    @Autowired
    ItemService itemService;

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    ManifestService manifestService;

    @Autowired
    SearchService searchService;

    @Autowired
    AnnotationListService annotationListService;

    @Autowired
    CanvasLookupService canvasLookupService;


    /**
     * The manifest response contains sufficient information for the client to initialize itself
     * and begin to display something quickly to the user. The manifest resource represents a single
     * object and any intellectual work or works embodied within that object. In particular it
     * includes the descriptive, rights and linking information for the object. It then embeds
     * the sequence(s) of canvases that should be rendered to the user.
     *
     * Returns manifest for single DSpace item.
     *
     * @param context DSpace context
     * @param id DSpace Item uuid
     * @return manifest as JSON
     */
    @Cacheable(key = "#id.toString()", cacheNames = "manifests")
    @PreAuthorize("hasPermission(#id, 'ITEM', 'READ')")
    public String getManifest(Context context, UUID id)
            throws ResourceNotFoundException {
        Item item;
        try {
            item = itemService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (item == null) {
            throw new ResourceNotFoundException("IIIF manifest for  id " + id + " not found");
        }
        return manifestService.getManifest(item, context);
    }

    /**
     * The canvas represents an individual page or view and acts as a central point for
     * laying out the different content resources that make up the display. This information
     * should be embedded within a sequence.
     *
     * @param context DSpace context
     * @param id DSpace item uuid
     * @param canvasId canvas identifier
     * @return canvas as JSON
     */
    @PreAuthorize("hasPermission(#id, 'ITEM', 'READ')")
    public String getCanvas(Context context, UUID id, String canvasId)
            throws ResourceNotFoundException {
        Item item;
        try {
            item = itemService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (item == null) {
            throw new ResourceNotFoundException("IIIF canvas for  id " + id + " not found");
        }
        return canvasLookupService.generateCanvas(context, item, canvasId);
    }

    /**
     * Returns search hits and word coordinates as an AnnotationList.
     *
     * Search scope is a single DSpace item or manifest.
     *
     * @param id DSpace item uuid
     * @param query  query terms
     * @return AnnotationList as JSON
     */
    @PreAuthorize("hasPermission(#id, 'ITEM', 'READ')")
    public String searchInManifest(UUID id, String query) {

        return searchService.searchWithinManifest(id, query);
    }

    /**
     * Returns annotations for machine readable metadata that describes the resource.
     *
     * @param context DSpace context
     * @param id the Item uuid
     * @return AnnotationList as JSON
     */
    @PreAuthorize("hasPermission(#id, 'ITEM', 'READ')")
    public String getSeeAlsoAnnotations(Context context, UUID id) {
        return annotationListService.getSeeAlsoAnnotations(context, id);
    }

}
