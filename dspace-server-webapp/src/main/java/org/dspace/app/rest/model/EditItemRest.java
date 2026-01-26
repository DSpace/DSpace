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

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return NAME_PLURAL;
    }

    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

}
