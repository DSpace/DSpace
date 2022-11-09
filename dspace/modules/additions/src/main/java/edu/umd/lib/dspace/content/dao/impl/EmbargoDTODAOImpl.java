package edu.umd.lib.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.hibernate.query.Query;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.hibernate.type.DateType;
import org.hibernate.type.PostgresUUIDType;
import org.hibernate.type.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.lib.dspace.content.EmbargoDTO;
import edu.umd.lib.dspace.content.dao.EmbargoDTODAO;

public class EmbargoDTODAOImpl extends AbstractHibernateDAO<Object> implements EmbargoDTODAO {

    private static Logger log = LoggerFactory.getLogger(EmbargoDTODAOImpl.class);

    private static final String sql = "SELECT DISTINCT ON (h.handle) h.handle as handle, "
          + "i1.uuid as itemId, bs.uuid as bitstreamId, "
          + "(SELECT dc.text_value FROM metadatavalue dc "
          + "WHERE dc.metadata_field_id=:titleId AND dc.dspace_object_id=i1.uuid LIMIT 1) as title, "
          + "(SELECT dc.text_value FROM metadatavalue dc "
          + "WHERE dc.metadata_field_id=:advisorId AND dc.dspace_object_id=i1.uuid LIMIT 1) as advisor, "
          + "(SELECT dc.text_value FROM metadatavalue dc "
          + "WHERE dc.metadata_field_id=:authorId AND dc.dspace_object_id=i1.uuid LIMIT 1) as author, "
          + "(SELECT dc.text_value FROM metadatavalue dc "
          + "WHERE dc.metadata_field_id=:departmentId AND dc.dspace_object_id=i1.uuid LIMIT 1) as department, "
          + "(SELECT dc.text_value FROM metadatavalue dc "
          + "WHERE dc.metadata_field_id=:typeId AND dc.dspace_object_id=i1.uuid LIMIT 1) as type, "
          + "rp.end_date as endDate "
          + "FROM handle h, item i1, item2bundle i2b1, bundle2bitstream b2b1, bitstream bs, "
          + "resourcepolicy rp, epersongroup g, metadatavalue mv "
          + "WHERE h.resource_id=i1.uuid AND i1.uuid=i2b1.item_id AND i2b1.bundle_id=b2b1.bundle_id AND "
          + "b2b1.bitstream_id=bs.uuid AND bs.uuid=rp.dspace_object AND (rp.end_date > CURRENT_DATE "
          + "OR rp.end_date IS NULL) AND rp.epersongroup_id = g.uuid AND "
          + "g.uuid = mv.dspace_object_id AND mv.text_value = :groupName";

    protected EmbargoDTODAOImpl() {
      super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EmbargoDTO> getEmbargoDTOList(Context context, int titleId, int advisorId, int authorId,
        int departmentId, int typeId, String groupName) throws SQLException {

        log.debug("Getting Embargo List with params titleId: {}, advisorId: {}, authorId: {}, " +
                "departmentId: {}, typeId: {}, groupName: {}", titleId, advisorId,
                authorId, departmentId, typeId, groupName);

        Query<EmbargoDTO> sqlQuery = (Query<EmbargoDTO>) createSQLQuery(context, sql)
                .addScalar("handle", StringType.INSTANCE)
                .addScalar("itemId", PostgresUUIDType.INSTANCE)
                .addScalar("bitstreamId", PostgresUUIDType.INSTANCE)
                .addScalar("title", StringType.INSTANCE)
                .addScalar("advisor", StringType.INSTANCE)
                .addScalar("author", StringType.INSTANCE)
                .addScalar("department", StringType.INSTANCE)
                .addScalar("type", StringType.INSTANCE)
                .addScalar("endDate", DateType.INSTANCE)
                .setParameter("titleId", titleId)
                .setParameter("advisorId", advisorId)
                .setParameter("authorId", authorId)
                .setParameter("departmentId", departmentId)
                .setParameter("typeId", typeId)
                .setParameter("groupName", groupName)
                .setResultTransformer(Transformers.aliasToBean(EmbargoDTO.class));

        return (List<EmbargoDTO>) sqlQuery.list();
    }

    private NativeQuery createSQLQuery(Context context, String query) throws SQLException {
        return getHibernateSession(context).createSQLQuery(query);
    }
}