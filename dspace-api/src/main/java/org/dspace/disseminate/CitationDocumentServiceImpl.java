/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.disseminate.service.CitationDocumentService;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Citation Document produces a dissemination package (DIP) that is different that the archival package (AIP).
 * In this case we append the descriptive metadata to the end (configurable) of the document. i.e. last page of PDF.
 * So instead of getting the original PDF, you get a cPDF (with citation information added).
 *
 * @author Peter Dietz (peter@longsight.com)
 */
public class CitationDocumentServiceImpl implements CitationDocumentService, InitializingBean {
    /**
     * Class Logger
     */
    private static final Logger log = LogManager.getLogger(CitationDocumentServiceImpl.class);

    /**
     * A set of MIME types that can have a citation page added to them. That is,
     * MIME types in this set can be converted to a PDF which is then prepended
     * with a citation page.
     */
    protected final Set<String> VALID_TYPES = new HashSet<>(2);

    /**
     * A set of MIME types that refer to a PDF
     */
    protected final Set<String> PDF_MIMES = new HashSet<>(2);

    /**
     * A set of MIME types that refer to a JPEG, PNG, or GIF
     */
    protected final Set<String> RASTER_MIMES = new HashSet<>();
    /**
     * A set of MIME types that refer to a SVG
     */
    protected final Set<String> SVG_MIMES = new HashSet<>();

    /**
     * List of all enabled collections, inherited/determined for those under communities.
     */
    protected List<String> citationEnabledCollectionsList;

    protected File tempDir;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected CommunityService communityService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected HandleService handleService;

    @Autowired
    CoverPageService coverPageService;

    @Override
    public void afterPropertiesSet() throws Exception {
        // Add valid format MIME types to set. This could be put in the Schema
        // instead.
        //Populate RASTER_MIMES
        SVG_MIMES.add("image/jpeg");
        SVG_MIMES.add("image/pjpeg");
        SVG_MIMES.add("image/png");
        SVG_MIMES.add("image/gif");
        //Populate SVG_MIMES
        SVG_MIMES.add("image/svg");
        SVG_MIMES.add("image/svg+xml");


        //Populate PDF_MIMES
        PDF_MIMES.add("application/pdf");
        PDF_MIMES.add("application/x-pdf");

        //Populate VALID_TYPES
        VALID_TYPES.addAll(PDF_MIMES);

        // Global enabled?
        citationEnabledGlobally = configurationService.getBooleanProperty("citation-page.enable_globally", false);

        //Load enabled collections
        String[] citationEnabledCollections = configurationService
                .getArrayProperty("citation-page.enabled_collections");
        citationEnabledCollectionsList = new ArrayList<String>(Arrays.asList(citationEnabledCollections));

        //Load enabled communities, and add to collection-list
        String[] citationEnabledCommunities = configurationService
                .getArrayProperty("citation-page.enabled_communities");
        if (citationEnabledCollectionsList == null) {
            citationEnabledCollectionsList = new ArrayList<>();
        }

        if (citationEnabledCommunities != null && citationEnabledCommunities.length > 0) {
            Context context = null;
            try {
                context = new Context();
                for (String communityString : citationEnabledCommunities) {
                    DSpaceObject dsoCommunity = handleService.resolveToObject(context, communityString.trim());
                    if (dsoCommunity instanceof Community) {
                        Community community = (Community) dsoCommunity;
                        List<Collection> collections = communityService.getAllCollections(context, community);

                        for (Collection collection : collections) {
                            citationEnabledCollectionsList.add(collection.getHandle());
                        }
                    } else {
                        log.error(
                                "Invalid community for citation.enabled_communities, value:" + communityString.trim());
                    }
                }
            } catch (SQLException e) {
                log.error(e.getMessage());
            } finally {
                if (context != null) {
                    context.abort();
                }
            }
        }

        //Ensure a temp directory is available
        String tempDirString = configurationService.getProperty("dspace.dir") + File.separator + "temp";
        tempDir = new File(tempDirString);
        if (!tempDir.exists()) {
            boolean success = tempDir.mkdir();
            if (success) {
                log.info("Created temp directory at: " + tempDirString);
            } else {
                log.info("Unable to create temp directory at: " + tempDirString);
            }
        }
    }

    protected CitationDocumentServiceImpl() {
    }

    /**
     * Boolean to determine is citation-functionality is enabled globally for entire site.
     * config/modules/citation-page: enable_globally, default false. true=on, false=off
     */
    protected Boolean citationEnabledGlobally = null;

    protected boolean isCitationEnabledGlobally() {
        return citationEnabledGlobally;
    }

    protected boolean isCitationEnabledThroughCollection(Context context, Bitstream bitstream) throws SQLException {
        //Reject quickly if no-enabled collections
        if (citationEnabledCollectionsList.isEmpty()) {
            return false;
        }

        DSpaceObject owningDSO = bitstreamService.getParentObject(context, bitstream);
        if (owningDSO instanceof Item) {
            Item item = (Item) owningDSO;

            List<Collection> collections = item.getCollections();

            for (Collection collection : collections) {
                if (citationEnabledCollectionsList.contains(collection.getHandle())) {
                    return true;
                }
            }
        }

        // If previous logic didn't return true, then we're false
        return false;
    }

    @Override
    public Boolean isCitationEnabledForBitstream(Bitstream bitstream, Context context) throws SQLException {
        if (isCitationEnabledGlobally() || isCitationEnabledThroughCollection(context, bitstream)) {

            boolean adminUser = authorizeService.isAdmin(context);

            if (!adminUser && canGenerateCitationVersion(context, bitstream)) {
                return true;
            }
        }

        // If previous logic didn't return true, then we're false.
        return false;
    }

    /**
     * Should the citation page be the first page of the document, or the last page?
     * default = true. true = first page, false = last page
     * citation_as_first_page=true
     */
    protected Boolean citationAsFirstPage = null;

    protected Boolean isCitationFirstPage() {
        if (citationAsFirstPage == null) {
            citationAsFirstPage = configurationService.getBooleanProperty("citation-page.citation_as_first_page", true);
        }

        return citationAsFirstPage;
    }

    @Override
    public boolean canGenerateCitationVersion(Context context, Bitstream bitstream) throws SQLException {
        return VALID_TYPES.contains(bitstream.getFormat(context).getMIMEType());
    }

    @Override
    public Pair<byte[], Long> makeCitedDocument(Context context, Bitstream bitstream)
            throws IOException, SQLException {

        try (
                var result = new PDDocument();
                var source = loadDocumentFromDB(context, bitstream)
        ) {
            var item = (Item) bitstreamService.getParentObject(context, bitstream);

            try (var cover = coverPageService.renderCoverDocument(item)) {
                addCoverPageToDocument(result, source, cover);

                return documentAsBytes(result);
            }
        }
    }

    private PDDocument loadDocumentFromDB(Context context, Bitstream bitstream) {
        try (var inputStream = bitstreamService.retrieve(context, bitstream)) {
            return Loader.loadPDF(new RandomAccessReadBuffer(inputStream));
        } catch (IOException | SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private static Pair<byte[], Long> documentAsBytes(PDDocument document) throws IOException {

        document.setAllSecurityToBeRemoved(true);

        //We already have the full PDF in memory, so keep it there
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.save(out);
            byte[] data = out.toByteArray();
            return Pair.of(data, (long) data.length);
        }
    }

    private void addCoverPageToDocument(PDDocument document, PDDocument sourceDocument, PDDocument coverPage) {
        var sourcePages = sourceDocument.getDocumentCatalog().getPages();
        var coverPages = coverPage.getDocumentCatalog().getPages();

        if (isCitationFirstPage()) {
            //citation as cover page

            for (var page: coverPages) {
                document.addPage(page);
            }

            for (PDPage sourcePage : sourcePages) {
                document.addPage(sourcePage);
            }
        } else {
            //citation as tail page
            for (PDPage sourcePage : sourcePages) {
                document.addPage(sourcePage);
            }

            for (var page: coverPages) {
                document.addPage(page);
            }
        }
    }
}
