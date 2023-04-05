/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * Make requests to the DOI registration angencies, f.e.to
 *  <a href='http://n2t.net/ezid/'>EZID</a> DOI service, and analyze the responses.
 * 
 * <p>
 * Use {@link org.dspace.identifier.ezid.EZIDRequestFactory#getInstance} to
 * configure an {@link org.dspace.identifier.ezid.EZIDRequest}
 * with your authority number and credentials.  {@code EZIDRequest} encapsulates
 * EZID's operations (lookup, create/mint, modify, delete...).
 * An operation returns an {@link org.dspace.identifier.ezid.EZIDResponse} which
 * gives easy access to EZID's status code and value, status of the underlying
 * HTTP request, and key/value pairs found in the response body (if any).
 * </p>
 */
package org.dspace.identifier.doi;
