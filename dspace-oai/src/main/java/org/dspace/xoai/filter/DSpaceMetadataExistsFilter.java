/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;
import org.dspace.xoai.filter.DSpaceFilter;
import org.dspace.xoai.filter.DatabaseFilterResult;
import org.dspace.xoai.filter.SolrFilterResult;
import org.dspace.xoai.util.MetadataFieldManager;

/**
 * This filter allows one to retrieve (from the data source) those items 
 * which contains at least one metadata field value defined, it allows
 * one to define multiple metadata fields to check against.
 *
 * One line summary: At least one metadata field defined
 *
 * @author Ariel J. Lira <arieljlira@gmail.com>
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceMetadataExistsFilter extends DSpaceFilter
{
    private static Logger log = LogManager
            .getLogger(DSpaceMetadataExistsFilter.class);

    private List<String> _fields;

    private List<String> getFields()
    {
        if (this._fields == null)
        {
            this._fields = super.getParameters("fields");

            //backwards compatibility check for field parameter
            if (this._fields.isEmpty())
                this._fields = super.getParameters("field");

        }
        return _fields;
    }

    @Override
    public DatabaseFilterResult getWhere(Context context)
    {
        try
        {
        	List<String> fields = this.getFields();
        	StringBuilder where = new StringBuilder();
        	List<Object> args = new ArrayList<Object>(fields.size());
        	where.append("(");
        	for (int i = 0; i < fields.size(); i++) {
        		where.append("EXISTS (SELECT tmp.* FROM metadatavalue tmp WHERE tmp.item_id=i.item_id AND tmp.metadata_field_id=?)");
        		args.add(MetadataFieldManager.getFieldID(context, fields.get(i)));
        		
        		if (i < fields.size() -1 )
					where.append(" OR ") ;
        	}
        	where.append(")");
        	
        	return new DatabaseFilterResult(where.toString(), args);
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
    public boolean isShown(DSpaceItem item)
    {
    	for (String field : this.getFields()) {
		    //do we have a match? if yes, our job is done
    		if (item.getMetadata(field).size() > 0)
		    	return true;
		}
        return false;
    }

    @Override
    public SolrFilterResult getQuery()
    {
    	StringBuilder cond = new StringBuilder("(");
    	List<String> fields = this.getFields();
    	for (int i = 0; i < fields.size(); i++) {
    		cond.append("metadata.").append(fields.get(i)).append(":[* TO *]");
			if (i < fields.size() -1 )
				cond.append(" OR ") ;
		}
    	cond.append(")");
    	
    	return new SolrFilterResult(cond.toString());
    }

}
