/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import org.springframework.stereotype.Component;

/**
 * A projection that provides an abbreviated form of any resource that omits all optional embeds.
 */
@Component
public class ListProjection extends AbstractProjection {

    public final static String NAME = "list";

    @Override
    public String getName() {
        return NAME;
    }

}
