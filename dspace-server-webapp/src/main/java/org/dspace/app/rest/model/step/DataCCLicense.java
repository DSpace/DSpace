/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import org.dspace.app.rest.model.BitstreamRest;

/**
 * Java Bean to expose the section creativecommons representing the CC License during in progress submission.
 */
public class DataCCLicense implements SectionData {

    private String uri;

    private String rights;

    private BitstreamRest file;

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public String getRights() {
        return rights;
    }

    public void setRights(final String rights) {
        this.rights = rights;
    }

    public BitstreamRest getFile() {
        return file;
    }

    public void setFile(final BitstreamRest file) {
        this.file = file;
    }
}
