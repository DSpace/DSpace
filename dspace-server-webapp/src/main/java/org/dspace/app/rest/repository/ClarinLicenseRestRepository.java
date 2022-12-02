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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Clarin License Rest object
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component(ClarinLicenseRest.CATEGORY + "." + ClarinLicenseRest.NAME)
public class ClarinLicenseRestRepository extends DSpaceRestRepository<ClarinLicenseRest, Integer> {

    public static final String OPERATION_PATH_LICENSE_RESOURCE = "license";

    public static final String OPERATION_PATH_LICENSE_GRANTED = "granted-license";

    @Autowired
    ClarinLicenseService clarinLicenseService;

    @Autowired
    WorkspaceItemService wis;

    @Autowired
    ItemService itemService;

    @Override
    @PreAuthorize("permitAll()")
    public ClarinLicenseRest findOne(Context context, Integer idValue) {
        ClarinLicense clarinLicense;
        try {
            clarinLicense = clarinLicenseService.find(context, idValue);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (Objects.isNull(clarinLicense)) {
            return null;
        }
        return converter.toRest(clarinLicense, utils.obtainProjection());
    }

    @SearchRestMethod(name = "byName")
    public Page<ClarinLicenseRest> findByName(@Parameter(value = "name", required = true) String name,
                                              Pageable pageable) {
        List<ClarinLicense> clarinLicenseList = new ArrayList<>();
        ClarinLicense clarinLicense;
        try {
            Context context = obtainContext();
            clarinLicense = clarinLicenseService.findByName(context, name);
            if (Objects.isNull(clarinLicense)) {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        clarinLicenseList.add(clarinLicense);
        return converter.toRestPage(clarinLicenseList, pageable, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ClarinLicenseRest> findAll(Context context, Pageable pageable) {
        try {
            List<ClarinLicense> clarinLicenseList = clarinLicenseService.findAll(context);
            return converter.toRestPage(clarinLicenseList, pageable, utils.obtainProjection());
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected ClarinLicenseRest createAndReturn(Context context)
            throws AuthorizeException, SQLException {

        // parse request body
        ClarinLicenseRest clarinLicenseRest;
        try {
            clarinLicenseRest = new ObjectMapper().readValue(
                    getRequestService().getCurrentRequest().getHttpServletRequest().getInputStream(),
                    ClarinLicenseRest.class
            );
        } catch (IOException excIO) {
            throw new DSpaceBadRequestException("error parsing request body", excIO);
        }

        // validate fields
        if (isBlank(clarinLicenseRest.getName()) || isBlank(clarinLicenseRest.getDefinition())) {
            throw new UnprocessableEntityException("Clarin License name, definition, " +
                    "license label cannot be null or empty");
        }

        // create
        ClarinLicense clarinLicense;
        clarinLicense = clarinLicenseService.create(context);
        clarinLicense.setName(clarinLicenseRest.getName());
        clarinLicense.setLicenseLabels(this.getClarinLicenseLabels(clarinLicenseRest.getClarinLicenseLabel(),
                clarinLicenseRest.getExtendedClarinLicenseLabels()));
        clarinLicense.setDefinition(clarinLicenseRest.getDefinition());
        clarinLicense.setConfirmation(clarinLicenseRest.getConfirmation());
        clarinLicense.setRequiredInfo(clarinLicenseRest.getRequiredInfo());

        clarinLicenseService.update(context, clarinLicense);
        // return
        return converter.toRest(clarinLicense, utils.obtainProjection());
    }

    @Override
    public void patch(Context context, HttpServletRequest request, String apiCategory, String model, Integer id,
                                   Patch patch) throws SQLException, AuthorizeException {
        // load
        List<Operation> operations = patch.getOperations();
        WorkspaceItem source = wis.find(context, id);
        Item item = source.getItem();
        itemService.setMetadataSingleValue(context, item, "dc", "rights","license", null, "license");
        itemService.update(context, item);
        wis.update(context, source);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected ClarinLicenseRest put(Context context, HttpServletRequest request, String apiCategory, String model,
                                    Integer id, JsonNode jsonNode) throws SQLException, AuthorizeException {

        ClarinLicenseRest clarinLicenseRest = new Gson().fromJson(jsonNode.toString(), ClarinLicenseRest.class);

        if (Objects.isNull(clarinLicenseRest)) {
            throw new RuntimeException("Cannot parse ClarinLicenseRest object from request.");
        }

        if (Objects.isNull(clarinLicenseRest.getClarinLicenseLabel()) ||
                Objects.isNull(clarinLicenseRest.getExtendedClarinLicenseLabels()) ||
                StringUtils.isBlank(clarinLicenseRest.getName()) ||
                StringUtils.isBlank(clarinLicenseRest.getDefinition())) {
            throw new UnprocessableEntityException("The ClarinLicense doesn't have required properties or some " +
                    "some property is null.");
        }

        ClarinLicense clarinLicense = clarinLicenseService.find(context, id);
        if (Objects.isNull(clarinLicense)) {
            throw new ResourceNotFoundException("Clarin License with id: " + id + " not found");
        }

        clarinLicense.setName(clarinLicenseRest.getName());
        clarinLicense.setRequiredInfo(clarinLicenseRest.getRequiredInfo());
        clarinLicense.setDefinition(clarinLicenseRest.getDefinition());
        clarinLicense.setConfirmation(clarinLicenseRest.getConfirmation());
        clarinLicense.setLicenseLabels(this.getClarinLicenseLabels(clarinLicenseRest.getClarinLicenseLabel(),
                clarinLicenseRest.getExtendedClarinLicenseLabels()));

        clarinLicenseService.update(context, clarinLicense);

        return converter.toRest(clarinLicense, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, Integer id) throws AuthorizeException {

        try {
            ClarinLicense clarinLicense = clarinLicenseService.find(context, id);

            if (Objects.isNull(clarinLicense)) {
                throw new ResourceNotFoundException("Clarin license with id: " + id + " not found");
            }

            clarinLicenseService.delete(context, clarinLicense);
        } catch (SQLException e) {
            throw new RuntimeException("Error while trying to delete " + ClarinLicenseRest.NAME + " with id: " + id, e);
        }
    }

    @Override
    public Class<ClarinLicenseRest> getDomainClass() {
        return ClarinLicenseRest.class;
    }

    private Set<ClarinLicenseLabel> getClarinLicenseLabels(ClarinLicenseLabelRest clarinLicenseLabelRest,
                                                           List<ClarinLicenseLabelRest> extendedClarinLicenseLabels) {
        Set<ClarinLicenseLabel> clarinLicenseLabels = new HashSet<>();

        clarinLicenseLabels.add(getClarinLicenseLabelFromRest(clarinLicenseLabelRest));
        extendedClarinLicenseLabels.forEach(cllr -> {
            clarinLicenseLabels.add(getClarinLicenseLabelFromRest(cllr));
        });

        return clarinLicenseLabels;
    }

    private ClarinLicenseLabel getClarinLicenseLabelFromRest(ClarinLicenseLabelRest clarinLicenseLabelRest) {
        ClarinLicenseLabel clarinLicenseLabel = new ClarinLicenseLabel();
        clarinLicenseLabel.setLabel(clarinLicenseLabelRest.getLabel());
        clarinLicenseLabel.setTitle(clarinLicenseLabelRest.getTitle());
        clarinLicenseLabel.setExtended(clarinLicenseLabelRest.isExtended());
        clarinLicenseLabel.setIcon(clarinLicenseLabelRest.getIcon());
        clarinLicenseLabel.setId(clarinLicenseLabelRest.getId());
        return clarinLicenseLabel;
    }

}
