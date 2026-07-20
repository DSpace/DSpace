/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction.grobid.client;

/**
 * Model the consolidateHeader parameter of {@link GrobidClient}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum ConsolidateHeaderEnum {

    /**
     * No consolidation at all is performed: all the metadata will come from the
     * source PDF.
     */
    NO_CONSOLIDATION("0"),

    /**
     * Consolidation against CrossRef and update of metadata: when we have a DOI
     * match, the publisher metadata are combined with the metadata extracted from
     * the PDF, possibly correcting them.
     */
    CONSOLIDATE_AND_INJECT_METADATA("1"),

    /**
     * Consolidation against CrossRef and, if matching, addition of the DOI only.
     */
    CONSOLIDATE_AND_INJECT_DOI("2");

    private String value;

    private ConsolidateHeaderEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
