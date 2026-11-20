/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.evaluators;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ConditionEvaluator} that evaluate if the given object
 * has or not the authority set on the metadata in the given condition.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class AuthorityNotBlankCondition extends ConditionEvaluator {

    @Autowired
    private ItemService itemService;

    @Override
    public boolean doTest(Context context, Item item, String condition, int place) {

        String[] conditionSections = condition.split("\\.");
        if (conditionSections.length != 2) {
            throw new IllegalArgumentException("Invalid authority condition: " + condition);
        }

        String metadataField = conditionSections[1].replaceAll("-", ".");

        List<MetadataValue> metadata = itemService.getMetadataByMetadataString(item, metadataField);
        if (CollectionUtils.isEmpty(metadata)) {
            return false;
        }

        if (place != -1) {
            return metadata.stream().skip(place).limit(1).map(metadataValue -> metadataValue.getAuthority())
                    .allMatch(authority -> isNotBlank(authority));
        } else {
            return metadata.stream()
                .map(metadataValue -> metadataValue.getAuthority())
                .allMatch(authority -> isNotBlank(authority));
        }
    }

}
