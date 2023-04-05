/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

/**
 * The default projection.
 *
 * This does no transformation, and allows linking but not embedding of all subresources.
 */
public class DefaultProjection extends AbstractProjection {

    public final static String NAME = "default";

    @Override
    public String getName() {
        return NAME;
    }
}
