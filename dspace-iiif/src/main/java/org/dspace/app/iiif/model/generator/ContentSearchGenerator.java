/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model.generator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import de.digitalcollections.iiif.model.Profile;
import de.digitalcollections.iiif.model.Service;
import de.digitalcollections.iiif.model.search.ContentSearchService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This generator wraps the search service annotation that is added to
 * the manifest for searchable items. Only a single search service is defined
 * for the manifest. There should be a single instance of this object per request.
 * The {@code @RequestScope} provides a single instance created and available during
 * complete lifecycle of the HTTP request.
 */
@RequestScope
@Component
public class ContentSearchGenerator implements IIIFService {

    private String identifier;
    private String label;

    @Autowired
    ProfileGenerator profile;

    /**
     * Mandatory URI for search service.
     * @param identifier
     */
    public void setIdentifier(@NotNull String identifier) {
        if (identifier.isEmpty()) {
            throw new RuntimeException("The search service requires an identifier.");
        }
        this.identifier = identifier;
    }

    /**
     * Optional label for the search service.
     * @param label the search service label.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public Service generateService() {
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
        profiles.add(profile.generateValue());
        contentSearchService.setProfiles(profiles);
        return contentSearchService;
    }
}
