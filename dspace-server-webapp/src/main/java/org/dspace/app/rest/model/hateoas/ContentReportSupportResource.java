/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ContentReportSupportRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

@RelNameDSpaceResource(ContentReportSupportRest.NAME)
public class ContentReportSupportResource extends HALResource<ContentReportSupportRest> {
    public ContentReportSupportResource(ContentReportSupportRest content) {
        super(content);
    }
}
