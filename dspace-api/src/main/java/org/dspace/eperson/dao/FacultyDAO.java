package org.dspace.eperson.dao;

import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.core.Context;
import org.dspace.eperson.FacultyEntity;

import java.sql.SQLException;
import java.util.List;

public interface FacultyDAO extends DSpaceObjectDAO<FacultyEntity> {
    List<FacultyEntity> findAll(Context context) throws SQLException;
}