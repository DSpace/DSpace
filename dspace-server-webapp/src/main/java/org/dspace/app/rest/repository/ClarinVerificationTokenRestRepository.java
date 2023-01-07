/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.model.ClarinVerificationTokenRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinVerificationToken;
import org.dspace.content.service.clarin.ClarinVerificationTokenService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage ClarinVerificationToken Rest object
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component(ClarinVerificationTokenRest.CATEGORY + "." + ClarinVerificationTokenRest.NAME)
public class ClarinVerificationTokenRestRepository extends DSpaceRestRepository<ClarinVerificationTokenRest, Integer> {
    private static final Logger log = LogManager.getLogger(ClarinVerificationTokenRestRepository.class);

    @Autowired
    ClarinVerificationTokenService clarinVerificationTokenService;

    @Override
    @PreAuthorize("permitAll()")
    public ClarinVerificationTokenRest findOne(Context context, Integer integer) {
        ClarinVerificationToken clarinVerificationToken;
        try {
            clarinVerificationToken = clarinVerificationTokenService.find(context, integer);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (Objects.isNull(clarinVerificationToken)) {
            log.error("Cannot find the clarin verification token with id: " + integer);
            return null;
        }
        return converter.toRest(clarinVerificationToken, utils.obtainProjection());
    }

    @Override
    public Page<ClarinVerificationTokenRest> findAll(Context context, Pageable pageable) {
        try {
            List<ClarinVerificationToken> clarinVerificationTokenList = clarinVerificationTokenService.findAll(context);
            return converter.toRestPage(clarinVerificationTokenList, pageable, utils.obtainProjection());
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "byNetId")
    public Page<ClarinVerificationTokenRest> findByNetId(@Parameter(value = "netid", required = true) String netid,
                                                          Pageable pageable) throws SQLException {
        Context context = obtainContext();

        ClarinVerificationToken clarinVerificationToken = clarinVerificationTokenService.findByNetID(context, netid);
        if (Objects.isNull(clarinVerificationToken)) {
            return null;
        }

        List<ClarinVerificationToken> clarinVerificationTokenList = new ArrayList<>();
        clarinVerificationTokenList.add(clarinVerificationToken);

        return converter.toRestPage(clarinVerificationTokenList, pageable, utils.obtainProjection());
    }

    @SearchRestMethod(name = "byToken")
    public Page<ClarinVerificationTokenRest> findToken(@Parameter(value = "token", required = true) String token,
                                                         Pageable pageable) throws SQLException {
        Context context = obtainContext();

        ClarinVerificationToken clarinVerificationToken = clarinVerificationTokenService.findByToken(context, token);
        if (Objects.isNull(clarinVerificationToken)) {
            return null;
        }

        List<ClarinVerificationToken> clarinVerificationTokenList = new ArrayList<>();
        clarinVerificationTokenList.add(clarinVerificationToken);

        return converter.toRestPage(clarinVerificationTokenList, pageable, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("permitAll()")
    protected void delete(Context context, Integer id) throws AuthorizeException {
        ClarinVerificationToken clarinVerificationToken ;
        try {
            clarinVerificationToken = clarinVerificationTokenService.find(context, id);
            if (Objects.isNull(clarinVerificationToken)) {
                throw new RuntimeException("Cannot find the clarin verification token with the id: " + id);
            }
            clarinVerificationTokenService.delete(context, clarinVerificationToken);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete the clarin verification token with the id: " + id +
                    " because:" + e.getSQLState());
        }
    }

    @Override
    public Class<ClarinVerificationTokenRest> getDomainClass() {
        return ClarinVerificationTokenRest.class;
    }
}
