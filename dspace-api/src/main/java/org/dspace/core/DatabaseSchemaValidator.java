/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

/**
 * Interface to validate the current domain model against the database schema.
 */
public interface DatabaseSchemaValidator {
    /**
     * Validate the model/schema mapping.
     * @return description of the invalidity of the mapping, or the empty string if valid.
     */
    String getDatabaseSchemaValidationError();

}
