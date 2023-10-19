/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.UUID;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class PrimaryBitstreamDTO {

    private UUID primary;

    public UUID getPrimary() {
        return primary;
    }

    public void setPrimary(UUID primary) {
        this.primary = primary;
    }

}
