/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.LinksRest;
import org.dspace.app.rest.model.RestAddressableModel;

/**
 * The REST object represents Typed Link.
 */
@LinksRest
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TypedLinkRest extends RestAddressableModel {
    public static final String NAME = "linkset";
    public static final String PLURAL_NAME = "linksets";
    public static final String CATEGORY = RestAddressableModel.CORE;

    private String href;

    private LinksetRelationType rel;

    private String type;

    public TypedLinkRest() {
    }

    public TypedLinkRest(String href, LinksetRelationType rel, String type) {
        this.href = href;
        this.rel = rel;
        this.type = type;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public LinksetRelationType getRel() {
        return rel;
    }

    public void setRel(LinksetRelationType rel) {
        this.rel = rel;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getType() {
        return type;
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
