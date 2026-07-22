/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ror.service;

import org.dspace.importer.external.service.components.QuerySource;

/**
 * Service interface for importing organizational metadata from the ROR
 * (Research Organization Registry) API.
 *
 * <p>Extends {@link QuerySource} to provide query-based record retrieval
 * for ROR data.</p>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public interface RorImportMetadataSourceService extends QuerySource {
}
