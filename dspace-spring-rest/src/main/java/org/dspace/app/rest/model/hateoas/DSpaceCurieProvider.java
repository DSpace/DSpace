/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.RestModel;
import org.springframework.beans.factory.annotation.Value;
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

    private static final String SEPERATOR = ":";

    private static final String SELF = "self";

    private static final String DEFAULT_CURIE = "core";

    @Value("#{${curiemapping}}")  private Map<String,String> curiemapping;

    public DSpaceCurieProvider() {
        super(new HashMap<>());
    }

    @Override
    public String getNamespacedRelFrom(Link link) {
        //Used for the links section
        return getNamespacedRelFor(link.getRel());
    }

    public String getNamespacedRelFor(String rel) {
        //TODO CLeanup
        String category = extractPrefix(rel);
        String coreRel = extractRel(rel);

        String curie = getCurieForCategory(category);

        return buildRel(curie, coreRel);
    }

    private String extractRel(String rel) {
        if(StringUtils.containsNone(rel, SEPERATOR)){
            return rel;
        }
        return rel.split(SEPERATOR)[1];
    }

    private String extractPrefix(String rel) {
        //TODO Cleanup
        if(StringUtils.containsNone(rel, SEPERATOR)){
            return DEFAULT_CURIE;
        }
        return rel.split(SEPERATOR)[0];
    }

    @Override
    public Collection<? extends Object> getCurieInformation(Links links) {
        Map<String, Curie> result = new TreeMap<>();

        for (Link link : links) {
            String category = extractPrefix(link.getRel());
            String curieName = getCurieForCategory(category);

            if(!result.containsKey(curieName)) {
                UriTemplate template = new UriTemplate("/documentation/" + curieName + "/{rel}.html");
                result.put(curieName, new Curie(curieName, getCurieHref(curieName, template)));
            }
        }

        return Collections.unmodifiableCollection(result.values());
    }

    public String getNamespacedRelFor(String category, String rel) {
        return buildRel(category, rel);
    }

    public String getNamespacedRelFor(RestModel data, String rel) {
        return getNamespacedRelFor(data.getCategory(), rel);
    }

    public String getCurieForCategory(final String category) {
        //TODO create mapping
        return curiemapping.get(category);
    }

    private String buildRel(String prefix, String rel) {
        boolean prefixingNeeded = StringUtils.isNotBlank(prefix) && !IanaRels.isIanaRel(rel) && !rel.contains(":");
        return prefixingNeeded ? String.format("%s"+SEPERATOR+"%s", prefix, rel) : rel;
    }


}
