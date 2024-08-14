/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.dspace.app.rest.RestResourceController;

/**
 * The Suggestion Target REST Resource. A suggestion target is a Person to whom
 * one or more suggester sources have found related objects to be importe in the
 * system.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
        @LinkRest(name = SuggestionTargetRest.TARGET, method = "getTarget")
})
public class SuggestionTargetRest extends BaseObjectRest<String> {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "suggestiontarget";
    public static final String PLURAL_NAME = "suggestiontargets";
    public static final String TARGET = "target";
    public static final String CATEGORY = RestAddressableModel.INTEGRATION;

    private String display;
    private String source;
    private int total;

    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
