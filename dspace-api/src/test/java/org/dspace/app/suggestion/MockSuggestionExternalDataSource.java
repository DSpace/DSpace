/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.binary.StringUtils;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MockSuggestionExternalDataSource extends AbstractExternalDataProvider {
    public static final String NAME = "suggestion";

    @Autowired
    private SuggestionService suggestionService;

    @Override
    public String getSourceIdentifier() {
        return NAME;
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        RequestService requestService = new DSpace().getRequestService();
        Request currentRequest = requestService.getCurrentRequest();
        Context context = (Context) currentRequest.getAttribute("dspace.context");
        Suggestion suggestion = suggestionService.findUnprocessedSuggestion(context, id);
        if (suggestion != null) {
            ExternalDataObject extDataObj = new ExternalDataObject(NAME);
            extDataObj.setDisplayValue(suggestion.getDisplay());
            extDataObj.setId(suggestion.getExternalSourceUri()
                    .substring(suggestion.getExternalSourceUri().lastIndexOf("/") + 1));
            extDataObj.setMetadata(suggestion.getMetadata());
            return Optional.of(extDataObj);
        }
        return null;
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        return null;
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equals(NAME, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        return 0;
    }

}
