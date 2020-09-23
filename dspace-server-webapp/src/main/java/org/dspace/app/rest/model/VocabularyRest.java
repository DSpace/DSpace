/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

/**
 * The vocabulary REST resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
    @LinkRest(name = VocabularyRest.ENTRIES,
            method = "filter"
    ),
})
public class VocabularyRest extends BaseObjectRest<String> {

    public static final String NAME = "vocabulary";
    public static final String CATEGORY = RestAddressableModel.SUBMISSION;
    public static final String ENTRIES = "entries";

    private String name;

    private boolean scrollable;

    private boolean hierarchical;

    private Integer preloadLevel;

    @Override
    public String getId() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isScrollable() {
        return scrollable;
    }

    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }

    public boolean isHierarchical() {
        return hierarchical;
    }

    public void setHierarchical(boolean hierarchical) {
        this.hierarchical = hierarchical;
    }

    public Integer getPreloadLevel() {
        return preloadLevel;
    }

    public void setPreloadLevel(Integer preloadLevel) {
        this.preloadLevel = preloadLevel;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }
}
