/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import com.lyncode.xoai.dataprovider.data.Filter;
import com.lyncode.xoai.dataprovider.data.ItemIdentifier;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterMap;

import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.SolrFilterResult;
import org.dspace.xoai.services.api.FieldResolver;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public abstract class DSpaceFilter implements Filter
{
    /** The configuration from xoai.xml file */
    protected ParameterMap configuration;

    /** The configuration from xoai.xml file */
    protected FieldResolver fieldResolver;

    /** The oai context */
    protected Context context;

    public abstract SolrFilterResult buildSolrQuery();
    public abstract boolean isShown(DSpaceItem item);

    @Override
    public boolean isItemShown(ItemIdentifier item)
    {
        if (item instanceof DSpaceItem)
        {
            return isShown((DSpaceItem) item);
        }
        return false;
    }

    /**
     * @return the configuration map if defined in xoai.xml, otherwise null.
     */
    public ParameterMap getConfiguration()
    {
        return configuration;
    }

    /**
     * @param configuration
     *            the configuration map to set
     */
    public void setConfiguration(ParameterMap configuration)
    {
        this.configuration = configuration;
    }

    /**
     * @return the fieldResolver
     */
    public FieldResolver getFieldResolver()
    {
        return fieldResolver;
    }

    /**
     * @param fieldResolver
     *            the fieldResolver to set
     */
    public void setFieldResolver(FieldResolver fieldResolver)
    {
        this.fieldResolver = fieldResolver;
    }

    /**
     * @return the context
     */
    public Context getContext()
    {
        return context;
    }

    /**
     * @param context
     *            the context to set
     */
    public void setContext(Context context)
    {
        this.context = context;
    }
}
