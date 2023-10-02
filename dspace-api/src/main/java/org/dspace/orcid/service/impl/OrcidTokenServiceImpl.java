/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service.impl;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.orcid.OrcidToken;
import org.dspace.orcid.dao.OrcidTokenDAO;
import org.dspace.orcid.service.OrcidTokenService;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidTokenService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidTokenServiceImpl implements OrcidTokenService {

    @Autowired
    private OrcidTokenDAO orcidTokenDAO;

    @Override
    public OrcidToken create(Context context, EPerson ePerson, String accessToken) {
        return create(context, ePerson, null, accessToken);
    }

    @Override
    public OrcidToken create(Context context, EPerson ePerson, Item profileItem, String accessToken) {
        OrcidToken orcidToken = new OrcidToken();
        orcidToken.setAccessToken(accessToken);
        orcidToken.setEPerson(ePerson);
        orcidToken.setProfileItem(profileItem);
        try {
            return orcidTokenDAO.create(context.getSession(), orcidToken);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OrcidToken findByEPerson(Session session, EPerson ePerson)
            throws SQLException {
        return orcidTokenDAO.findByEPerson(session, ePerson);
    }

    @Override
    public OrcidToken findByProfileItem(Session session, Item profileItem)
            throws SQLException {
        return orcidTokenDAO.findByProfileItem(session, profileItem);
    }

    @Override
    public void delete(Context context, OrcidToken orcidToken) {
        try {
            orcidTokenDAO.delete(context.getSession(), orcidToken);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteAll(Context context) {
        try {

            List<OrcidToken> tokens = orcidTokenDAO.findAll(context.getSession(), OrcidToken.class);
            for (OrcidToken token : tokens) {
                delete(context, token);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteByEPerson(Context context, EPerson ePerson)
            throws SQLException {
        OrcidToken orcidToken = findByEPerson(context.getSession(), ePerson);
        if (orcidToken != null) {
            delete(context, orcidToken);
        }
    }

    @Override
    public void deleteByProfileItem(Context context, Item profileItem)
            throws SQLException {
        OrcidToken orcidToken = findByProfileItem(context.getSession(), profileItem);
        if (orcidToken != null) {
            delete(context, orcidToken);
        }
    }
}
