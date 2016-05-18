/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Hibernate implementation used by DSO Database Access Objects , includes commonly used methods
 * Each DSO Database Access Objects should extend this class to prevent code duplication.
 *
 * @author kevinvandevelde at atmire.com
 * @param <T> class type
 */
public abstract class AbstractHibernateDSODAO<T extends DSpaceObject> extends AbstractHibernateDAO<T>
{
    public T findByLegacyId(Context context, int legacyId, Class<T> clazz) throws SQLException
    {
        Criteria criteria = createCriteria(context, clazz);
        criteria.add(Restrictions.eq("legacyId", legacyId));
        return uniqueResult(criteria);
    }


    /**
     * Add left outer join on all metadata fields which are passed to this function.
     * The identifier of the join will be the toString() representation of the metadata field.
     * The joineded metadata fields can then be used to query or sort.
     * @param query
     * @param tableIdentifier
     * @param metadataFields
     */
    protected void addMetadataLeftJoin(StringBuilder query, String tableIdentifier, Collection<MetadataField> metadataFields)
    {
        for (MetadataField metadataField : metadataFields) {
            query.append(" left join ").append(tableIdentifier).append(".metadata ").append(metadataField.toString());
            query.append(" WITH ").append(metadataField.toString()).append(".metadataField.id").append(" = :").append(metadataField.toString());
        }
    }

    /**
     * Using the metadata tables mapped in the leftJoin, this function creates a where query which can check the values
     * Values can be checked using a like or an "=" query, this is determined by the "operator" parameter
     * When creating a query, the "queryParam" string can be used set as parameter for the query.
     *
     * @param query the already existing query builder, all changes will be appended
     * @param metadataFields the metadatafields who's metadata value should be queried
     * @param operator can either be "=" or "like"
     * @param additionalWhere additional where query
     */
    protected void addMetadataValueWhereQuery(StringBuilder query, List<MetadataField> metadataFields, String operator, String additionalWhere)
    {
        if(CollectionUtils.isNotEmpty(metadataFields) || StringUtils.isNotBlank(additionalWhere)){
            //Add the where query on metadata
            query.append(" WHERE ");
            for (int i = 0; i < metadataFields.size(); i++) {
                MetadataField metadataField = metadataFields.get(i);
                if(StringUtils.isNotBlank(operator))
                {
                    query.append(" (");
                    query.append("lower(STR(" + metadataField.toString()).append(".value)) ").append(operator).append(" lower(:queryParam)");
                    query.append(")");
                    if(i < metadataFields.size() - 1)
                    {
                        query.append(" OR ");
                    }
                }
            }

            if(StringUtils.isNotBlank(additionalWhere))
            {
                if(CollectionUtils.isNotEmpty(metadataFields))
                {
                    query.append(" OR ");
                }
                query.append(additionalWhere);
            }

        }
    }

    protected void addMetadataSortQuery(StringBuilder query, List<MetadataField> metadataSortFields, List<String> columnSortFields)
    {

        if(CollectionUtils.isNotEmpty(metadataSortFields)){
            query.append(" ORDER BY ");
            for (int i = 0; i < metadataSortFields.size(); i++) {
                MetadataField metadataField = metadataSortFields.get(i);
                query.append("STR(").append(metadataField.toString()).append(".value)");
                if(i != metadataSortFields.size() -1)
                {
                    query.append(",");
                }
            }
        }else if(CollectionUtils.isNotEmpty(columnSortFields))
        {
            query.append(" ORDER BY ");
            for (int i = 0; i < columnSortFields.size(); i++) {
                String sortField = columnSortFields.get(i);
                query.append(sortField);
                if(i != columnSortFields.size() -1)
                {
                    query.append(",");
                }
            }
        }
    }

}
