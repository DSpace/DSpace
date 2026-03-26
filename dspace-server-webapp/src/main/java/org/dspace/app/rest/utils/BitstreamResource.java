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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.util.DigestUtils;

/**
 * This class acts as a {@link AbstractResource} used by Spring's framework to send the data in a proper and
 * streamlined way inside the {@link org.springframework.http.ResponseEntity} body.
 * This class' attributes are being used by Spring's framework in the overridden methods so that the proper
 * attributes are given and used in the response.
 */
public class BitstreamResource extends AbstractResource {

    static final Logger LOG = LogManager.getLogger(BitstreamResource.class);

    protected final String name;
    protected final UUID uuid;
    protected final UUID currentUserUUID;
    protected final boolean shouldGenerateCoverPage;
    protected final Set<UUID> currentSpecialGroups;
    protected final boolean skipAuthCheck;


    protected final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected final CitationDocumentService citationDocumentService =
            new DSpace().getServiceManager()
                    .getServicesByType(CitationDocumentService.class).get(0);

    protected BitstreamDocument document;

    public BitstreamResource(String name, UUID uuid, UUID currentUserUUID, Set<UUID> currentSpecialGroups,
        boolean shouldGenerateCoverPage, boolean skipAuth) {
        this.name = name;
        this.uuid = uuid;
        this.currentUserUUID = currentUserUUID;
        this.currentSpecialGroups = currentSpecialGroups;
        this.shouldGenerateCoverPage = shouldGenerateCoverPage;
        this.skipAuthCheck = skipAuth;
    }

    /**
     * Get Potential cover page by array, this method should only be called when a coverpage should be generated
     * In case of failure the original file will be returned
     *
     * @param context   the DSpace context
     * @param bitstream the pdf for which we want to generate a coverpage
     * @return a byte array containing the cover page
     */
    byte[] getCoverpageByteArray(Context context, Bitstream bitstream)
            throws IOException, SQLException, AuthorizeException {
        try {
            var citedDocument = citationDocumentService.makeCitedDocument(context, bitstream);
            return citedDocument.getLeft();
        } catch (Exception e) {
            LOG.warn("Could not generate cover page. Will fallback to original document", e);
            // Return the original bitstream without the cover page
            return IOUtils.toByteArray(bitstreamService.retrieve(context, bitstream));
        }
    }

    @Override
    public String getDescription() {
        return "bitstream [" + uuid + "]";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        fetchDocument();

        return document.inputStream();
    }

    @Override
    public String getFilename() {
        return name;
    }

    @Override
    public long contentLength() throws IOException {
        fetchDocument();

        return document.length();
    }

    public String getChecksum() {
        fetchDocument();

        return document.etag();
    }

    void fetchDocument() {
        if (document != null) {
            return;
        }

        try (Context context = initializeContext()) {
            Bitstream bitstream = bitstreamService.find(context, uuid);
            if (shouldGenerateCoverPage) {
                var coverPage = getCoverpageByteArray(context, bitstream);

                this.document = new BitstreamDocument(etag(bitstream),
                        coverPage.length,
                        new ByteArrayInputStream(coverPage));
            } else {
                this.document = new BitstreamDocument(bitstream.getChecksum(),
                        bitstream.getSizeBytes(),
                        bitstreamService.retrieve(context, bitstream));
            }
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException(e);
        }

        LOG.debug("fetched document {} {}", shouldGenerateCoverPage, document);
    }

    String etag(Bitstream bitstream) {

         /* Ideally we would calculate the md5 checksum based on the document with coverpage.
          However it looks like the coverpage generation is not stable (e.g. if invoked twice it will return
         different results). This means we cannot use it for etag calculation/comparison!

         Instead we will create the MD5 based off the original checksum plus fixed prefix. This ensures
         that checksums will differ when coverpage is on/off.
         However the checksum will _not_ change if the coverpage content changes.
          */

        var content = "coverpage:" + bitstream.getChecksum();

        StringBuilder builder = new StringBuilder(37);
        DigestUtils.appendMd5DigestAsHex(content.getBytes(), builder);

        return builder.toString();
    }

    Context initializeContext() throws SQLException {
        Context context = new Context();
        EPerson currentUser = ePersonService.find(context, currentUserUUID);
        context.setCurrentUser(currentUser);
        currentSpecialGroups.forEach(context::setSpecialGroup);
        if (skipAuthCheck) {
            context.turnOffAuthorisationSystem();
        }
        return context;
    }

    record BitstreamDocument(String etag, long length, InputStream inputStream) {}
}
