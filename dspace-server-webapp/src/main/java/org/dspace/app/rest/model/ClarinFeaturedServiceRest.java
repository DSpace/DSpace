/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.dspace.app.rest.RestResourceController;

/**
 * The FeaturedService REST Resource
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinFeaturedServiceRest extends BaseObjectRest<Integer> {

    public static final String NAME = "featuredservice";
    public static final String CATEGORY = RestAddressableModel.CORE;

    private String name;
    private String url;
    private String description;
    private List<ClarinFeaturedServiceLinkRest> featuredServiceLinks;

    public ClarinFeaturedServiceRest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ClarinFeaturedServiceLinkRest> getFeaturedServiceLinks() {
        if (Objects.isNull(featuredServiceLinks)) {
            featuredServiceLinks = new ArrayList<>();
        }
        return featuredServiceLinks;
    }

    public void setFeaturedServiceLinks(List<ClarinFeaturedServiceLinkRest> featuredServiceLinks) {
        this.featuredServiceLinks = featuredServiceLinks;
    }

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
}
