/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

/**
 * This class is the REST representation of the EntityType model object and acts as a data object
 * for the EntityTypeResource class.
 * Refer to {@link org.dspace.content.EntityType} for explanation of the properties
 */
@LinksRest(links = {
        @LinkRest(
                name = EntityTypeRest.RELATION_SHIP_TYPES,
                method = "getEntityTypeRelationship"
        )
})
public class EntityTypeRest extends BaseObjectRest<Integer> {

    private static final long serialVersionUID = 8166078961459192770L;

    public static final String NAME = "entitytype";
    public static final String PLURAL_NAME = "entitytypes";
    public static final String CATEGORY = RestModel.CORE;
    public static final String RELATION_SHIP_TYPES = "relationshiptypes";

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return RestResourceController.class;
    }

    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    private String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
