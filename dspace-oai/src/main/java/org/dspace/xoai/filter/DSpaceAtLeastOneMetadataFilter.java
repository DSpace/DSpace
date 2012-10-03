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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceDatabaseItem;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;
import org.dspace.xoai.filter.data.DSpaceMetadataFilterOperator;
import org.dspace.xoai.util.MetadataFieldManager;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceAtLeastOneMetadataFilter extends DSpaceFilter
{
    private static Logger log = LogManager
            .getLogger(DSpaceAtLeastOneMetadataFilter.class);

    private String _field;

    private DSpaceMetadataFilterOperator _operator = DSpaceMetadataFilterOperator.UNDEF;

    private List<String> _values;

    private String _value;

    private String getField()
    {
        if (_field == null)
        {
            _field = super.getParameter("field");
        }
        return _field;
    }

    @SuppressWarnings("unused")
    private String getValue()
    {
        if (_value == null)
        {
            _value = super.getParameter("value");
        }
        return _value;
    }

    private List<String> getValues()
    {
        if (_values == null)
        {
            _values = super.getParameters("value");
        }
        return _values;
    }

    private DSpaceMetadataFilterOperator getOperator()
    {
        if (_operator == DSpaceMetadataFilterOperator.UNDEF)
        {
            _operator = DSpaceMetadataFilterOperator.valueOf(super
                    .getParameter("operator").toUpperCase());
        }
        return _operator;
    }

    @Override
    public DatabaseFilterResult getWhere(Context context)
    {
        if (this.getField() != null)
        {
            try
            {
                int id = MetadataFieldManager.getFieldID(context,
                        this.getField());
                return this.getWhere(id, this.getValues());
            }
            catch (InvalidMetadataFieldException ex)
            {
                log.error(ex.getMessage(), ex);
            }
            catch (SQLException ex)
            {
                log.error(ex.getMessage(), ex);
            }
        }
        return new DatabaseFilterResult();
    }

    @Override
    public boolean isShown(DSpaceDatabaseItem item)
    {
        if (this.getField() == null)
            return true;
        List<String> values = item.getMetadata(this.getField()+".*");
        for (String praticalValue : values)
        {
            for (String theoreticValue : this.getValues())
            {
                switch (this.getOperator())
                {
                case CONTAINS:
                    if (praticalValue.contains(theoreticValue))
                        return true;
                    break;
                case ENDS_WITH:
                    if (praticalValue.endsWith(theoreticValue))
                        return true;
                    break;
                case EQUAL:
                    if (praticalValue.equals(theoreticValue))
                        return true;
                    break;
                case GREATER:
                    if (praticalValue.compareTo(theoreticValue) > 0)
                        return true;
                    break;
                case GREATER_OR_EQUAL:
                    if (praticalValue.compareTo(theoreticValue) >= 0)
                        return true;
                    break;
                case LOWER:
                    if (praticalValue.compareTo(theoreticValue) < 0)
                        return true;
                    break;
                case LOWER_OR_EQUAL:
                    if (praticalValue.compareTo(theoreticValue) <= 0)
                        return true;
                    break;
                }
            }
        }
        return false;
    }

    private DatabaseFilterResult getWhere(int mdid, List<String> values)
    {
        List<String> parts = new ArrayList<String>();
        List<Object> params = new ArrayList<Object>();
        params.add(mdid);
        for (String v : values)
            this.buildWhere(v, parts, params);
        if (parts.size() > 0)
        {
            String query = "EXISTS (SELECT tmp.* FROM metadatavalue tmp WHERE tmp.item_id=i.item_id AND tmp.metadata_field_id=?"
                    + " AND ("
                    + StringUtils.join(parts.iterator(), " OR ")
                    + "))";
            return new DatabaseFilterResult(query, params);
        }
        return new DatabaseFilterResult();
    }

    private void buildWhere(String value, List<String> parts,
            List<Object> params)
    {
        switch (this.getOperator())
        {
        case CONTAINS:
            parts.add("(tmp.text_value LIKE ?)");
            params.add("%" + value + "%");
            break;
        case ENDS_WITH:
            parts.add("(tmp.text_value LIKE ?)");
            params.add("%" + value);
            break;
        case STARTS_WITH:
            parts.add("(tmp.text_value LIKE ?)");
            params.add(value + "%");
            break;
        case EQUAL:
            parts.add("(tmp.text_value LIKE ?)");
            params.add(value);
            break;
        case GREATER:
            parts.add("(tmp.text_value > ?)");
            params.add(value);
            break;
        case LOWER:
            parts.add("(tmp.text_value < ?)");
            params.add(value);
            break;
        case LOWER_OR_EQUAL:
            parts.add("(tmp.text_value <= ?)");
            params.add(value);
            break;
        case GREATER_OR_EQUAL:
            parts.add("(tmp.text_value >= ?)");
            params.add(value);
            break;
        }
    }

    @Override
    public SolrFilterResult getQuery()
    {
        String field = this.getField();
        List<String> parts = new ArrayList<String>();
        if (this.getField() != null)
        {
            for (String v : this.getValues())
                this.buildQuery("metadata." + field,
                        ClientUtils.escapeQueryChars(v), parts);
            if (parts.size() > 0)
            {
                return new SolrFilterResult(StringUtils.join(parts.iterator(),
                        " OR "));
            }
        }
        return new SolrFilterResult();
    }

    private void buildQuery(String field, String value, List<String> parts)
    {
        switch (this.getOperator())
        {
        case CONTAINS:
            parts.add("(" + field + ":*" + value + "*)");
            break;
        case ENDS_WITH:
            parts.add("(" + field + ":*" + value + ")");
            break;
        case STARTS_WITH:
            parts.add("(" + field + ":" + value + "*)");
            break;
        case EQUAL:
            parts.add("(" + field + ":" + value + ")");
            break;
        case GREATER:
            parts.add("(" + field + ":[" + value + " TO *])");
            break;
        case LOWER:
            parts.add("(" + field + ":[* TO " + value + "])");
            break;
        case LOWER_OR_EQUAL:
            parts.add("(-(" + field + ":[" + value + " TO *]))");
            break;
        case GREATER_OR_EQUAL:
            parts.add("(-(" + field + ":[* TO " + value + "]))");
            break;
        }
    }

}
