package edu.umd.lib.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;

import edu.umd.lib.dspace.content.EmbargoDTO;
import edu.umd.lib.dspace.content.dao.EmbargoDTODAO;

public class EmbargoDTODAOImpl extends AbstractHibernateDAO<Object> implements EmbargoDTODAO {

    private static final String sql = "SELECT DISTINCT ON (h.handle) h.handle as handle, "
          + "i1.id as itemId, bs.id as bitstreamId, "
          + "(SELECT dc.text_value FROM metadatavalue dc "
          + "WHERE dc.metadata_field_id=:titleId AND dc.resource_id=i1.id AND dc.resource_type_id=:resourceType LIMIT 1) as title, "
          + "(SELECT dc.text_value FROM metadatavalue dc "
          + "WHERE dc.metadata_field_id=:AdvisorId AND dc.resource_id=i1.id LIMIT 1) as advisor, "
          + "(SELECT dc.text_value FROM metadatavalue dc "
          + "WHERE dc.metadata_field_id=:authorId AND dc.resource_id=i1.id LIMIT 1) as author, "
          + "(SELECT dc.text_value FROM metadatavalue dc "
          + "WHERE dc.metadata_field_id=:departmentId AND dc.resource_id=i1.id LIMIT 1) as department, "
          + "(SELECT dc.text_value FROM metadatavalue dc "
          + "WHERE dc.metadata_field_id=typeId AND dc.resource_id=i1.id LIMIT 1) as type, "
          + "rp.end_date as endDate"
          + "FROM  handle h, item i1, item2bundle i2b1, bundle2bitstream b2b1, bitstream bs, "
          + "resourcepolicy rp, epersongroup g, metadatavalue mv "
          + "WHERE h.resource_id=i1.id AND i1.id=i2b1.item_id AND i2b1.bundle_id=b2b1.bundle_id AND "
          + "b2b1.bitstream_id=bs.id AND bs.id=rp.resource_id AND (rp.end_date > CURRENT_DATE "
          + "OR rp.end_date IS NULL) AND rp.epersongroup_id = g.eperson_group_id AND "
          + "g.eperson_group_id = mv.resource_id AND mv.text_value = :groupName";

    private static Query sqlQuery = null;  
    
    protected EmbargoDTODAOImpl() {
      super();
    }

    @Override
    public List<EmbargoDTO> getEmbargoDTOList(Context context, int titleId, int advisorId, int authorId,
        int departmentId, int typeId, int resourceType, String groupName) throws SQLException {
        
        if (sqlQuery == null) {
            this.sqlQuery = createSQLQuery(context, sql)
                  .setParameter("titleId", titleId)
                  .setParameter("advisorId", advisorId)
                  .setParameter("authorId", authorId)
                  .setParameter("departmentId", departmentId)
                  .setParameter("typeId", typeId)
                  .setParameter("resourceType", resourceType)
                  .setParameter("groupName", groupName)
                  .setResultTransformer(Transformers.aliasToBean(EmbargoDTO.class))
                  .setCacheable(true);
        }
        return sqlQuery.list();
    }

    private SQLQuery createSQLQuery(Context context, String query) throws SQLException {
        return getHibernateSession(context).createSQLQuery(query);
    }
}