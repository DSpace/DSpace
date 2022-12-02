/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.clarin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.ws.rs.NotFoundException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NullArgumentException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.dao.clarin.ClarinLicenseResourceMappingDAO;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.content.service.clarin.ClarinLicenseResourceUserAllowanceService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ClarinLicenseResourceMappingServiceImpl implements ClarinLicenseResourceMappingService {

    private static final Logger log = LoggerFactory.getLogger(ClarinLicenseServiceImpl.class);

    @Autowired
    ClarinLicenseResourceMappingDAO clarinLicenseResourceMappingDAO;
    @Autowired
    ClarinLicenseResourceUserAllowanceService clarinLicenseResourceUserAllowanceService;

    @Autowired
    ClarinLicenseService clarinLicenseService;

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    AuthorizeService authorizeService;

    @Override
    public ClarinLicenseResourceMapping create(Context context) throws SQLException {
        // Create a table row
        ClarinLicenseResourceMapping clarinLicenseResourceMapping = clarinLicenseResourceMappingDAO.create(context,
                new ClarinLicenseResourceMapping());

        log.info(LogHelper.getHeader(context, "create_clarin_license_resource_mapping",
                "clarin_license_resource_mapping_id=" + clarinLicenseResourceMapping.getID()));

        return clarinLicenseResourceMapping;
    }

    @Override
    public ClarinLicenseResourceMapping create(Context context,
                                               ClarinLicenseResourceMapping clarinLicenseResourceMapping)
            throws SQLException {
        return clarinLicenseResourceMappingDAO.create(context, clarinLicenseResourceMapping);
    }

    @Override
    public ClarinLicenseResourceMapping create(Context context, Integer licenseId, UUID bitstreamUuid)
            throws SQLException {
        ClarinLicenseResourceMapping clarinLicenseResourceMapping = new ClarinLicenseResourceMapping();
        ClarinLicense clarinLicense = clarinLicenseService.find(context, licenseId);
        if (Objects.isNull(clarinLicense)) {
            throw new NotFoundException("Cannot find the license with id: " + licenseId);
        }

        Bitstream bitstream = bitstreamService.find(context, bitstreamUuid);
        if (Objects.isNull(bitstream)) {
            throw new NotFoundException("Cannot find the bitstream with id: " + bitstreamUuid);
        }
        clarinLicenseResourceMapping.setLicense(clarinLicense);
        clarinLicenseResourceMapping.setBitstream(bitstream);

        return clarinLicenseResourceMappingDAO.create(context, clarinLicenseResourceMapping);
    }

    @Override
    public ClarinLicenseResourceMapping find(Context context, int valueId) throws SQLException {
        return clarinLicenseResourceMappingDAO.findByID(context, ClarinLicenseResourceMapping.class, valueId);
    }

    @Override
    public List<ClarinLicenseResourceMapping> findAll(Context context) throws SQLException {
        return clarinLicenseResourceMappingDAO.findAll(context, ClarinLicenseResourceMapping.class);
    }

    @Override
    public List<ClarinLicenseResourceMapping> findAllByLicenseId(Context context, Integer licenseId)
            throws SQLException {
        List<ClarinLicenseResourceMapping> mappings =
                clarinLicenseResourceMappingDAO.findAll(context, ClarinLicenseResourceMapping.class);
        List<ClarinLicenseResourceMapping> mappingsByLicenseId = new ArrayList<>();
        for (ClarinLicenseResourceMapping mapping: mappings) {
            if (Objects.equals(mapping.getLicense().getID(), licenseId)) {
                mappingsByLicenseId.add(mapping);
            }
        }
        return mappingsByLicenseId;
    }

    @Override
    public void update(Context context, ClarinLicenseResourceMapping newClarinLicenseResourceMapping)
            throws SQLException {
        if (Objects.isNull(newClarinLicenseResourceMapping)) {
            throw new NullArgumentException("Cannot update clarin license resource mapping because " +
                    "the new clarin license resource mapping is null");
        }

        ClarinLicenseResourceMapping foundClarinLicenseResourceMapping =
                find(context, newClarinLicenseResourceMapping.getID());
        if (Objects.isNull(foundClarinLicenseResourceMapping)) {
            throw new ObjectNotFoundException(newClarinLicenseResourceMapping.getID(), "Cannot update " +
                    "the license resource mapping because" +
                    " the clarin license resource mapping wasn't found " +
                    "in the database.");
        }

        clarinLicenseResourceMappingDAO.save(context, newClarinLicenseResourceMapping);
    }

    @Override
    public void delete(Context context, ClarinLicenseResourceMapping clarinLicenseResourceMapping)
            throws SQLException {
        clarinLicenseResourceMappingDAO.delete(context, clarinLicenseResourceMapping);
    }

    @Override
    public void detachLicenses(Context context, Bitstream bitstream) throws SQLException {
        List<ClarinLicenseResourceMapping> clarinLicenseResourceMappings =
                clarinLicenseResourceMappingDAO.findByBitstreamUUID(context, bitstream.getID());

        if (CollectionUtils.isEmpty(clarinLicenseResourceMappings)) {
            log.info("Cannot detach licenses because bitstream with id: " + bitstream.getID() + " is not " +
                    "attached to any license.");
            return;
        }

        clarinLicenseResourceMappings.forEach(clarinLicenseResourceMapping -> {
            try {
                this.delete(context, clarinLicenseResourceMapping);
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        });
    }

    @Override
    public void attachLicense(Context context, ClarinLicense clarinLicense, Bitstream bitstream) throws SQLException {
        ClarinLicenseResourceMapping clarinLicenseResourceMapping = this.create(context);
        if (Objects.isNull(clarinLicenseResourceMapping)) {
            throw new NotFoundException("Cannot create the ClarinLicenseResourceMapping.");
        }
        if (Objects.isNull(clarinLicense) || Objects.isNull(bitstream)) {
            throw new NullArgumentException("CLARIN License or Bitstream cannot be null.");
        }

        clarinLicenseResourceMapping.setBitstream(bitstream);
        clarinLicenseResourceMapping.setLicense(clarinLicense);

        clarinLicenseResourceMappingDAO.save(context, clarinLicenseResourceMapping);
    }

    @Override
    public List<ClarinLicenseResourceMapping> findByBitstreamUUID(Context context, UUID bitstreamID)
            throws SQLException {
        return clarinLicenseResourceMappingDAO.findByBitstreamUUID(context, bitstreamID);
    }

    @Override
    public ClarinLicense getLicenseToAgree(Context context, UUID userId, UUID resourceID) throws SQLException {
        // Load Clarin License for current bitstream.
        List<ClarinLicenseResourceMapping> clarinLicenseResourceMappings =
                clarinLicenseResourceMappingDAO.findByBitstreamUUID(context, resourceID);

        // Check there is mappings for the clarin license and bitstream
        if (CollectionUtils.isEmpty(clarinLicenseResourceMappings)) {
            return null;
        }

        // Get the first resource mapping, get only fist - there shouldn't b more mappings
        ClarinLicenseResourceMapping clarinLicenseResourceMapping = clarinLicenseResourceMappings.get(0);
        if (Objects.isNull(clarinLicenseResourceMapping)) {
            return null;
        }

        // Get Clarin License from resource mapping to get confirmation policies.
        ClarinLicense clarinLicenseToAgree = clarinLicenseResourceMapping.getLicense();
        if (Objects.isNull(clarinLicenseToAgree)) {
            return null;
        }

        // Confirmation states:
        // 0 - Not required
        // 1 - Ask only once
        // 2 - Ask always
        // 3 - Allow anonymous
        if (Objects.equals(clarinLicenseToAgree.getConfirmation(), 0)) {
            return null;
        }

        switch (clarinLicenseToAgree.getConfirmation()) {
            case 1:
                // Ask only once - check if the clarin license required info is filled in by the user
                if (userFilledInRequiredInfo(context, clarinLicenseResourceMapping, userId)) {
                    return null;
                }
                return clarinLicenseToAgree;
            case 2:
            case 3:
                return clarinLicenseToAgree;
            default:
                return null;
        }
    }

    private boolean userFilledInRequiredInfo(Context context,
                                             ClarinLicenseResourceMapping clarinLicenseResourceMapping, UUID userID)
            throws SQLException {
        if (Objects.isNull(userID)) {
            return false;
        }

        // Find all records when the current user fill in some clarin license required info
        List<ClarinLicenseResourceUserAllowance> clarinLicenseResourceUserAllowances =
                clarinLicenseResourceUserAllowanceService.findByEPersonId(context, userID);
        // The user hasn't been filled in any information.
        if (CollectionUtils.isEmpty(clarinLicenseResourceUserAllowances)) {
            return false;
        }

        // The ClarinLicenseResourceMapping.id record is in the ClarinLicenseResourceUserAllowance
        // that means the user added some information for the downloading bitstream's license.
        for (ClarinLicenseResourceUserAllowance clrua : clarinLicenseResourceUserAllowances) {
            int userAllowanceMappingID = clrua.getLicenseResourceMapping().getID();
            int resourceMappingID = clarinLicenseResourceMapping.getID();
            if (Objects.equals(userAllowanceMappingID, resourceMappingID)) {
                return true;
            }
        }

        return false;
    }
}
