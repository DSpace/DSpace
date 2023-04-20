/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.disseminate.service.CitationDocumentService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.utils.DSpace;
import org.springframework.core.io.AbstractResource;

/**
 * This class acts as a {@link AbstractResource} used by Spring's framework to send the data in a proper and
 * streamlined way inside the {@link org.springframework.http.ResponseEntity} body.
 * This class' attributes are being used by Spring's framework in the overridden methods so that the proper
 * attributes are given and used in the response.
 */
public class BitstreamResource extends AbstractResource {

    private String name;
    private UUID uuid;
    private UUID currentUserUUID;
    private boolean shouldGenerateCoverPage;
    private byte[] file;
    private Set<UUID> currentSpecialGroups;

    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private CitationDocumentService citationDocumentService =
        new DSpace().getServiceManager()
                    .getServicesByType(CitationDocumentService.class).get(0);

    public BitstreamResource(String name, UUID uuid, UUID currentUserUUID, Set<UUID> currentSpecialGroups,
        boolean shouldGenerateCoverPage) {
        this.name = name;
        this.uuid = uuid;
        this.currentUserUUID = currentUserUUID;
        this.currentSpecialGroups = currentSpecialGroups;
        this.shouldGenerateCoverPage = shouldGenerateCoverPage;
    }

    /**
     * Get Potential cover page by array, this method should only be called when a coverpage should be generated
     * In case of failure the original file will be returned
     *
     * @param context   the DSpace context
     * @param bitstream the pdf for which we want to generate a coverpage
     * @return a byte array containing the cover page
     */
    private byte[] getCoverpageByteArray(Context context, Bitstream bitstream)
        throws IOException, SQLException, AuthorizeException {
        if (file == null) {
            try {
                Pair<byte[], Long> citedDocument = citationDocumentService.makeCitedDocument(context, bitstream);
                this.file = citedDocument.getLeft();
            } catch (Exception e) {
                // Return the original bitstream without the cover page
                this.file = IOUtils.toByteArray(bitstreamService.retrieve(context, bitstream));
            }
        }
        return file;
    }

    @Override
    public String getDescription() {
        return "bitstream [" + uuid + "]";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try (Context context = initializeContext()) {

            Bitstream bitstream = bitstreamService.find(context, uuid);
            InputStream out;

            if (shouldGenerateCoverPage) {
                out = new ByteArrayInputStream(getCoverpageByteArray(context, bitstream));
            } else {
                out = bitstreamService.retrieve(context, bitstream);
            }

            this.file = null;
            return out;
        } catch (SQLException | AuthorizeException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getFilename() {
        return name;
    }

    @Override
    public long contentLength() throws IOException {
        try (Context context = initializeContext()) {
            Bitstream bitstream = bitstreamService.find(context, uuid);
            if (shouldGenerateCoverPage) {
                return getCoverpageByteArray(context, bitstream).length;
            } else {
                return bitstream.getSizeBytes();
            }
        } catch (SQLException | AuthorizeException e) {
            throw new IOException(e);
        }
    }

    private Context initializeContext() throws SQLException {
        Context context = new Context();
        EPerson currentUser = ePersonService.find(context, currentUserUUID);
        context.setCurrentUser(currentUser);
        currentSpecialGroups.forEach(context::setSpecialGroup);
        return context;
    }
}
