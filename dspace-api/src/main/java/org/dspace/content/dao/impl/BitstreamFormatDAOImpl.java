/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.BitstreamFormat;
import org.dspace.content.dao.BitstreamFormatDAO;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the BitstreamFormat object.
 * This class is responsible for all database calls for the BitstreamFormat object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BitstreamFormatDAOImpl extends AbstractHibernateDAO<BitstreamFormat> implements BitstreamFormatDAO
{

    protected BitstreamFormatDAOImpl()
    {
        super();
    }

    /**
     * Find a bitstream format by its (unique) MIME type.
     * If more than one bitstream format has the same MIME type, the
     * one returned is unpredictable.
     *
     * @param context
     *            DSpace context object
     * @param mimeType
     *            MIME type value
     * @param includeInternal whether to include internal mimetypes
     *
     * @return the corresponding bitstream format, or <code>null</code> if
     *         there's no bitstream format with the given MIMEtype.
     * @throws SQLException if database error
     */
    @Override
    public BitstreamFormat findByMIMEType(Context context, String mimeType, boolean includeInternal) throws SQLException
    {
        // NOTE: Avoid internal formats since e.g. "License" also has
        // a MIMEtype of text/plain.
        Criteria criteria = createCriteria(context, BitstreamFormat.class);
        criteria.add(Restrictions.and(
                Restrictions.eq("internal", includeInternal),
                Restrictions.like("mimetype", mimeType)
        ));

        return singleResult(criteria);
    }

    /**
     * Find a bitstream format by its (unique) short description
     *
     * @param context
     *            DSpace context object
     * @param desc
     *            the short description
     *
     * @return the corresponding bitstream format, or <code>null</code> if
     *         there's no bitstream format with the given short description
     * @throws SQLException if database error
     */
    @Override
    public BitstreamFormat findByShortDescription(Context context,
            String desc) throws SQLException
    {
        Criteria criteria = createCriteria(context, BitstreamFormat.class);
        criteria.add(Restrictions.and(
                Restrictions.eq("shortDescription", desc)
        ));

        return uniqueResult(criteria);
    }

    @Override
    public int updateRemovedBitstreamFormat(Context context, BitstreamFormat deletedBitstreamFormat, BitstreamFormat newBitstreamFormat) throws SQLException {
        // Set bitstreams with this format to "unknown"
        Query query = createQuery(context, "update Bitstream set bitstreamFormat = :unknown_format where bitstreamFormat = :deleted_format");
        query.setParameter("unknown_format", newBitstreamFormat);
        query.setParameter("deleted_format", deletedBitstreamFormat);

        return query.executeUpdate();
    }

    @Override
    public List<BitstreamFormat> findNonInternal(Context context) throws SQLException {
        Criteria criteria = createCriteria(context, BitstreamFormat.class);
        criteria.add(Restrictions.and(
                Restrictions.eq("internal", false),
                Restrictions.not(Restrictions.like("shortDescription", "Unknown"))
        ));
        criteria.addOrder(Order.desc("supportLevel")).addOrder(Order.asc("shortDescription"));

        return list(criteria);

    }

    @Override
    public List<BitstreamFormat> findByFileExtension(Context context, String extension) throws SQLException {

        Query query = createQuery(context, "from BitstreamFormat bf where :extension in elements(bf.fileExtensions)");
        query.setParameter("extension", extension);

//        Criteria criteria = createCriteria(context, BitstreamFormat.class, "bitstreamFormat");
//        criteria.createAlias("bitstreamFormat.fileExtensions", "extension");
//        criteria.add(Restrictions.eq("extension",extension ));
        return list(query);
    }

    @Override
    public List<BitstreamFormat> findAll(Context context, Class clazz) throws SQLException {
        Criteria criteria = createCriteria(context, BitstreamFormat.class);
        criteria.addOrder(Order.asc("id"));
        return list(criteria);
    }

}
