/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * A condition that returns true if the item has at least one value for the
 * configured metadata field.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class RequiredMetadataCondition extends AbstractCondition {

    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {
        String field = (String) getParameters().get("field");
        if (field == null) {
            return false;
        }

        return CollectionUtils.isNotEmpty(itemService.getMetadataByMetadataString(item, field));
    }

}
