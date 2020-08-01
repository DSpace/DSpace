/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.UUID;

import org.dspace.app.rest.RestResourceController;

/**
 * The Researcher Profile REST resource.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@LinksRest(links = {
        @LinkRest(name = ResearcherProfileRest.ITEM, method = "getItem"),
        @LinkRest(name = ResearcherProfileRest.EPERSON, method = "getEPerson")
})
public class ResearcherProfileRest extends BaseObjectRest<UUID> {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY = RestModel.CRIS;
    public static final String NAME = "profile";

    public static final String ITEM = "item";
    public static final String EPERSON = "eperson";

    private boolean visible;

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<?> getController() {
        return RestResourceController.class;
    }

}
