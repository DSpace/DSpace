/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;

import org.dspace.disseminate.CSLBibliography;


public class BibliographyRest extends BaseObjectRest<String> {

    public static final String NAME = "bibliography";
    public static final String PLURAL_NAME = "bibliographies";

    private final List<CSLBibliography> bibliographies;

    public BibliographyRest() {
        bibliographies = new ArrayList<>();
    }

    public List<CSLBibliography> getBibliographies() {
        return bibliographies;
    }

    public void addBibliography(CSLBibliography bibliography) {
        bibliographies.add(bibliography);
    }

    public void addBibliographies(List<CSLBibliography> bibliographies) {
        this.bibliographies.addAll(bibliographies);
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public Class getController() {
        return null;
    }
}
