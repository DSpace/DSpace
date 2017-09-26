package org.dspace.app.rest.model;

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
}
