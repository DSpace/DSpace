/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.List;

/**
 * Java bean with basic DOI / Handle / other identifier data for
 * display in submission step
 *
 * @author Kim Shepherd (kim@shepherd.nz)
 */
public class DataIdentifiers implements SectionData {
    String handle;
    String doi;
    List<String> otherIdentifiers;
    // Types to display, a hint for te UI
    List<String> displayTypes;

    public DataIdentifiers() {

    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public List<String> getOtherIdentifiers() {
        return otherIdentifiers;
    }

    public void setOtherIdentifiers(List<String> otherIdentifiers) {
        this.otherIdentifiers = otherIdentifiers;
    }

    public List<String> getDisplayTypes() {
        return displayTypes;
    }

    public void setDisplayTypes(List<String> displayTypes) {
        this.displayTypes = displayTypes;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Handle: ").append(handle);
        sb.append("DOI: ").append(doi);
        sb.append("Others: ").append(String.join(", ", otherIdentifiers));
        return sb.toString();
    }

}
