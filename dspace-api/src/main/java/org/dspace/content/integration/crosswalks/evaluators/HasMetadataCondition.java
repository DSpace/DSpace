/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.evaluators;

import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.util.List;

import org.apache.commons.codec.binary.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.security.service.MetadataSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ConditionEvaluator} to evaluate
 * if the given item has the specified metadata.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class HasMetadataCondition extends ConditionEvaluator {

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataSecurityService metadataSecurityService;

    @Override
    protected boolean doTest(Context context, Item item, String condition, int place) {
        String[] conditionSections = condition.split("\\.");
        if (conditionSections.length != 2) {
            throw new IllegalArgumentException("Invalid has metadata condition: " + condition);
        }

        String metadataField = conditionSections[1].replaceAll("-", ".");

        List<MetadataValue> metadata = metadataSecurityService.getPermissionFilteredMetadataValues(context,
            item, metadataField);
        if (place != -1) {
            return metadata.size() > place
                    && !StringUtils.equals(metadata.get(place).getValue(), PLACEHOLDER_PARENT_METADATA_VALUE);
        } else {
            return metadata.stream().filter(m -> !StringUtils.equals(m.getValue(), PLACEHOLDER_PARENT_METADATA_VALUE))
                    .findFirst().isPresent();
        }
    }

}
