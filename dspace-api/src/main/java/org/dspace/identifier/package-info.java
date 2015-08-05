/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * Providers of durable unique identifiers (Handles, DOIs, etc.).
 * Generally, subclasses of {@link org.dspace.identifier.IdentifierProvider}
 * offer methods to create, delete, and resolve subclasses of
 * {@link org.dspace.identifier.Identifier}.  Classes outside this package
 * should rely on {@link org.dspace.identifier.service.IdentifierService} to perform
 * these operations using the most appropriate provider.
 *
 * <p>
 * Providers which store the generated identifiers as metadata on the identified
 * objects should use {@link org.dspace.identifier.IdentifierProvider#URI_METADATA_SCHEMA},
 * {@link org.dspace.identifier.IdentifierProvider#URI_METADATA_ELEMENT}, and
 * {@link org.dspace.identifier.IdentifierProvider#URI_METADATA_QUALIFIER} to
 * specify the metadata field.
 * </p>
 */

package org.dspace.identifier;
