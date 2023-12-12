/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import java.io.IOException;
import java.sql.SQLException;

import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import org.dspace.app.iiif.model.generator.CanvasGenerator;
import org.dspace.app.iiif.model.reader.ManifestReader;
import org.dspace.app.iiif.service.utils.IIIFUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This service provides methods for creating a single {@code Canvas}. There should be a single instance of
 * this service per request. The {@code @RequestScope} provides a single instance created and available during
 * complete lifecycle of the HTTP request.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RequestScope
@Component
public class CanvasLookupService extends AbstractResourceService {

    @Autowired
    IIIFUtils utils;

    @Autowired
    CanvasService canvasService;

    @Autowired
    ManifestReader reader;

    public CanvasLookupService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    public String getCanvas(Context context, Item item, String canvasId) {
        Manifest manifest;
        try {
            manifest = reader.getManifestResource(item, context);
        } catch (SQLException | IOException | AuthorizeException e) {
            throw new RuntimeException(e);
        }

        Canvas canvas = this.getFromManifest(manifest, canvasId);
        if (canvas != null) {
            return utils.asJson(canvas);
        }

        return generateCanvas(context, item, canvasId);
    }

    private String generateCanvas(Context context, Item item, String canvasId) {
        int canvasPosition = utils.getCanvasId(canvasId);
        Bitstream bitstream = utils.getBitstreamForCanvas(context, item, canvasPosition);
        if (bitstream == null) {
            throw new ResourceNotFoundException();
        }
        String mimeType = utils.getBitstreamMimeType(bitstream, context);
        CanvasGenerator canvasGenerator;
        try {
            canvasGenerator = canvasService.getCanvas(context, item.getID().toString(), bitstream,
                    bitstream.getBundles().get(0), item, canvasPosition, mimeType);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return utils.asJson(canvasGenerator.generateResource());
    }

    private Canvas getFromManifest(Manifest manifest, String canvasId) {
        if (manifest == null) {
            return null;
        }
        return manifest.getDefaultSequence().getCanvases().stream()
            .filter(c -> c.getIdentifier().toString().equals(canvasId))
            .findFirst()
            .orElse(null);
    }

}
