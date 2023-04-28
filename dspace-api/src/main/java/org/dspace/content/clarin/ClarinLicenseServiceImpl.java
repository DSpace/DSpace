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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NullArgumentException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.clarin.ClarinLicenseDAO;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the ClarinLicense object.
 * This class is responsible for all business logic calls for the ClarinLicense object and
 * is autowired by spring. This class should never be accessed directly.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinLicenseServiceImpl implements ClarinLicenseService {

    private static final Logger log = LoggerFactory.getLogger(ClarinLicenseServiceImpl.class);

    @Autowired
    ClarinLicenseDAO clarinLicenseDAO;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    ItemService itemService;

    @Autowired
    ClarinLicenseService clarinLicenseService;

    @Autowired
    ClarinLicenseResourceMappingService clarinLicenseResourceMappingService;

    @Override
    public ClarinLicense create(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to create an CLARIN License");
        }

        // Create a table row
        ClarinLicense clarinLicense = clarinLicenseDAO.create(context, new ClarinLicense());

        log.info(LogHelper.getHeader(context, "create_clarin_license", "clarin_license_id="
                + clarinLicense.getID()));

        return clarinLicense;
    }

    @Override
    public ClarinLicense create(Context context, ClarinLicense clarinLicense) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to create an CLARIN License");
        }

        return clarinLicenseDAO.create(context, clarinLicense);
    }

    @Override
    public ClarinLicense find(Context context, int valueId) throws SQLException {
        return clarinLicenseDAO.findByID(context, ClarinLicense.class, valueId);
    }

    @Override
    public ClarinLicense findByName(Context context, String name) throws SQLException {
        return clarinLicenseDAO.findByName(context, name);
    }

    @Override
    public List<ClarinLicense> findByNameLike(Context context, String name) throws SQLException {
        return clarinLicenseDAO.findByNameLike(context, name);
    }

    @Override
    public void addLicenseMetadataToItem(Context context, ClarinLicense clarinLicense, Item item) throws SQLException {
        if (Objects.isNull(clarinLicense) || Objects.isNull(item)) {
            log.error("Cannot add clarin license to the item metadata because the Item or the CLARIN License is null.");
        }
        if (Objects.isNull(clarinLicense.getDefinition()) ||
                Objects.isNull(clarinLicense.getNonExtendedClarinLicenseLabel()) ||
                Objects.isNull(clarinLicense.getName())) {
            log.error("Cannot add clarin license to the item metadata because one of the necessary clairn license" +
                    "attribute is null: " +
                    "nonExtendedClarinLicenseLabel: " + clarinLicense.getNonExtendedClarinLicenseLabel() +
                    ", name: " + clarinLicense.getName() +
                    ", definition: " + clarinLicense.getDefinition());
        }
        itemService.addMetadata(context, item, "dc", "rights", "uri", Item.ANY,
                clarinLicense.getDefinition());
        itemService.addMetadata(context, item, "dc", "rights", null, Item.ANY,
                clarinLicense.getName());
        itemService.addMetadata(context, item, "dc", "rights", "label", Item.ANY,
                clarinLicense.getNonExtendedClarinLicenseLabel().getLabel());
    }

    @Override
    public void clearLicenseMetadataFromItem(Context context, Item item) throws SQLException {
        itemService.clearMetadata(context, item, "dc", "rights", "holder", Item.ANY);
        itemService.clearMetadata(context, item,"dc", "rights", "uri", Item.ANY);
        itemService.clearMetadata(context, item, "dc", "rights", null, Item.ANY);
        itemService.clearMetadata(context, item, "dc", "rights", "label", Item.ANY);
    }

    @Override
    public void addClarinLicenseToBitstream(Context context, Item item, Bundle bundle, Bitstream bitstream) {
        try {
            if (!Objects.equals(bundle.getName(), Constants.CONTENT_BUNDLE_NAME)) {
                return;
            }

            if (Objects.isNull(item)) {
                return;
            }

            List<MetadataValue> dcRights =
                    itemService.getMetadata(item, "dc", "rights", null, Item.ANY);
            List<MetadataValue> dcRightsUri =
                    itemService.getMetadata(item, "dc", "rights", "uri", Item.ANY);

            String licenseName = null;
            // If the item bitstreams has license
            if (CollectionUtils.isNotEmpty(dcRights)) {
                if ( dcRights.size() != dcRightsUri.size() ) {
                    log.warn( String.format("Harvested bitstream [%s / %s] has different length of " +
                            "dc_rights and dc_rights_uri", bitstream.getName(), bitstream.getHandle()));
                    licenseName = "unknown";
                } else {
                    licenseName = Objects.requireNonNull(dcRights.get(0)).getValue();
                }
            }

            ClarinLicense clarinLicense = this.clarinLicenseService.findByName(context, licenseName);
            // The item bitstreams doesn't have the license
            if (Objects.isNull(clarinLicense)) {
                log.info("Cannot find clarin license with name: " + licenseName);
                return;
            }

            // The item bitstreams has the license -> detach old license and attach the new license
            List<Bundle> bundles = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
            for (Bundle clarinBundle : bundles) {
                List<Bitstream> bitstreamList = clarinBundle.getBitstreams();
                for (Bitstream bundleBitstream : bitstreamList) {
                    // in case bitstream ID exists in license table for some reason .. just remove it
                    this.clarinLicenseResourceMappingService.detachLicenses(context, bundleBitstream);
                    // add the license to bitstream
                    this.clarinLicenseResourceMappingService.attachLicense(context, clarinLicense, bundleBitstream);
                }
            }

            this.clearLicenseMetadataFromItem(context, item);
            this.addLicenseMetadataToItem(context, clarinLicense, item);
        } catch (SQLException | AuthorizeException e) {
            log.error("Something went wrong in the maintenance of clarin license in the bitstream bundle: "
                    + e.getMessage());
        }
    }

    @Override
    public List<ClarinLicense> findAll(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                     "You must be an admin to create an CLARIN License");
        }

        return clarinLicenseDAO.findAll(context, ClarinLicense.class);
    }


    @Override
    public void delete(Context context, ClarinLicense clarinLicense) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to create an CLARIN License");
        }

        clarinLicenseDAO.delete(context, clarinLicense);
    }

    @Override
    public void update(Context context, ClarinLicense newClarinLicense) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to create an CLARIN License");
        }

        if (Objects.isNull(newClarinLicense)) {
            throw new NullArgumentException("Cannot update clarin license because the new clarin license is null");
        }

        ClarinLicense foundClarinLicense = find(context, newClarinLicense.getID());
        if (Objects.isNull(foundClarinLicense)) {
            throw new ObjectNotFoundException(newClarinLicense.getID(),
                    "Cannot update the license because the clarin license wasn't found in the database.");
        }

        clarinLicenseDAO.save(context, newClarinLicense);
    }
}
