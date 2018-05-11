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
 */

package org.dspace.identifier;
