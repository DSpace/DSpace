package org.dspace.app.rest.model;

import org.dspace.disseminate.CSLBibliography;
import org.dspace.disseminate.CSLBibliographyGenerator;

import java.util.ArrayList;
import java.util.List;

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
