package org.dspace.eperson.dao.impl;

import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.eperson.FacultyEntity;
import org.dspace.eperson.dao.FacultyDAO;
import org.hibernate.Query;

import java.sql.SQLException;
import java.util.List;

public class FacultyDAOImpl extends AbstractHibernateDSODAO<FacultyEntity> implements FacultyDAO {
    protected FacultyDAOImpl()
    {
        super();
    }
    @Override
    public List<FacultyEntity> findAll(Context context) throws SQLException {
        Query query = createQuery(context,
                "SELECT f FROM FacultyEntity f ORDER BY f.name ASC");
        query.setCacheable(true);

        return list(query);
    }
}
