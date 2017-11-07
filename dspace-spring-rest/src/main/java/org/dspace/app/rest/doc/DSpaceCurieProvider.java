package org.dspace.app.rest.doc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.RestModel;
import org.springframework.hateoas.IanaRels;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.hal.CurieProvider;
import org.springframework.hateoas.hal.DefaultCurieProvider;
import org.springframework.stereotype.Component;

/**
 * {@link CurieProvider} implementation to provide CURIE links to the documentation
 */
@Component
public class DSpaceCurieProvider extends DefaultCurieProvider {

    private static final String DEFAULT_CURIE = "core";

    public DSpaceCurieProvider() {
        super(new HashMap<>());
    }

    @Override
    public String getNamespacedRelFrom(Link link) {
        //Used for the links section
        return getNamespacedRelFromHref(link.getRel(), link.getHref());
    }

    @Override
    public String getNamespacedRelFor(String rel) {
        //Used for the embedded section
        return getNamespacedRelFromHref(rel, null);
    }

    @Override
    public Collection<? extends Object> getCurieInformation(Links links) {
        Map<String, Curie> result = new TreeMap<>();

        for (Link link : links) {
            String curieName = getCurie(link.getHref());

            if(!result.containsKey(curieName)) {
                UriTemplate template = new UriTemplate("/documentation/" + curieName + "/{rel}.html");
                result.put(curieName, new Curie(curieName, getCurieHref(curieName, template)));
            }
        }

        return Collections.unmodifiableCollection(result.values());
    }

    public String getNamespacedRelFor(String category, String name) {
        String curie = getCurieForCategory(category);
        return String.format("%s:%s", curie, name);
    }

    public String getNamespacedRelFor(RestModel data, String name) {
        return getNamespacedRelFor(data.getCategory(), name);
    }

    private String getCurieForCategory(final String category) {
        //TODO define a mapping in XML or a properties file
        return category;
    }

    private String getNamespacedRelFromHref(String rel, String href) {
        String curie = getCurie(href);

        boolean prefixingNeeded = !IanaRels.isIanaRel(rel) && !rel.contains(":");
        return prefixingNeeded ? String.format("%s:%s", curie, rel) : rel;
    }

    private String getCurie(String href) {
        String category = extractRestCategory(href);

        String curie;
        if(StringUtils.isBlank(category)) {
            curie = DEFAULT_CURIE;
        } else {
            curie = getCurieForCategory(category);
        }

        return curie;
    }

    private String extractRestCategory(String href) {
        return StringUtils.substringBefore(
                StringUtils.substringAfter(
                        StringUtils.trimToNull(href),
                        "/api/"),
                "/");
    }

}
