/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.solr.common.SolrDocument;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ItemAuthorityExtraMetadataGenerator;
import org.dspace.utils.DSpace;

/**
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public class ItemAuthorityUtils {

    private static final DSpace dspace = new DSpace();

    private ItemAuthorityUtils() {
    }

    public static Map<String, String> buildExtra(String authorityName, SolrDocument item,
                                                 List<String> objectNames, String uuid) {
        Map<String, String> extras = new HashMap<String, String>();
        List<ItemAuthorityExtraMetadataGenerator> generators = dspace.getServiceManager()
                                                                     .getServicesByType(
                                                                         ItemAuthorityExtraMetadataGenerator.class);
        if (generators != null) {
            for (ItemAuthorityExtraMetadataGenerator gg : generators) {
                Map<String, String> extrasTmp = gg.build(authorityName, item);
                extras.putAll(extrasTmp);
            }
        }
        Set<String> alternativeNames = new TreeSet<>(objectNames);
        if (alternativeNames.size() > 1) {
            String alternativeNameKey = "alternative-names";
            alternativeNames.forEach(alternativeName -> {
                String alternative = alternativeName + "::" + uuid;
                if (extras.containsKey(alternativeNameKey)) {
                    extras.put(alternativeNameKey, extras.get(alternativeNameKey) + "|||" + alternative);
                } else {
                    extras.put(alternativeNameKey, alternative);
                }
            });
        }
        return extras;
    }

    public static List<Choice> buildAggregateByExtra(String authorityName, SolrDocument item) {
        List<Choice> choiceList = new LinkedList<Choice>();
        List<ItemAuthorityExtraMetadataGenerator> generators = dspace.getServiceManager()
                                                                     .getServicesByType(
                                                                         ItemAuthorityExtraMetadataGenerator.class);
        if (generators != null) {
            for (ItemAuthorityExtraMetadataGenerator gg : generators) {
                choiceList.addAll(gg.buildAggregate(authorityName, item));
            }
        }
        return choiceList;
    }

}