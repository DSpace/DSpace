package org.dspace.eperson;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Context;
import org.dspace.eperson.dao.FacultyDAO;
import org.dspace.eperson.service.FacultyService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class FacultyServiceImpl extends DSpaceObjectServiceImpl<FacultyEntity> implements FacultyService {
    @Autowired(required = true)
    private FacultyDAO facultyDAO;

    @Override
    public FacultyEntity find(Context context, UUID id) throws SQLException {
        return null;
    }

    @Override
    public void updateLastModified(Context context, FacultyEntity dso) throws SQLException, AuthorizeException {

    }

    @Override
    public void delete(Context context, FacultyEntity dso) throws SQLException, AuthorizeException, IOException {

    }

    @Override
    public int getSupportsTypeConstant() {
        return 0;
    }

    @Override
    public List<FacultyEntity> findAll(Context context) throws SQLException {
        return facultyDAO.findAll(context);
    }
}
