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

import de.digitalcollections.iiif.model.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * This class wraps the domain model service profile. Spring can inject unique
 * instances of this generator into services using the prototype scope.
 */
@Scope("prototype")
@Component
public class ProfileGenerator implements IIIFValue {

    private String identifier;
    /**
     * Input String will be converted to URI for use in the Profile.
     * @param identifier  URI as string
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public Profile generate() {
        try {
            return new Profile(new URI(identifier));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
