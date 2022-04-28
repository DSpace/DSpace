/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * A condition that returns true if a pattern (regex) matches any value
 * in a given metadata field
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class MetadataValueMatchCondition extends AbstractCondition {

    private final static Logger log = LogManager.getLogger();

    /**
     * Return true if any value for a specified field in the item matches a specified regex pattern
     * Return false if not
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of evaluation
     * @throws LogicalStatementException
     */
    @Override
    public boolean getResult(Context context, Item item) throws LogicalStatementException {
        String field = (String)getParameters().get("field");
        if (field == null) {
            return false;
        }

        String[] fieldParts = field.split("\\.");
        String schema = (fieldParts.length > 0 ? fieldParts[0] : null);
        String element = (fieldParts.length > 1 ? fieldParts[1] : null);
        String qualifier = (fieldParts.length > 2 ? fieldParts[2] : null);

        List<MetadataValue> values = itemService.getMetadata(item, schema, element, qualifier, Item.ANY);
        for (MetadataValue value : values) {
            if (getParameters().get("pattern") instanceof String) {
                String pattern = (String)getParameters().get("pattern");
                log.debug("logic for " + item.getHandle() + ": pattern passed is " + pattern
                    + ", checking value " + value.getValue());
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(value.getValue());
                if (m.find()) {
                    return true;
                }
            }
        }
        return false;
    }
}
