/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model.reader;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import org.dspace.app.iiif.model.ObjectMapperFactory;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ManifestReader {

    @Autowired
    protected BitstreamService bitstreamService;

    protected ObjectMapper objectMapper = ObjectMapperFactory.getIiifObjectMapper();
    protected SimpleModule iiifModule = ObjectMapperFactory.getIiifModule();

    /**
     * Get a Manifest object for an item. If the item does not have a dedicated manifest
     * bitstream, then one is generated from the item's metadata
     *
     * @param item DSpace item
     * @param context DSpace context
     * @return NULL if no manifest bitstream was found
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public Manifest getManifestResource(Item item, Context context)
            throws SQLException, IOException, AuthorizeException {
        Bitstream manifestBitstream = bitstreamService.getFirstBitstream(
            item, Constants.IIIF_MANIFEST_BUNDLE_NAME);

        if (manifestBitstream == null) {
            return null;
        }

        try (InputStream inputStream = bitstreamService.retrieve(context, manifestBitstream)) {
            return this.readManifest(inputStream);
        }
    }

    // Possibly cache this input stream read?
    private Manifest readManifest(InputStream in) throws IOException {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(iiifModule);

        return objectMapper.readValue(in, Manifest.class);
    }

}
