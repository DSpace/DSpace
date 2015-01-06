/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

/**
 * DSpace's own internal object identifiers.  These are explicitly not meaningful
 * outside of a specific instance of DSpace.  They are persistent (unlike, for
 * example, database record IDs, which can change if an instance's content is
 * exported and imported).
 *
 * @author mwood
 */
public class DSpace
        implements Identifier
{
    // This space intentionally left blank.
}
