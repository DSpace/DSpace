/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * <p>
 * Access status allows the users to view the bitstreams availability before
 * browsing into the item itself.
 * </p>
 * <p>
 * The access status is calculated through a pluggable class:
 * {@link org.dspace.access.status.AccessStatusHelper}.
 * The {@link org.dspace.access.status.AccessStatusServiceImpl}
 * must be configured to specify this class, as well as a forever embargo date
 * threshold year, month and day.
 * </p>
 * <p>
 * See {@link org.dspace.access.status.DefaultAccessStatusHelper} for a simple calculation
 * based on the primary or the first bitstream of the original bundle. You can
 * supply your own class to implement more complex access statuses.
 * </p>
 * <p>
 * For now, the access status is calculated when the item is shown in a list.
 * </p>
 */

package org.dspace.access.status;
