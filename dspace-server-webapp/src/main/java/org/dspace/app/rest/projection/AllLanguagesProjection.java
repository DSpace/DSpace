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
 * This projection. When set,
 * no metadata languages filtration will be performed during DSpaceObject conversion.
 * will return all metadata in all languages.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 */
@Component
public class AllLanguagesProjection extends AbstractProjection {

    public final static String NAME = "allLanguages";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isAllLanguages() {
        return true;
    }
}
