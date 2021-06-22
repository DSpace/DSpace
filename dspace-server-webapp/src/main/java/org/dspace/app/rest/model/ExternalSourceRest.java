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
 * This class serves as a REST representation for an External Source
 */
public class ExternalSourceRest extends BaseObjectRest<String> {

    public static final String NAME = "externalsource";
    public static final String PLURAL_NAME = "externalsources";
    public static final String CATEGORY = RestAddressableModel.INTEGRATION;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }

    private String id;
    private String name;
    private boolean hierarchical;

    /**
     * Generic getter for the id
     * @return the id value of this ExternalSourceRest
     */
    public String getId() {
        return id;
    }

    /**
     * Generic setter for the id
     * @param id   The id to be set on this ExternalSourceRest
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Generic getter for the name
     * @return the name value of this ExternalSourceRest
     */
    public String getName() {
        return name;
    }

    /**
     * Generic setter for the name
     * @param name   The name to be set on this ExternalSourceRest
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Generic getter for the hierarchical
     * @return the hierarchical value of this ExternalSourceRest
     */
    public boolean isHierarchical() {
        return hierarchical;
    }

    /**
     * Generic setter for the hierarchical
     * @param hierarchical   The hierarchical to be set on this ExternalSourceRest
     */
    public void setHierarchical(boolean hierarchical) {
        this.hierarchical = hierarchical;
    }
}
