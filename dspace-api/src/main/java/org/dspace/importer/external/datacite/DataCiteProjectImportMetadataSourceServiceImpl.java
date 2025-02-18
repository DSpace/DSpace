/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.datacite;

/**
 * Implements a data source for querying Datacite for specific for Project resourceTypes.
 * This inherits the methods of DataCiteImportMetadataSourceServiceImpl
 *
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 *
 */
public class DataCiteProjectImportMetadataSourceServiceImpl
    extends DataCiteImportMetadataSourceServiceImpl {

    @Override
    public String getImportSource() {
        return "dataciteProject";
    }
}
