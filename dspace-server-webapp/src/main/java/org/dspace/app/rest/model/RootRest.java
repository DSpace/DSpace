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
 * The purpose of this class is to show the representation of information on the home/root page of the REST API
 */
public class RootRest extends RestAddressableModel {
    public static final String NAME = "root";
    public static final String CATEGORY = RestModel.ROOT;
    private String dspaceUI;
    private String dspaceName;
    private String dspaceServer;
    private String dspaceVersion;

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Class getController() {
        return RootRestResourceController.class;
    }

    public String getDspaceUI() {

        return dspaceUI;
    }

    public void setDspaceUI(String dspaceUI) {
        this.dspaceUI = dspaceUI;
    }

    public String getDspaceName() {
        return dspaceName;
    }

    public void setDspaceName(String dspaceName) {
        this.dspaceName = dspaceName;
    }

    public String getDspaceServer() {
        return dspaceServer;
    }

    public void setDspaceServer(String dspaceServerURL) {
        this.dspaceServer = dspaceServerURL;
    }

    public String getDspaceVersion() {
        return dspaceVersion;
    }

    public void setDspaceVersion(String dspaceVersion) {
        this.dspaceVersion = dspaceVersion;
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof RootRest &&
            new EqualsBuilder().append(this.getCategory(), ((RootRest) object).getCategory())
                               .append(this.getType(), ((RootRest) object).getType())
                               .append(this.getController(), ((RootRest) object).getController())
                               .append(this.getDspaceUI(), ((RootRest) object).getDspaceUI())
                               .append(this.getDspaceName(), ((RootRest) object).getDspaceName())
                               .append(this.getDspaceServer(), ((RootRest) object).getDspaceServer())
                               .isEquals());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.getCategory())
            .append(this.getType())
            .append(this.getController())
            .append(this.getDspaceName())
            .append(this.getDspaceUI())
            .append(this.getDspaceServer())
            .toHashCode();
    }
}
