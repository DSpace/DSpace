/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;

public class MockSolrSuggestionProvider extends SolrSuggestionProvider {

    @Override
    protected boolean isExternalDataObjectPotentiallySuggested(Context context, ExternalDataObject externalDataObject) {
        return StringUtils.equals(MockSuggestionExternalDataSource.NAME, externalDataObject.getSource());
    }
}
