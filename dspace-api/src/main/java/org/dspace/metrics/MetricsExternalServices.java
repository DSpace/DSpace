/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics;
import java.util.Collections;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface MetricsExternalServices {

    public boolean updateMetric(Context context, Item item, String param);

    public default List<String> getFilters() {
        return Collections.EMPTY_LIST;
    }
}
