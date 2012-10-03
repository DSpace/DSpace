/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import java.sql.SQLException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceDatabaseItem;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;
import org.dspace.xoai.util.MetadataFieldManager;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceMetadataExistsFilter extends DSpaceFilter
{
    private static Logger log = LogManager
            .getLogger(DSpaceMetadataExistsFilter.class);

    private String _field;

    private String getField()
    {
        if (this._field == null)
        {
            _field = super.getParameter("field");
        }
        return _field;
    }

    @Override
    public DatabaseFilterResult getWhere(Context context)
    {
        try
        {
            return new DatabaseFilterResult(
                    "EXISTS (SELECT tmp.* FROM metadatavalue tmp WHERE tmp.item_id=i.item_id AND tmp.metadata_field_id=?)",
                    MetadataFieldManager.getFieldID(context, this.getField()));
        }
        catch (InvalidMetadataFieldException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return new DatabaseFilterResult();
    }

    @Override
    public boolean isShown(DSpaceDatabaseItem item)
    {
        if (item.getMetadata(this.getField()+".*").size() > 0)
            return true;

        return false;
    }

    @Override
    public SolrFilterResult getQuery()
    {
        return new SolrFilterResult("metadata." + this.getField() + ":[* TO *]");
    }

}
