/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.BasicTransformerAdapter;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hibernate implementation of the Database Access Object interface class for the Collection object.
 * This class is responsible for all database calls for the Collection object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class CollectionDAOImpl extends AbstractHibernateDSODAO<Collection> implements CollectionDAO
{
    protected CollectionDAOImpl()
    {
        super();
    }

    /**
     * Get all collections in the system. These are alphabetically sorted by
     * collection name.
     *
     * @param context
     *            DSpace context object
     * @param order order by MetadataField
     * @return the collections in the system
     * @throws SQLException if database error
     */
    @Override
    public List<Collection> findAll(Context context, MetadataField order) throws SQLException
    {
        return findAll(context, order, null, null);
    }

    @Override
    public List<Collection> findAll(Context context, MetadataField order, Integer limit, Integer offset) throws SQLException {
        StringBuilder query = new StringBuilder();
        query.append("SELECT ").append(Collection.class.getSimpleName()).append(" FROM Collection as ").append(Collection.class.getSimpleName()).append(" ");
        addMetadataLeftJoin(query, Collection.class.getSimpleName(), Arrays.asList(order));
        addMetadataSortQuery(query, Arrays.asList(order), null);

        Query hibernateQuery = createQuery(context, query.toString());
        if(offset != null)
        {
            hibernateQuery.setFirstResult(offset);
        }
        if(limit != null){
            hibernateQuery.setMaxResults(limit);
        }
        hibernateQuery.setParameter(order.toString(), order.getID());
        return list(hibernateQuery);
    }

    @Override
    public Collection findByTemplateItem(Context context, Item item) throws SQLException {
        Criteria criteria = createCriteria(context, Collection.class);
        criteria.add(Restrictions.eq("template_item", item));
        return uniqueResult(criteria);
    }

    @Override
    public Collection findByGroup(Context context, Group group) throws SQLException {
        Criteria criteria = createCriteria(context, Collection.class);
        criteria.add(
                Restrictions.or(
                        Restrictions.eq("workflowStep1", group),
                        Restrictions.eq("workflowStep2", group),
                        Restrictions.eq("workflowStep3", group),
                        Restrictions.eq("submitters", group),
                        Restrictions.eq("admins", group)
                )
        );
        return singleResult(criteria);
    }

    @Override
    public List<Collection> findAuthorized(Context context, EPerson ePerson, List<Integer> actions) throws SQLException {
        //        TableRowIterator tri = DatabaseManager.query(context,
//                "SELECT * FROM collection, resourcepolicy, eperson " +
//                        "WHERE resourcepolicy.resource_id = collection.collection_id AND " +
//                        "eperson.eperson_id = resourcepolicy.eperson_id AND "+
//                        "resourcepolicy.resource_type_id = 3 AND "+
//                        "( resourcepolicy.action_id = 3 OR resourcepolicy.action_id = 11 ) AND "+
//                        "eperson.eperson_id = ?", context.getCurrentUser().getID());

        Criteria criteria = createCriteria(context, Collection.class);
        criteria.createAlias("resourcePolicies", "resourcePolicy");

        Disjunction actionQuery = Restrictions.or();
        for (Integer action : actions)
        {
            actionQuery.add(Restrictions.eq("resourcePolicy.actionId", action));
        }
        criteria.add(Restrictions.and(
                Restrictions.eq("resourcePolicy.resourceTypeId", Constants.COLLECTION),
                Restrictions.eq("resourcePolicy.eperson", ePerson),
                actionQuery
        ));
        criteria.setCacheable(true);

        return list(criteria);
    }

    @Override
    public List<Collection> findAuthorizedByGroup(Context context, EPerson ePerson, List<Integer> actions) throws SQLException {
 //        TableRowIterator tri = DatabaseManager.query(context,
 //                "SELECT \n" +
 //                        "  * \n" +
 //                        "FROM \n" +
 //                        "  public.eperson, \n" +
 //                        "  public.epersongroup2eperson, \n" +
 //                        "  public.epersongroup, \n" +
 //                        "  public.group2group, \n" +
 //                        "  public.resourcepolicy rp_parent, \n" +
 //                        "  public.collection\n" +
 //                        "WHERE \n" +
 //                        "  epersongroup2eperson.eperson_id = eperson.eperson_id AND\n" +
 //                        "  epersongroup.eperson_group_id = epersongroup2eperson.eperson_group_id AND\n" +
 //                        "  group2group.child_id = epersongroup.eperson_group_id AND\n" +
 //                        "  rp_parent.epersongroup_id = group2group.parent_id AND\n" +
 //                        "  collection.collection_id = rp_parent.resource_id AND\n" +
 //                        "  eperson.eperson_id = ? AND \n" +
 //                        "  (rp_parent.action_id = 3 OR \n" +
 //                        "  rp_parent.action_id = 11  \n" +
 //                        "  )  AND rp_parent.resource_type_id = 3;", context.getCurrentUser().getID());
        StringBuilder query = new StringBuilder();
        query.append("select c from Collection c join c.resourcePolicies rp join rp.epersonGroup rpGroup WHERE ");
        for (int i = 0; i < actions.size(); i++) {
            Integer action = actions.get(i);
            if(i != 0)
            {
                query.append(" AND ");
            }
            query.append("rp.actionId=").append(action);
        }
        query.append(" AND rp.resourceTypeId=").append(Constants.COLLECTION);
        query.append(" AND rp.epersonGroup.id IN (select g.id from Group g where (from EPerson e where e.id = :eperson_id) in elements(epeople))");
        Query hibernateQuery = createQuery(context, query.toString());
        hibernateQuery.setParameter("eperson_id", ePerson.getID());
        hibernateQuery.setCacheable(true);

        return list(hibernateQuery);


    }

    @Override
    public List<Collection> findCollectionsWithSubscribers(Context context) throws SQLException {
        return list(createQuery(context, "SELECT DISTINCT col FROM Subscription s join  s.collection col"));
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Collection"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map.Entry<Collection, Long>> getCollectionsWithBitstreamSizesTotal(Context context) throws SQLException {
        String q = "select col as collection, sum(bit.sizeBytes) as totalBytes from Item i join i.collections col join i.bundles bun join bun.bitstreams bit group by col";
        Query query = createQuery(context, q);
        query.setResultTransformer(new BasicTransformerAdapter() {
            @Override
            public Object transformTuple(Object[] tuple, String[] aliases) {
                return new java.util.AbstractMap.SimpleImmutableEntry<>((Collection)tuple[0], (Long)tuple[1]);
            }
        });
        return ((List<Map.Entry<Collection, Long>>)query.list());
    }

    // Begin UMD Customization
    // Methods to support searching of collections by text_value metadata field
    // Used in EditETDDepartmentsForm.java
    // Adapted from EPersonDAOImpl.java
    protected Query getSearchQuery(Context context, String queryString, String queryParam, List<MetadataField> queryFields, List<MetadataField> sortFields, String sortField) throws SQLException {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(queryString);
        Set<MetadataField> metadataFieldsToJoin = new LinkedHashSet<>();
        metadataFieldsToJoin.addAll(queryFields);
        metadataFieldsToJoin.addAll(sortFields);

        if(!CollectionUtils.isEmpty(metadataFieldsToJoin)) {
            addMetadataLeftJoin(queryBuilder, Collection.class.getSimpleName().toLowerCase(), metadataFieldsToJoin);
        }
        if(StringUtils.isNotBlank(queryParam)) {
            addMetadataValueWhereQuery(queryBuilder, queryFields, "like", null);
        }
        if(!CollectionUtils.isEmpty(sortFields)) {
            addMetadataSortQuery(queryBuilder, sortFields, Collections.singletonList(sortField));
        }

        Query query = createQuery(context, queryBuilder.toString());
        if(StringUtils.isNotBlank(queryParam)) {
            query.setParameter("queryParam", "%"+queryParam.toLowerCase()+"%");
        }
        for (MetadataField metadataField : metadataFieldsToJoin) {
            query.setParameter(metadataField.toString(), metadataField.getID());
        }

        return query;
    }

    @Override
    public List<Collection> search(Context context, String query, List<MetadataField> queryFields, List<MetadataField> sortFields, int offset, int limit) throws SQLException
    {
        String queryString = "SELECT " + Collection.class.getSimpleName().toLowerCase() + " FROM Collection as " + Collection.class.getSimpleName().toLowerCase() + " ";
        if(query != null) query= "%"+query.toLowerCase()+"%";
        Query hibernateQuery = getSearchQuery(context, queryString, query, queryFields, sortFields, null);

        if(0 <= offset)
        {
            hibernateQuery.setFirstResult(offset);
        }
        if(0 <= limit)
        {
            hibernateQuery.setMaxResults(limit);
        }
        return list(hibernateQuery);
    }

    @Override
    public int searchResultCount(Context context, String query, List<MetadataField> queryFields) throws SQLException
    {
        String queryString = "SELECT count(*) FROM Collection as " + Collection.class.getSimpleName().toLowerCase();
        Query hibernateQuery = getSearchQuery(context, queryString, query, queryFields, ListUtils.EMPTY_LIST, null);

        return count(hibernateQuery);
    }
    // End UMD Customization
}