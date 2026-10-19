/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.LinksRest;
import org.dspace.app.rest.model.RestAddressableModel;

/**
 * The REST object for the Linkset objects.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
@LinksRest
public class LinksetRest extends RestAddressableModel {
    public static final String NAME = "linkset";
    public static final String PLURAL_NAME = "linksets";
    public static final String CATEGORY = RestAddressableModel.CORE;

    public static final String JSON = "json";

    @JsonInclude(Include.NON_EMPTY)
    private List<Linkset> linkset;

    public List<Linkset> getLinkset() {
        if (this.linkset == null) {
            this.linkset = new ArrayList<>();
        }
        return linkset;
    }
    public void setLinkset(List<Linkset> linkset) {
        this.linkset = linkset;
    }

    @JsonIgnore
    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }
}
