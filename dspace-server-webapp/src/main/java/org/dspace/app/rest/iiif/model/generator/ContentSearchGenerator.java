/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import de.digitalcollections.iiif.model.Profile;
import de.digitalcollections.iiif.model.Service;
import de.digitalcollections.iiif.model.search.ContentSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Facade for the Search API version 1.0 search service description.
 *
 * Added to the Manifest when the item supports full-text searching, identified
 * by the "dspace.entity.type: IIIFSearchable" DSpace metadata field. NOTE: the
 * entity.type is going to be abandoned in favor of another DSO metadata field.
 */
@Component
@RequestScope
public class ContentSearchGenerator implements org.dspace.app.rest.iiif.model.generator.IIIFService {

    private String identifier;
    private String label;

    @Autowired
    org.dspace.app.rest.iiif.model.generator.ProfileGenerator profile;

    /**
     * Mandatory URI for search service.
     * @param identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Optional label
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public Service getService() {
        if (identifier == null) {
            throw new RuntimeException("You must provide an identifier for the search service.");
        }
        ContentSearchService contentSearchService = new ContentSearchService(identifier);
        if (label != null) {
            contentSearchService.setLabel(label);
        }
        try {
            contentSearchService.setContext(new URI("http://iiif.io/api/search/0/context.json"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        ArrayList<Profile> profiles = new ArrayList<>();
        profile.setIdentifier("http://iiif.io/api/search/0/search");
        profiles.add(profile.getValue());
        contentSearchService.setProfiles(profiles);
        return contentSearchService;
    }
}
