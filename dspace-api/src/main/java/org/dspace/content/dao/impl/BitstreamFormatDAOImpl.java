/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.content.BitstreamFormat;
import org.dspace.content.BitstreamFormat_;
import org.dspace.content.dao.BitstreamFormatDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.hibernate.Session;

/**
 * Hibernate implementation of the Database Access Object interface class for the BitstreamFormat object.
 * This class is responsible for all database calls for the BitstreamFormat object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BitstreamFormatDAOImpl extends AbstractHibernateDAO<BitstreamFormat> implements BitstreamFormatDAO {

    protected BitstreamFormatDAOImpl() {
        super();
    }

    /**
     * Find a bitstream format by its (unique) MIME type.
     * If more than one bitstream format has the same MIME type, the
     * one returned is unpredictable.
     *
     * @param session
     *            The current request's database context.
     * @param mimeType
     *            MIME type value
     * @param includeInternal whether to include internal mimetypes
     *
     * @return the corresponding bitstream format, or <code>null</code> if
     *         there's no bitstream format with the given MIMEtype.
     * @throws SQLException if database error
     */
    @Override
    public BitstreamFormat findByMIMEType(Session session, String mimeType, boolean includeInternal)
        throws SQLException {
        // NOTE: Avoid internal formats since e.g. "License" also has
        // a MIMEtype of text/plain.
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, BitstreamFormat.class);
        Root<BitstreamFormat> bitstreamFormatRoot = criteriaQuery.from(BitstreamFormat.class);
        criteriaQuery.select(bitstreamFormatRoot);
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(bitstreamFormatRoot.get(BitstreamFormat_.internal), includeInternal),
            criteriaBuilder.like(bitstreamFormatRoot.get(BitstreamFormat_.mimetype), mimeType)
                            )
        );
        return singleResult(session, criteriaQuery);
    }

    /**
     * Find a bitstream format by its (unique) short description
     *
     * @param session
     *            The current request's database context.
     * @param desc
     *            the short description
     *
     * @return the corresponding bitstream format, or <code>null</code> if
     *         there's no bitstream format with the given short description
     * @throws SQLException if database error
     */
    @Override
    public BitstreamFormat findByShortDescription(Session session,
                                                  String desc) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, BitstreamFormat.class);
        Root<BitstreamFormat> bitstreamFormatRoot = criteriaQuery.from(BitstreamFormat.class);
        criteriaQuery.select(bitstreamFormatRoot);
        criteriaQuery.where(criteriaBuilder.equal(bitstreamFormatRoot.get(BitstreamFormat_.shortDescription), desc));
        return uniqueResult(session, criteriaQuery, false, BitstreamFormat.class);
    }

    @Override
    public int updateRemovedBitstreamFormat(Session session, BitstreamFormat deletedBitstreamFormat,
                                            BitstreamFormat newBitstreamFormat) throws SQLException {
        // Set bitstreams with this format to "unknown"
        Query query = createQuery(session,
                                  "update Bitstream set bitstreamFormat = :unknown_format where bitstreamFormat = " +
                                      ":deleted_format");
        query.setParameter("unknown_format", newBitstreamFormat);
        query.setParameter("deleted_format", deletedBitstreamFormat);

        return query.executeUpdate();
    }

    @Override
    public List<BitstreamFormat> findNonInternal(Session session) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, BitstreamFormat.class);
        Root<BitstreamFormat> bitstreamFormatRoot = criteriaQuery.from(BitstreamFormat.class);
        criteriaQuery.select(bitstreamFormatRoot);
        criteriaQuery
            .where(criteriaBuilder.and(criteriaBuilder.equal(bitstreamFormatRoot.get(BitstreamFormat_.internal), false),
                                       criteriaBuilder.not(
                                           criteriaBuilder
                                               .like(bitstreamFormatRoot.get(BitstreamFormat_.shortDescription),
                                                     "Unknown"))
                   )
        );


        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.desc(bitstreamFormatRoot.get(BitstreamFormat_.supportLevel)));
        orderList.add(criteriaBuilder.asc(bitstreamFormatRoot.get(BitstreamFormat_.shortDescription)));
        criteriaQuery.orderBy(orderList);


        return list(session, criteriaQuery, false, BitstreamFormat.class, -1, -1);

    }

    @Override
    public List<BitstreamFormat> findByFileExtension(Session session, String extension) throws SQLException {

        Query query = createQuery(session, "from BitstreamFormat bf where :extension in elements(bf.fileExtensions)");
        query.setParameter("extension", extension);

        return list(query);
    }

    @Override
    public List<BitstreamFormat> findAll(Session session, Class clazz) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, BitstreamFormat.class);
        Root<BitstreamFormat> bitstreamFormatRoot = criteriaQuery.from(BitstreamFormat.class);
        criteriaQuery.select(bitstreamFormatRoot);

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(bitstreamFormatRoot.get(BitstreamFormat_.id)));
        criteriaQuery.orderBy(orderList);

        return list(session, criteriaQuery, false, BitstreamFormat.class, -1, -1);
    }

}
