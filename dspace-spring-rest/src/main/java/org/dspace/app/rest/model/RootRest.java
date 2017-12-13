/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.app.rest.RootRestResourceController;

/**
 * Created by raf on 26/09/2017.
 */
public class RootRest implements RestModel {
    public static final String NAME = "root";
    public static final String CATEGORY = RestModel.ROOT;
    private String dspaceURL;
    private String dspaceName;

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Class getController() {
        return RootRestResourceController.class;
    }
    public String getDspaceURL(){

        return dspaceURL;
    }

    public void setDspaceURL(String dspaceURL) {
        this.dspaceURL = dspaceURL;
    }

    public String getDspaceName(){
        return dspaceName;
    }
    public void setDspaceName(String dspaceName) {
        this.dspaceName = dspaceName;
    }

    @Override
    public boolean equals(Object object){
        return (object instanceof RootRest &&
                new EqualsBuilder().append(this.getCategory(), ((RootRest) object).getCategory())
                        .append(this.getType(), ((RootRest) object).getType())
                        .append(this.getController(), ((RootRest) object).getController())
                        .append(this.getDspaceURL(), ((RootRest) object).getDspaceURL())
                        .append(this.getDspaceName(), ((RootRest) object).getDspaceName())
                        .isEquals());
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.getCategory())
                .append(this.getType())
                .append(this.getController())
                .append(this.getDspaceURL())
                .append(this.getDspaceURL())
                .toHashCode();
    }
}
