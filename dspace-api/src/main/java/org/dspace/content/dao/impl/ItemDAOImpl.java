/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.ItemDAO;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.eperson.EPerson;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.type.StandardBasicTypes;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Hibernate implementation of the Database Access Object interface class for the Item object.
 * This class is responsible for all database calls for the Item object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class ItemDAOImpl extends AbstractHibernateDSODAO<Item> implements ItemDAO
{
    private static final Logger log = Logger.getLogger(ItemDAOImpl.class);

    protected ItemDAOImpl()
    {
        super();
    }

    @Override
    public Iterator<Item> findAll(Context context, boolean archived) throws SQLException {
        Query query = createQuery(context, "FROM Item WHERE inArchive= :in_archive order by id");
        query.setParameter("in_archive", archived);
        return iterate(query);
    }

    @Override
    public Iterator<Item> findAll(Context context, boolean archived, boolean withdrawn) throws SQLException {
        Query query = createQuery(context, "FROM Item WHERE inArchive= :in_archive or withdrawn = :withdrawn order by id");
        query.setParameter("in_archive", archived);
        query.setParameter("withdrawn", withdrawn);
        return iterate(query);
    }

    @Override
    public Iterator<Item> findAll(Context context, boolean archived,
            boolean withdrawn, boolean discoverable, Date lastModified)
            throws SQLException
    {
        StringBuilder queryStr = new StringBuilder();
        queryStr.append("SELECT i FROM Item i");
        queryStr.append(" WHERE (inArchive = :in_archive OR withdrawn = :withdrawn)");
        queryStr.append(" AND discoverable = :discoverable");

        if(lastModified != null)
        {
            queryStr.append(" AND last_modified > :last_modified");
        }

        queryStr.append(" order by id");

        Query query = createQuery(context, queryStr.toString());
        query.setParameter("in_archive", archived);
        query.setParameter("withdrawn", withdrawn);
        query.setParameter("discoverable", discoverable);
        if(lastModified != null)
        {
            query.setTimestamp("last_modified", lastModified);
	}
        return iterate(query);
    }

    @Override
    public Iterator<Item> findBySubmitter(Context context, EPerson eperson) throws SQLException {
        Query query = createQuery(context, "FROM Item WHERE inArchive= :in_archive and submitter= :submitter order by id");
        query.setParameter("in_archive", true);
        query.setParameter("submitter", eperson);
        return iterate(query);
    }

    @Override
    public Iterator<Item> findBySubmitter(Context context, EPerson eperson, MetadataField metadataField, int limit) throws SQLException {
        StringBuilder query = new StringBuilder();
        query.append("SELECT item FROM Item as item ");
        addMetadataLeftJoin(query, Item.class.getSimpleName().toLowerCase(), Collections.singletonList(metadataField));
        query.append(" WHERE item.inArchive = :in_archive");
        query.append(" AND item.submitter =:submitter");
        //submissions should sort in reverse by date by default
        addMetadataSortQuery(query, Collections.singletonList(metadataField), null, Collections.singletonList("desc"));

        Query hibernateQuery = createQuery(context, query.toString());
        hibernateQuery.setParameter(metadataField.toString(), metadataField.getID());
        hibernateQuery.setParameter("in_archive", true);
        hibernateQuery.setParameter("submitter", eperson);
        hibernateQuery.setMaxResults(limit);
        return iterate(hibernateQuery);
    }

    @Override
    public Iterator<Item> findByMetadataField(Context context, MetadataField metadataField, String value, boolean inArchive) throws SQLException {
        String hqlQueryString = "SELECT item FROM Item as item join item.metadata metadatavalue WHERE item.inArchive=:in_archive AND metadatavalue.metadataField = :metadata_field";
        if(value != null)
        {
            hqlQueryString += " AND STR(metadatavalue.value) = :text_value";
        }
        Query query = createQuery(context, hqlQueryString + " order by item.id");

        query.setParameter("in_archive", inArchive);
        query.setParameter("metadata_field", metadataField);
        if(value != null)
        {
            query.setParameter("text_value", value);
        }
        return iterate(query);
    }

    enum OP {equals,not_equals,like,not_like,contains,doesnt_contain,exists,doesnt_exist,matches,doesnt_match;}
    
    @Override
    public Iterator<Item> findByMetadataQuery(Context context, List<List<MetadataField>> listFieldList, List<String> query_op, List<String> query_val, List<UUID> collectionUuids, String regexClause, int offset, int limit) throws SQLException {
    	Criteria criteria = createCriteria(context, Item.class, "item");
    	criteria.setFirstResult(offset);
    	criteria.setMaxResults(limit);
    	
    	if (!collectionUuids.isEmpty()){
			DetachedCriteria dcollCriteria = DetachedCriteria.forClass(Collection.class, "coll");
        	dcollCriteria.setProjection(Projections.property("coll.id"));
        	dcollCriteria.add(Restrictions.eqProperty("coll.id", "item.owningCollection"));
			dcollCriteria.add(Restrictions.in("coll.id", collectionUuids));
			criteria.add(Subqueries.exists(dcollCriteria));
    	}
    	
        int index = Math.min(listFieldList.size(), Math.min(query_op.size(), query_val.size()));
        StringBuilder sb = new StringBuilder();

        for(int i=0; i<index; i++) {
        	OP op = OP.valueOf(query_op.get(i));
        	if (op == null) {
        		log.warn("Skipping Invalid Operator: " + query_op.get(i));
        		continue;
        	}
        	
        	if (op == OP.matches || op == OP.doesnt_match) {
        		if (regexClause.isEmpty()) {
            		log.warn("Skipping Unsupported Regex Operator: " + query_op.get(i));
            		continue;
        		}
        	}
        	
        	DetachedCriteria subcriteria = DetachedCriteria.forClass(MetadataValue.class,"mv");
        	subcriteria.add(Property.forName("mv.dSpaceObject").eqProperty("item.id"));
        	subcriteria.setProjection(Projections.property("mv.dSpaceObject"));
        	
        	if (!listFieldList.get(i).isEmpty()) {
        		subcriteria.add(Restrictions.in("metadataField", listFieldList.get(i)));
        	}
        	
        	sb.append(op.name() + " ");
        	if (op == OP.equals || op == OP.not_equals){
    			subcriteria.add(Property.forName("mv.value").eq(query_val.get(i)));
    			sb.append(query_val.get(i));
        	} else if (op == OP.like || op == OP.not_like){
    			subcriteria.add(Property.forName("mv.value").like(query_val.get(i)));        		        		
    			sb.append(query_val.get(i));
        	} else if (op == OP.contains || op == OP.doesnt_contain){
    			subcriteria.add(Property.forName("mv.value").like("%"+query_val.get(i)+"%"));        		        		
    			sb.append(query_val.get(i));
        	} else if (op == OP.matches || op == OP.doesnt_match) {
            	subcriteria.add(Restrictions.sqlRestriction(regexClause, query_val.get(i), StandardBasicTypes.STRING));
    			sb.append(query_val.get(i));        		
        	}
        	
        	if (op == OP.exists || op == OP.equals || op == OP.like || op == OP.contains || op == OP.matches) {
        		criteria.add(Subqueries.exists(subcriteria));
        	} else {
        		criteria.add(Subqueries.notExists(subcriteria));        		
        	}
        }
        criteria.addOrder(Order.asc("item.id"));

     	log.debug(String.format("Running custom query with %d filters", index));

        return list(criteria).iterator();
    }

    @Override
    public Iterator<Item> findByAuthorityValue(Context context, MetadataField metadataField, String authority, boolean inArchive) throws SQLException {
        Query query = createQuery(context, "SELECT item FROM Item as item join item.metadata metadatavalue WHERE item.inArchive=:in_archive AND metadatavalue.metadataField = :metadata_field AND metadatavalue.authority = :authority order by item.id");
        query.setParameter("in_archive", inArchive);
        query.setParameter("metadata_field", metadataField);
        query.setParameter("authority", authority);
        return iterate(query);
    }

    @Override
    public Iterator<Item> findArchivedByCollection(Context context, Collection collection, Integer limit, Integer offset) throws SQLException {
        Query query = createQuery(context, "select i from Item i join i.collections c WHERE :collection IN c AND i.inArchive=:in_archive order by i.id");
        query.setParameter("collection", collection);
        query.setParameter("in_archive", true);
        if(offset != null)
        {
            query.setFirstResult(offset);
        }
        if(limit != null)
        {
            query.setMaxResults(limit);
        }
        return iterate(query);
    }

    @Override
    public Iterator<Item> findAllByCollection(Context context, Collection collection) throws SQLException {
        Query query = createQuery(context, "select i from Item i join i.collections c WHERE :collection IN c order by i.id");
        query.setParameter("collection", collection);

        return iterate(query);
    }

    @Override
    public Iterator<Item> findAllByCollection(Context context, Collection collection, Integer limit, Integer offset) throws SQLException {
        Query query = createQuery(context, "select i from Item i join i.collections c WHERE :collection IN c order by i.id");
        query.setParameter("collection", collection);

        if(offset != null)
        {
            query.setFirstResult(offset);
        }
        if(limit != null)
        {
            query.setMaxResults(limit);
        }
 
        return iterate(query);
    }

    
    @Override
    public int countItems(Context context, Collection collection, boolean includeArchived, boolean includeWithdrawn) throws SQLException {
        Query query = createQuery(context, "select count(i) from Item i join i.collections c WHERE :collection IN c AND i.inArchive=:in_archive AND i.withdrawn=:withdrawn");
        query.setParameter("collection", collection);
        query.setParameter("in_archive", includeArchived);
        query.setParameter("withdrawn", includeWithdrawn);

        return count(query);
    }
    
    @Override
    public int countItems(Context context, List<Collection> collections, boolean includeArchived, boolean includeWithdrawn) throws SQLException {
        if (collections.size() == 0) {
            return 0;
        }
        Query query = createQuery(context, "select count(distinct i) from Item i " +
                                            "join i.collections collection " +
                                            "WHERE collection IN (:collections) AND i.inArchive=:in_archive AND i.withdrawn=:withdrawn");
        query.setParameterList("collections", collections);
        query.setParameter("in_archive", includeArchived);
        query.setParameter("withdrawn", includeWithdrawn);

        return count(query);
    }

    @Override
    public Iterator<Item> findByLastModifiedSince(Context context, Date since)
            throws SQLException
    {
        Query query = createQuery(context, "SELECT i FROM item i WHERE last_modified > :last_modified order by id");
        query.setTimestamp("last_modified", since);
        return iterate(query);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Item"));
    }

    @Override
    public int countItems(Context context, boolean includeArchived, boolean includeWithdrawn) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) FROM Item i WHERE i.inArchive=:in_archive AND i.withdrawn=:withdrawn");
        query.setParameter("in_archive", includeArchived);
        query.setParameter("withdrawn", includeWithdrawn);
        return count(query); 
    }
}
