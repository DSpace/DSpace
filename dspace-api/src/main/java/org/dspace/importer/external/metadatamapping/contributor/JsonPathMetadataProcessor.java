/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.Collection;

/**
 * Service interface class for processing json object.
 * The implementation of this class is responsible for all business logic calls
 * for extracting of values from json object.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public interface JsonPathMetadataProcessor {

    public Collection<String> processMetadata(String json);

}