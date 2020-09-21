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
 * The EditItem REST Resource
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 */
public class EditItemRest extends AInprogressSubmissionRest<UUID> {

    private static final long serialVersionUID = 964876735342568998L;
    public static final String NAME = "edititem";
    public static final String CATEGORY = RestAddressableModel.SUBMISSION;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

}
