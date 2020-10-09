/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Map;
import java.util.UUID;

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
public class SuggestionTargetRest extends BaseObjectRest<UUID> {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "suggestiontarget";
    public static final String TARGET = "target";
    public static final String CATEGORY = RestAddressableModel.INTEGRATION;

    private String display;
    private Map<String, Integer> totals;

    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getType() {
        return NAME;
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

    public Map<String, Integer> getTotals() {
        return totals;
    }

    public void setTotals(Map<String, Integer> totals) {
        this.totals = totals;
    }
}
