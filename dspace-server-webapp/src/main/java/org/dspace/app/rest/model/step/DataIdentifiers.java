/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.IdentifierRest;
/**
 * Java bean with basic DOI / Handle / other identifier data for
 * display in submission step
 *
 * @author Kim Shepherd (kim@shepherd.nz)
 */
public class DataIdentifiers implements SectionData {
    // Map of identifier types and values
    List<IdentifierRest> identifiers;
    // Types to display, a hint for te UI
    List<String> displayTypes;

    public DataIdentifiers() {
        identifiers = new ArrayList<>();
        displayTypes = new ArrayList<>();
    }

    public List<IdentifierRest> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<IdentifierRest> identifiers) {
        this.identifiers = identifiers;
    }

    public void addIdentifier(String type, String value, String status) {
        IdentifierRest identifier = new IdentifierRest();
        identifier.setValue(value);
        identifier.setIdentifierType(type);
        identifier.setIdentifierStatus(status);
        this.identifiers.add(identifier);
    }

    public List<String> getDisplayTypes() {
        return displayTypes;
    }

    public void setDisplayTypes(List<String> displayTypes) {
        this.displayTypes = displayTypes;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (IdentifierRest identifier : identifiers) {
            sb.append(identifier.getType()).append(": ").append(identifier.getValue()).append("\n");
        }
        return sb.toString();
    }

}
