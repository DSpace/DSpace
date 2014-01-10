/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceItem;

import com.lyncode.xoai.dataprovider.data.AbstractItemIdentifier;
import com.lyncode.xoai.dataprovider.filter.AbstractFilter;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public abstract class DSpaceFilter extends AbstractFilter
{
    private static Logger log = LogManager.getLogger(DSpaceFilter.class);
    private Context _ctx = null;

    public void initialize(Context ctx)
    {
        _ctx = ctx;
    }

    public Context getContext()
    {
        return _ctx;
    }

    /**
     * Returns null if no where given. Or non empty if some where is given.
     * 
     * @param context
     */
    public abstract DatabaseFilterResult getWhere(Context context);

    public abstract SolrFilterResult getQuery();

    public abstract boolean isShown(DSpaceItem item);

    @Override
    public boolean isItemShown(AbstractItemIdentifier item)
    {
        if (item instanceof DSpaceItem)
        {
            boolean value = this.isShown((DSpaceItem) item);
            if (!value) log.debug("Item "+item.getIdentifier()+" not shown because of filter "+this.getClass().getName());
            return value;
        }
        return false;
    }
}
