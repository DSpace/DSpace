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
 * The EditItem REST Resource
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 */
@LinksRest(
    links = {
        @LinkRest(name = EditItemRest.MODE, method = "getModes"),
        @LinkRest(name = EditItemRest.ITEM, method = "getEditItemItem"),
        @LinkRest(name = EditItemRest.COLLECTION, method = "getEditItemCollection")
    }
)
public class EditItemRest extends AInprogressSubmissionRest<String> {

    private static final long serialVersionUID = 964876735342568998L;
    public static final String NAME = "edititem";
    public static final String NAME_PLURAL = "edititems";
    public static final String MODE = "modes";
    public static final String CATEGORY = RestAddressableModel.CORE;

    public static final String ITEM = "item";
    public static final String COLLECTION = "collection";

    /**
     * Returns the category of this REST resource.
     * <p>
     * The category is used to group related REST resources together
     * and is part of the REST API URL structure.
     *
     * @return the category constant for this resource (CORE)
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /**
     * Returns the type identifier for this REST resource.
     * <p>
     * The type is used to identify this specific resource type within
     * the REST API and forms part of the resource URL path.
     *
     * @return the singular type name for this resource ("edititem")
     */
    @Override
    public String getType() {
        return NAME;
    }

    /**
     * Returns the plural form of the type identifier for this REST resource.
     * <p>
     * The plural type is used when referring to collections of this resource
     * type in the REST API endpoints.
     *
     * @return the plural type name for this resource ("edititems")
     */
    @Override
    public String getTypePlural() {
        return NAME_PLURAL;
    }

    /**
     * Returns the controller class responsible for handling REST requests
     * for this resource type.
     * <p>
     * The controller class defines the HTTP endpoints and request handlers
     * that operate on EditItem resources.
     *
     * @return the RestResourceController class that handles this resource type
     */
    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

}
