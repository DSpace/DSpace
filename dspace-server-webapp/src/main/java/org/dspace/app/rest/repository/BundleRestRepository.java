/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible for managing the Bundle Rest object
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */

@Component(BundleRest.CATEGORY + "." + BundleRest.NAME)
public class BundleRestRepository extends DSpaceObjectRestRepository<Bundle, BundleRest> {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private BundleService bundleService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private BitstreamFormatService bitstreamFormatService;

    public BundleRestRepository(BundleService dsoService) {
        super(dsoService);
        this.bundleService = dsoService;
    }

    @PreAuthorize("hasPermission(#uuid, 'BUNDLE', 'READ')")
    public BundleRest findOne(Context context, UUID uuid) {
        Bundle bundle = null;
        try {
            bundle = bundleService.find(context, uuid);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (bundle == null) {
            return null;
        }
        return converter.toRest(bundle, utils.obtainProjection());
    }

    public Page<BundleRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(BundleRest.NAME, "findAll");
    }

    /**
     * Apply a patch operation to a bundle
     *
     * @param context       The context
     * @param request       The http request
     * @param apiCategory   The API category e.g. "api"
     * @param model         The DSpace model e.g. "metadatafield"
     * @param uuid          The UUID of the bundle to perform patch operations on
     * @param patch         The JSON Patch (https://tools.ietf.org/html/rfc6902) operation
     * @throws AuthorizeException
     * @throws SQLException
     */
    @Override
    @PreAuthorize("hasPermission(#uuid, 'BUNDLE', 'WRITE')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID uuid,
                         Patch patch) throws AuthorizeException, SQLException {
        patchDSpaceObject(apiCategory, model, uuid, patch);
    }

    /**
     * Method to upload a bitstream to a bundle.
     *
     * @param context         The context
     * @param bundle          The bundle where the bitstream should be stored
     * @param fileName        The filename as it was uploaded
     * @param fileInputStream The input stream used to create the bitstream
     * @param properties      The properties to be assigned to the bitstream
     * @return The uploaded bitstream
     */
    public BitstreamRest uploadBitstream(Context context, Bundle bundle, String fileName, InputStream fileInputStream,
                                         String properties) {
        Item item = null;
        Bitstream bitstream = null;
        try {
            List<Item> items = bundle.getItems();
            if (!items.isEmpty()) {
                item = items.get(0);
            }
            if (item != null && !(authorizeService.authorizeActionBoolean(context, item, Constants.WRITE)
                    && authorizeService.authorizeActionBoolean(context, item, Constants.ADD))) {
                throw new AccessDeniedException("You do not have write rights to update the Bundle's item");
            }
            bitstream = processBitstreamCreation(context, bundle, fileInputStream, properties,
                                                 fileName);
            if (item != null) {
                itemService.update(context, item);
            }
            bundleService.update(context, bundle);
            context.commit();
        } catch (AuthorizeException | IOException | SQLException e) {
            String message = "Something went wrong with trying to create the single bitstream for file with filename: "
                    + fileName
                    + " for item with uuid: " + bundle.getID() + " and possible properties: " + properties;
            log.error(message, e);
            throw new RuntimeException(message, e);
        }

        return converter.toRest(bitstream, Projection.DEFAULT);
    }

    /**
     * Creates the bitstream based on the given parameters
     *
     * @param context          The context
     * @param bundle           The bundle where the bitstream should be stored
     * @param fileInputStream  The input stream used to create the bitstream
     * @param properties       The properties to be assigned to the bitstream
     * @param originalFilename The filename as it was uploaded
     * @return The bitstream which has been created
     */
    private Bitstream processBitstreamCreation(Context context, Bundle bundle, InputStream fileInputStream,
                                               String properties, String originalFilename)
            throws AuthorizeException, IOException, SQLException {

        Bitstream bitstream = null;
        if (StringUtils.isNotBlank(properties)) {
            ObjectMapper mapper = new ObjectMapper();
            BitstreamRest bitstreamRest = null;
            try {
                bitstreamRest = mapper.readValue(properties, BitstreamRest.class);
            } catch (Exception e) {
                throw new UnprocessableEntityException("The properties parameter was incorrect: " + properties);
            }
            bitstream = bitstreamService.create(context, bundle, fileInputStream);
            if (bitstreamRest.getMetadata() != null) {
                metadataConverter.setMetadata(context, bitstream, bitstreamRest.getMetadata());
            }
            String name = bitstreamRest.getName();
            if (StringUtils.isNotBlank(name)) {
                bitstream.setName(context, name);
            } else {
                bitstream.setName(context, originalFilename);
            }

        } else {
            bitstream = bitstreamService.create(context, bundle, fileInputStream);
            bitstream.setName(context, originalFilename);

        }
        BitstreamFormat bitstreamFormat = bitstreamFormatService.guessFormat(context, bitstream);
        bitstreamService.setFormat(context, bitstream, bitstreamFormat);
        bitstreamService.update(context, bitstream);

        return bitstream;
    }

    public Class<BundleRest> getDomainClass() {
        return BundleRest.class;
    }

    /**
     * Deletes a bundle whose uuid is given and deletes all the bitstreams it contains in BundleService.delete
     * @param context
     *            the dspace context
     * @param id
     *            the id of the bundle to delete
     */
    @Override
    @PreAuthorize("hasPermission(#id, 'BUNDLE', 'DELETE')")
    protected void delete(Context context, UUID id) throws AuthorizeException {
        Bundle bundleToDelete = null;
        try {
            bundleToDelete = bundleService.find(context, id);
            if (bundleToDelete == null) {
                throw new ResourceNotFoundException("Bundle with id: " + id + " not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Can't find a bundle with id: " + id, e);
        }
        try {
            bundleService.delete(context, bundleToDelete);
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Something went wrong trying to delete bundle with id: " + id, e);
        }
    }
}
