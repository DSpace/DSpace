/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.dspace.content.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * This service renders a PDF coverpage for an item.
 */
public class CoverPageService {

    private static final Logger LOG = LogManager.getLogger(CoverPageService.class);

    private final PdfGenerator pdfGenerator = new PdfGenerator();

    CoverPageService(
            @Value("${citation-page.cover-template:dspace_coverpage}")
            String coverTemplate,

            @Autowired(required = false)
            CoverPageContributor coverPageContributor
    ) {
        this.coverTemplate = coverTemplate;
        this.coverPageContributor = coverPageContributor == null ?
                new DefaultCoverPageContributor() :
                coverPageContributor;
    }

    private final String coverTemplate;

    private final CoverPageContributor coverPageContributor;

    /**
     * Render a PDF coverpage for the given Item. The implementation may use the context and
     * any relevant meta data from the Item to populate dynamic content in the rendered page.
     * All metadata fields (using format schema_field_qualifier) are passed to coverPageContributor
     * and can be referenced in HTML template
     *
     * @param item the current item
     * @return a PDDocument containing the rendered coverpage.
     * The caller is responsible to close the PDDocument after use!
     */
    public PDDocument renderCoverDocument(Item item) {

        var parameters = prepareParams(item);

        // The contributor allows to calculate additional parameters for the template or to replace existing ones.
        parameters = coverPageContributor.processCoverPageParams(item, parameters);

        LOG.debug("Rendering cover document with params = {}", parameters);

        var html = pdfGenerator.parseTemplate(coverTemplate, parameters);

        return pdfGenerator.generate(html);
    }

    protected Map<String, String> prepareParams(Item item) {
        return item.getMetadata().stream()
                .filter(meta -> meta.getPlace() == 0)
                .map(meta -> Map.entry(meta.getMetadataField().toString(), meta.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}