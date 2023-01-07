/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.clarin;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.NullArgumentException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.clarin.ClarinVerificationTokenDAO;
import org.dspace.content.service.clarin.ClarinVerificationTokenService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the ClarinVerificationToken object.
 * This class is responsible for all business logic calls for the ClarinVerificationToken object and
 * is autowired by spring. This class should never be accessed directly.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinVerificationTokenServiceImpl implements ClarinVerificationTokenService {

    private static final Logger log = LoggerFactory.getLogger(ClarinVerificationTokenServiceImpl.class);

    @Autowired
    ClarinVerificationTokenDAO clarinVerificationTokenDAO;
    @Autowired
    AuthorizeService authorizeService;

    @Override
    public ClarinVerificationToken create(Context context) throws SQLException {
        ClarinVerificationToken clarinVerificationToken = clarinVerificationTokenDAO.create(context,
                new ClarinVerificationToken());

        log.info(LogHelper.getHeader(context, "create_clarin_verification_token",
                "clarin_verification_token_id=" + clarinVerificationToken.getID()));

        return clarinVerificationToken;
    }

    @Override
    public ClarinVerificationToken find(Context context, int valueId) throws SQLException {
        return clarinVerificationTokenDAO.findByID(context, ClarinVerificationToken.class, valueId);
    }

    @Override
    public List<ClarinVerificationToken> findAll(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to load all clarin verification tokens.");
        }

        return clarinVerificationTokenDAO.findAll(context, ClarinVerificationToken.class);
    }

    @Override
    public ClarinVerificationToken findByToken(Context context, String token) throws SQLException {
        return clarinVerificationTokenDAO.findByToken(context, token);
    }

    @Override
    public ClarinVerificationToken findByNetID(Context context, String netID) throws SQLException {
        return clarinVerificationTokenDAO.findByNetID(context, netID);
    }

    @Override
    public void delete(Context context, ClarinVerificationToken clarinVerificationToken)
            throws SQLException {
        clarinVerificationTokenDAO.delete(context, clarinVerificationToken);
    }

    @Override
    public void update(Context context, ClarinVerificationToken newClarinVerificationToken) throws SQLException {
        if (Objects.isNull(newClarinVerificationToken)) {
            throw new NullArgumentException("Cannot update clarin verification token because " +
                    "the new verification token is null");
        }

        ClarinVerificationToken foundClarinVerificationToken = find(context, newClarinVerificationToken.getID());
        if (Objects.isNull(foundClarinVerificationToken)) {
            throw new ObjectNotFoundException(newClarinVerificationToken.getID(),
                    "Cannot update the clarin verification token because the clarin verification token wasn't " +
                            "found in the database.");
        }

        clarinVerificationTokenDAO.save(context, newClarinVerificationToken);
    }
}
