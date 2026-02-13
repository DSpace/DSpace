/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction.grobid.client;

import java.io.InputStream;

import org.dspace.submit.extraction.grobid.TEI;

/**
 * GROBID client.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface GrobidClient {

    /**
     * Extract the header of the input PDF document, normalize it and convert it
     * into a TEI XML format.
     *
     * @param  inputStream the PDF document
     * @return             the document in TEI XML format
     */
    TEI processHeaderDocument(InputStream inputStream);

    /**
     * Extract the header of the input PDF document, normalize it and convert it
     * into a TEI XML format.
     *
     * @param  inputStream       the PDF document
     * @param  consolidateHeader the consolidate header parameter
     * @return                   the document in TEI XML format
     */
    TEI processHeaderDocument(InputStream inputStream, ConsolidateHeaderEnum consolidateHeader);


}
