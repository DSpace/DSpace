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

    private Bitstream bitstream;
    private String name;
    private UUID uuid;
    private UUID currentUserUUID;
    private boolean shouldGenerateCoverPage;
    private byte[] file;
    private Long fileSize;

    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private CitationDocumentService citationDocumentService =
        new DSpace().getServiceManager()
            .getServicesByType(CitationDocumentService.class).get(0);

    public BitstreamResource(Bitstream bitstream, String name, UUID uuid, UUID currentUserUUID,
        boolean shouldGenerateCoverPage) {
        this.bitstream = bitstream;
        this.name = name;
        this.uuid = uuid;
        this.currentUserUUID = currentUserUUID;
        this.shouldGenerateCoverPage = shouldGenerateCoverPage;
    }

    private Pair<byte[], Long> getFileData(Context context, Bitstream bitstream) throws SQLException,
        AuthorizeException, IOException {
        if (file == null || fileSize == null) {
            if (shouldGenerateCoverPage) {
                var citedDocument = citationDocumentService.makeCitedDocument(context, bitstream);
                this.file = citedDocument.getLeft();
                this.fileSize = citedDocument.getRight();
            } else {
                var inputStream = bitstreamService.retrieve(context, bitstream);
                this.file = IOUtils.toByteArray(inputStream);
                inputStream.close();
                this.fileSize = bitstream.getSizeBytes();
            }
        }
        return Pair.of(file, fileSize);
    }

    @Override
    public String getDescription() {
        return "bitstream [" + uuid + "]";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        Context context = new Context();
        try {
            EPerson currentUser = ePersonService.find(context, currentUserUUID);
            context.setCurrentUser(currentUser);
            Bitstream bitstream = bitstreamService.find(context, uuid);
            return new ByteArrayInputStream(getFileData(context, bitstream).getLeft());
        } catch (SQLException | AuthorizeException e) {
            throw new IOException(e);
        } finally {
            try {
                context.complete();
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public String getFilename() {
        return name;
    }

    @Override
    public long contentLength() throws IOException {
        try (Context context = new Context()) {
            EPerson currentUser = ePersonService.find(context, currentUserUUID);
            context.setCurrentUser(currentUser);
            Bitstream bitstream = bitstreamService.find(context, uuid);
            return getFileData(context, bitstream).getRight();
        } catch (SQLException | AuthorizeException e) {
            throw new IOException(e);
        }
    }
}
