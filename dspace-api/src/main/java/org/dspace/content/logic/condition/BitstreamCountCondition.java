/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.util.List;

import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * A condition to evaluate an item based on how many bitstreams it has in a particular bundle
 *
 * @author Kim Shepherd
 */
public class BitstreamCountCondition extends AbstractCondition {
    /**
     * Return true if bitstream count is within bounds of min and/or max parameters
     * Return false if out of bounds
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of evaluation
     * @throws LogicalStatementException
     */
    @Override
    public boolean getResult(Context context, Item item) throws LogicalStatementException {

        // This super call just throws some useful exceptions if required objects are null
        super.getResult(context, item);

        int min = -1;
        if (getParameters().get("min") != null) {
            min = Integer.parseInt((String)getParameters().get("min"));
        }
        int max = -1;
        if (getParameters().get("max") != null) {
            max = Integer.parseInt((String)getParameters().get("max"));
        }
        String bundleName = (String)getParameters().get("bundle");
        if (min < 0 && max < 0) {
            throw new LogicalStatementException("Either min or max parameter must be 0 or bigger.");
        }

        List<Bundle> bundles;
        int count = 0;

        if (bundleName != null) {
            bundles = item.getBundles(bundleName);
        } else {
            bundles = item.getBundles();
        }

        for (Bundle bundle : bundles) {
            count += bundle.getBitstreams().size();
        }

        if (min < 0) {
            return (count <= max);
        }
        if (max < 0) {
            return (count >= min);
        }
        return (count <= max && count >= min);
    }
}
