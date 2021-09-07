/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;

/**
 * Hibernate implementation used by DSpaceObject Database Access Objects.
 * Includes commonly used methods.
 *
 * <p>
 * Each DSO Database Access Object should extend this class to prevent code duplication.
 *
 * @author kevinvandevelde at atmire.com
 * @param <T> type of DSO represented.
 */
public abstract class AbstractHibernateDSODAO<T extends DSpaceObject> extends AbstractHibernateDAO<T> {
    /**
     * Find a DSO by its "legacy ID".  Former versions of DSpace used integer
     * record IDs, and these may still be found in external records such as AIPs.
     * All DSOs now have UUID primary keys, and those should be used when available.
     * Each type derived from DSpaceObject had its own stream of record IDs, so
     * it is also necessary to know the specific type.
     * @param context current DSpace context.
     * @param legacyId the old integer record identifier.
     * @param clazz DSO subtype of record identified by {@link legacyId}.
     * @return
     * @throws SQLException
     */
    public T findByLegacyId(Context context, int legacyId, Class<T> clazz) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, clazz);
        Root<T> root = criteriaQuery.from(clazz);
        criteriaQuery.where(criteriaBuilder.equal(root.get("legacyId"), legacyId));
        return uniqueResult(context, criteriaQuery, false, clazz);
    }

    /**
     * Add left outer join on all metadata fields which are passed to this function.
     * The identifier of the join will be the toString() representation of the metadata field.
     * The joined metadata fields can then be used to query or sort.
     * @param query the query string being built.
     * @param tableIdentifier name of the table to be joined.
     * @param metadataFields names of the desired fields.
     */
    protected void addMetadataLeftJoin(StringBuilder query, String tableIdentifier,
                                       Collection<MetadataField> metadataFields) {
        for (MetadataField metadataField : metadataFields) {
            query.append(" left join ").append(tableIdentifier).append(".metadata ").append(metadataField.toString());
            query.append(" WITH ").append(metadataField.toString()).append(".metadataField.id").append(" = :")
                 .append(metadataField.toString());
        }
    }

    /**
     * Using the metadata tables mapped in the leftJoin, this function creates a where query which can check the values.
     * Values can be checked using a like or an "=" query, as determined by the "operator" parameter.
     * When creating a query, the "queryParam" string can be used set as parameter for the query.
     *
     * @param query the already existing query builder, all changes will be appended
     * @param metadataFields the metadata fields whose metadata value should be queried
     * @param operator can either be "=" or "like"
     * @param additionalWhere additional where query
     */
    protected void addMetadataValueWhereQuery(StringBuilder query, List<MetadataField> metadataFields, String operator,
                                              String additionalWhere) {
        if (CollectionUtils.isNotEmpty(metadataFields) || StringUtils.isNotBlank(additionalWhere)) {
            //Add the where query on metadata
            query.append(" WHERE ");
            for (int i = 0; i < metadataFields.size(); i++) {
                MetadataField metadataField = metadataFields.get(i);
                if (StringUtils.isNotBlank(operator)) {
                    query.append(" (");
                    query.append("lower(STR(" + metadataField.toString()).append(".value)) ").append(operator)
                         .append(" lower(:queryParam)");
                    query.append(")");
                    if (i < metadataFields.size() - 1) {
                        query.append(" OR ");
                    }
                }
            }

            if (StringUtils.isNotBlank(additionalWhere)) {
                if (CollectionUtils.isNotEmpty(metadataFields)) {
                    query.append(" OR ");
                }
                query.append(additionalWhere);
            }

        }
    }

    /**
     * Append ORDER BY clause based on metadata fields or column names.
     * All fields will be in ascending order.
     * @param query the query being built.
     * @param metadataSortFields fields on which to sort -- use this OR columnSortFields.
     * @param columnSortFields columns on which to sort -- use this OR metadataSortFields.
     */
    protected void addMetadataSortQuery(StringBuilder query, List<MetadataField> metadataSortFields,
                                        List<String> columnSortFields) {
        addMetadataSortQuery(query, metadataSortFields, columnSortFields, ListUtils.EMPTY_LIST);
    }

    /**
     * Append ORDER BY clause based on metadata fields or column names.
     * @param query the query being built.
     * @param metadataSortFields fields on which to sort -- use this OR columnSortFields.
     * @param columnSortFields columns on which to sort -- use this OR metadataSortFields.
     * @param direction ASC or DESC for each field.  Unspecified fields will be ASC.
     */
    protected void addMetadataSortQuery(StringBuilder query, List<MetadataField> metadataSortFields,
                                        List<String> columnSortFields, List<String> direction) {

        if (CollectionUtils.isNotEmpty(metadataSortFields)) {
            query.append(" ORDER BY ");
            for (int i = 0; i < metadataSortFields.size(); i++) {
                MetadataField metadataField = metadataSortFields.get(i);
                query.append("STR(").append(metadataField.toString()).append(".value)");
                String dir = direction.size() > i ? " " + direction.get(i) : "";
                query.append(dir);
                if (i != metadataSortFields.size() - 1) {
                    query.append(",");
                }
            }
        } else if (CollectionUtils.isNotEmpty(columnSortFields)) {
            query.append(" ORDER BY ");
            for (int i = 0; i < columnSortFields.size(); i++) {
                String sortField = columnSortFields.get(i);
                query.append(sortField);
                if (i != columnSortFields.size() - 1) {
                    query.append(",");
                }
            }
        }
    }

}
