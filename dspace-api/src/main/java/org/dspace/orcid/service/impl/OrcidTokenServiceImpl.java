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
            return orcidTokenDAO.create(context, orcidToken);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OrcidToken findByEPerson(Context context, EPerson ePerson) {
        return orcidTokenDAO.findByEPerson(context, ePerson);
    }

    @Override
    public OrcidToken findByProfileItem(Context context, Item profileItem) {
        return orcidTokenDAO.findByProfileItem(context, profileItem);
    }

    @Override
    public void delete(Context context, OrcidToken orcidToken) {
        try {
            orcidTokenDAO.delete(context, orcidToken);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteAll(Context context) {
        try {

            List<OrcidToken> tokens = orcidTokenDAO.findAll(context, OrcidToken.class);
            for (OrcidToken token : tokens) {
                delete(context, token);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteByEPerson(Context context, EPerson ePerson) {
        OrcidToken orcidToken = findByEPerson(context, ePerson);
        if (orcidToken != null) {
            delete(context, orcidToken);
        }
    }

    @Override
    public void deleteByProfileItem(Context context, Item profileItem) {
        OrcidToken orcidToken = findByProfileItem(context, profileItem);
        if (orcidToken != null) {
            delete(context, orcidToken);
        }
    }

}
