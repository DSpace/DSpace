/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.clarin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This is NOT a service.
 * This class is representing a featured service in the ref box (item view). The featured services are defined
 * in the `clarin-dspace.cfg` file.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinFeaturedService {

    public ClarinFeaturedService() {
    }

    /**
     * The name of the Featured Service e.g. `kontext`
     */
    private String name;

    /**
     * The URL of the Featured Service, it's where the user will be redirected after clicking on it in the ref box.
     */
    private String url;

    /**
     * Some description of the Featured service.
     */
    private String description;

    /**
     * Links for the Featured Services in the more languages.
     */
    private List<ClarinFeaturedServiceLink> featuredServiceLinks;

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

    public List<ClarinFeaturedServiceLink> getFeaturedServiceLinks() {
        if (Objects.isNull(featuredServiceLinks)) {
            featuredServiceLinks = new ArrayList<>();
        }
        return featuredServiceLinks;
    }

    public void setFeaturedServiceLinks(List<ClarinFeaturedServiceLink> featuredServiceLinks) {
        this.featuredServiceLinks = featuredServiceLinks;
    }
}
