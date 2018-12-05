/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.curate;

/**
 * A marker interface needed to make curation reporter classes into plugins.
 *
 * @author mhwood
 */
public interface Reporter
        extends Appendable, AutoCloseable {
}
