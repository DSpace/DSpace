/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction.grobid.client;

import java.io.InputStream;
import java.util.Optional;

import org.w3c.dom.Document;

/**
 * A GROBID client should return a valid {@link org.w3c.dom.Document} representing the TEI response header,
 * in which extracted PDF metadata is contained.
 *
 * The GROBID spec also supports several other formats, e.g. BibTeX, but this is currently unsupported.
 *
 * @author Kim Shepherd
 *
 */
public interface GrobidClient {

    /**
     * POST a PDF input stream to a GROBID service and validate and return the TEI header
     * as a standard {@link org.w3c.dom.Document}
     *
     * @param  inputStream the PDF document
     * @return the DOM document
     */
    Optional<Document> retrieveHeaderDocument(InputStream inputStream) throws GrobidClientException;

    /**
     * POST a PDF input stream to a GROBID service and validate and return the consolidated TEI header
     * as a standard {@link org.w3c.dom.Document}
     *
     * @param  inputStream       the PDF document
     * @param  consolidateHeader the consolidate header parameter
     * @return the document in TEI XML format
     */
    Optional<Document> retrieveHeaderDocument(InputStream inputStream, ConsolidateHeaderEnum consolidateHeader)
            throws GrobidClientException;

}
