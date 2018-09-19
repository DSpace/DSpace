
package org.dspace.curate;

/**
 * A marker interface needed to make curation reporter classes into plugins.
 *
 * @author mhwood
 */
public interface Reporter
        extends Appendable, AutoCloseable {
}
