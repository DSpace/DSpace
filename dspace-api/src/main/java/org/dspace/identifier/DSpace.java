/*
 * Copyright 2013 Indiana University.  All rights reserved.
 *
 * Mark H. Wood, IUPUI University Library, Nov 11, 2013
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
