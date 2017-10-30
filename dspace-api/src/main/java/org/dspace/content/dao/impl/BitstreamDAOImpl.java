/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Hibernate implementation of the Database Access Object interface class for the Bitstream object.
 * This class is responsible for all database calls for the Bitstream object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BitstreamDAOImpl extends AbstractHibernateDSODAO<Bitstream> implements BitstreamDAO
{
    private static final Logger log = Logger.getLogger(BitstreamDAO.class);


    protected BitstreamDAOImpl()
    {
        super();
    }

    @Override
    public List<Bitstream> findDeletedBitstreams(Context context) throws SQLException {
        Criteria criteria = createCriteria(context, Bitstream.class);
        criteria.add(Restrictions.eq("deleted", true));

        return list(criteria);

    }

    @Override
    public List<Bitstream> findDuplicateInternalIdentifier(Context context, Bitstream bitstream) throws SQLException {
        Criteria criteria = createCriteria(context, Bitstream.class);
        criteria.add(Restrictions.and(
                Restrictions.eq("internalId", bitstream.getInternalId()),
                Restrictions.not(Restrictions.eq("id", bitstream.getID()))
        ));

        return list(criteria);
    }

    @Override
    public List<Bitstream> findBitstreamsWithNoRecentChecksum(Context context) throws SQLException {
//        "select bitstream.deleted, bitstream.store_number, bitstream.size_bytes, "
//                    + "bitstreamformatregistry.short_description, bitstream.bitstream_id,  "
//                    + "bitstream.user_format_description, bitstream.internal_id, "
//                    + "bitstream.source, bitstream.checksum_algorithm, bitstream.checksum, "
//                    + "bitstream.name, bitstream.description "
//                    + "from bitstream left outer join bitstreamformatregistry on "
//                    + "bitstream.bitstream_format_id = bitstreamformatregistry.bitstream_format_id "
//                    + "where not exists( select 'x' from most_recent_checksum "
//                    + "where most_recent_checksum.bitstream_id = bitstream.bitstream_id )"

        Query query = createQuery(context, "select b from Bitstream b where b not in (select c.bitstream from MostRecentChecksum c)");
        return query.list();
    }

    @Override
    public Iterator<Bitstream> findByCommunity(Context context, Community community) throws SQLException {
        Query query = createQuery(context, "select b from Bitstream b " +
                "join b.bundles bitBundles " +
                "join bitBundles.items item " +
                "join item.collections itemColl " +
                "join itemColl.communities community " +
                "WHERE :community IN community");

        query.setParameter("community", community);

        return iterate(query);
    }

    @Override
    public Iterator<Bitstream> findByCollection(Context context, Collection collection) throws SQLException {
        Query query = createQuery(context, "select b from Bitstream b " +
                "join b.bundles bitBundles " +
                "join bitBundles.items item " +
                "join item.collections c " +
                "WHERE :collection IN c");

        query.setParameter("collection", collection);

        return iterate(query);
    }

    @Override
    public Iterator<Bitstream> findByItem(Context context, Item item) throws SQLException {
        Query query = createQuery(context, "select b from Bitstream b " +
                "join b.bundles bitBundles " +
                "join bitBundles.items item " +
                "WHERE :item IN item");

        query.setParameter("item", item);

        return iterate(query);
    }

    @Override
    public Iterator<Bitstream> findByStoreNumber(Context context, Integer storeNumber) throws SQLException {
        Query query = createQuery(context, "select b from Bitstream b where b.storeNumber = :storeNumber");
        query.setParameter("storeNumber", storeNumber);
        return iterate(query);
    }

    @Override
    public Long countByStoreNumber(Context context, Integer storeNumber) throws SQLException {
        Criteria criteria = createCriteria(context, Bitstream.class);
        criteria.add(Restrictions.eq("storeNumber", storeNumber));
        return countLong(criteria);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) from Bitstream"));
    }

    @Override
    public int countDeleted(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Bitstream b WHERE b.deleted=true"));
    }

    @Override
    public int countWithNoPolicy(Context context) throws SQLException {
        Query query = createQuery(context,"SELECT count(bit.id) from Bitstream bit where bit.deleted<>true and bit.id not in" +
                " (select res.dSpaceObject from ResourcePolicy res where res.resourceTypeId = :typeId )" );
        query.setParameter("typeId", Constants.BITSTREAM);
        return count(query);
    }

    @Override
    public List<Bitstream> getNotReferencedBitstreams(Context context) throws SQLException {
        return list(createQuery(context,"select bit from Bitstream bit where bit.deleted != true" +
                " and bit.id not in (select bit2.id from Bundle bun join bun.bitstreams bit2)" +
                " and bit.id not in (select com.logo.id from Community com)" +
                " and bit.id not in (select col.logo.id from Collection col)" +
                " and bit.id not in (select bun.primaryBitstream.id from Bundle bun)"));
    }

    @Override
    public Iterator<Bitstream> findAll(Context context, int limit, int offset) throws SQLException {
        Query query = createQuery(context, "select b FROM Bitstream b");
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return iterate(query);
    }

    @Override
    public Iterator<Bitstream> findAllAuthorized(Context context, int pageSize, int pageOffset, EPerson currentUser, int action, Set<Group> groups) throws SQLException{
        Query query = createQuery(context, "SELECT DISTINCT b" +
                " FROM Bitstream b" +
                " JOIN b.resourcePolicies r" +
                " WHERE (r.epersonGroup.id IN (:groupIdList) OR r.eperson.id = :currentUserId)" +
                " AND (r.actionId = :actionId)" +
                " AND (r.startDate IS NULL or r.startDate <= :currentDate)" +
                " AND (r.endDate IS NULL or r.endDate >= :currentDate)");
        LinkedList<UUID> list = new LinkedList<>();
        for(Group group: groups){
            list.add(group.getID());
        }
        query.setParameter("groupIdList", list);
        if(currentUser == null){
            query.setParameter("currentUserId", null);
        }
        else{
            query.setParameter("currentUserId", currentUser.getID());
        }
        query.setParameter("actionId", action);
        Date currentDate = new Date();
        query.setParameter("currentDate", currentDate);
        query.setFirstResult(pageOffset);
        query.setMaxResults(pageSize);
        return iterate(query);
    }
}
