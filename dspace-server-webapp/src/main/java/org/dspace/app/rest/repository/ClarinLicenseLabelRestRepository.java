/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Clarin License Label Rest object
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component(ClarinLicenseLabelRest.CATEGORY + "." + ClarinLicenseLabelRest.NAME)
public class ClarinLicenseLabelRestRepository extends DSpaceRestRepository<ClarinLicenseLabelRest, Integer> {

    @Autowired
    ClarinLicenseLabelService clarinLicenseLabelService;

    @Override
    public ClarinLicenseLabelRest findOne(Context context, Integer id) {
        ClarinLicenseLabel clarinLicenseLabel;
        try {
            clarinLicenseLabel = clarinLicenseLabelService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (Objects.isNull(clarinLicenseLabel)) {
            return null;
        }
        return converter.toRest(clarinLicenseLabel, utils.obtainProjection());
    }

    @Override
    public Page<ClarinLicenseLabelRest> findAll(Context context, Pageable pageable) {
        try {
            List<ClarinLicenseLabel> clarinLicenseLabelList = clarinLicenseLabelService.findAll(context);
            return converter.toRestPage(clarinLicenseLabelList, pageable, utils.obtainProjection());
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // create
    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected ClarinLicenseLabelRest createAndReturn(Context context)
            throws AuthorizeException, SQLException {

        // parse request body
        ClarinLicenseLabelRest clarinLicenseLabelRest;
        try {
            clarinLicenseLabelRest = new ObjectMapper().readValue(
                    getRequestService().getCurrentRequest().getHttpServletRequest().getInputStream(),
                    ClarinLicenseLabelRest.class
            );
        } catch (IOException excIO) {
            throw new DSpaceBadRequestException("error parsing request body", excIO);
        }

        // validate fields
        if (isBlank(clarinLicenseLabelRest.getLabel()) || isBlank(clarinLicenseLabelRest.getTitle()) ||
                ArrayUtils.isEmpty(clarinLicenseLabelRest.getIcon())) {
            throw new UnprocessableEntityException("CLARIN License Label title, label, icon cannot be null or empty");
        }

        // create
        ClarinLicenseLabel clarinLicenseLabel;
        clarinLicenseLabel = clarinLicenseLabelService.create(context);
//        if (Objects.nonNull(clarinLicenseLabelRest.getId())) {
//            clarinLicenseLabel.setId(clarinLicenseLabelRest.getId());
//        }
        clarinLicenseLabel.setLabel(clarinLicenseLabelRest.getLabel());
        clarinLicenseLabel.setTitle(clarinLicenseLabelRest.getTitle());
        clarinLicenseLabel.setIcon(clarinLicenseLabelRest.getIcon());
        clarinLicenseLabel.setExtended(clarinLicenseLabelRest.isExtended());

        clarinLicenseLabelService.update(context, clarinLicenseLabel);
        // return
        return converter.toRest(clarinLicenseLabel, utils.obtainProjection());
    }


    @Override
    public Class<ClarinLicenseLabelRest> getDomainClass() {
        return ClarinLicenseLabelRest.class;
    }
}
