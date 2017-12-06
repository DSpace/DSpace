/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.model;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class Citation {

    private CitationType type;
    private String citation;

    public Citation(CitationType type, String citation) {
        this.type = type;
        this.citation = citation;
    }

    public CitationType getType() {
        return type;
    }

    public void setType(CitationType type) {
        this.type = type;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    @Override
    public String toString() {
        return "Citation{" +
                "type=" + type +
                ", citation='" + citation + '\'' +
                '}';
    }
}
