package org.dspace.eperson.service;

import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.FacultyEntity;

import java.sql.SQLException;
import java.util.List;

public interface FacultyService extends DSpaceObjectService<FacultyEntity> {
    List<FacultyEntity> findAll(Context context) throws SQLException;
}
