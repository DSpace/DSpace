/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * <p>
 * Embargo allows the deposit of Items whose content should not be made visible
 * until later.  Some journals, for example, permit self-publication after a
 * period of exclusive access through the journal.
 * </p>
 * <p>
 * Embargo policy is applied through a pair of pluggable classes:  an
 * {@link org.dspace.embargo.EmbargoSetter} and an
 * {@link org.dspace.embargo.EmbargoLifter}.  The {@link org.dspace.embargo.EmbargoServiceImpl}
 * must be configured to specify these classes, as well as names of two metadata
 * fields for use by the embargo facility:  an embargo lift date (when the
 * content will be released) and the embargo terms (which the EmbargoSetter will
 * use to calculate the lift date).  You must select or create appropriate
 * metadata fields for this purpose.
 * </p>
 * <p>
 * See {@link org.dspace.embargo.DefaultEmbargoSetter},
 * {@link org.dspace.embargo.DayTableEmbargoSetter}, and
 * {@link org.dspace.embargo.DefaultEmbargoLifter} for simple policy classes
 * which ship with DSpace.  You can supply your own classes to implement more
 * elaborate policies.
 * </p>
 * <p>
 * Embargo is applied when an Item is installed in a Collection.  An Item subject
 * to embargo passes through several stages:
 * </p>
 * <ol>
 *  <li>During submission, the metadata field established for embargo terms must
 *  be set to a value which is interpretable by the selected setter.  Typically
 *  this will be a date or an interval.  There is no specific mechanism for
 *  requesting embargo; you must customize your submission forms as needed,
 *  create a template Item which applies a standard value, or in some other way
 *  cause the specified metadata field to be set.
 *  </li>
 *  <li>When the Item is accepted into a Collection, the setter will apply the
 *  embargo, making the content inaccessible.
 *  </li>
 *  <li>The site should run the embargo lifter tool ({@code dspace embargo-lifter})
 *  from time to time, for example using an automatic daily job.  This discovers
 *  Items which have passed their embargo lift dates and makes their content
 *  accessible.
 *  </li>
 * </ol>
 */

package org.dspace.embargo;
