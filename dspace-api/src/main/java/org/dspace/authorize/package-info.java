/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 * Represents permissions for access to DSpace content.
 *
 * <h2>Philosophy</h2>
 * DSpace's authorization system follows the classical "police state"
 * philosophy of security - the user can do nothing, unless it is
 * specifically allowed.  Those permissions are spelled out with
 * {@link ResourcePolicy} objects, stored in the {@code resourcepolicy} table
 * in the database.
 *
 * <h2>Policies are attached to Content</h2>
 * Resource Policies get assigned to all of the content objects in
 * DSpace - collections, communities, items, bundles, and bitstreams.
 * (Currently they are not attached to non-content objects such as
 * {@code EPerson} or {@code Group}.  But they could be, hence the name
 * {@code ResourcePolicy} instead of {@code ContentPolicy}.)
 *
 * <h2>Policies are tuples</h2>
 * Authorization is based on evaluating the tuple of (object, action, actor),
 * such as (ITEM, READ, EPerson John Smith) to check if the {@code EPerson}
 * "John Smith" can read an item.  {@code ResourcePolicy} objects are pretty
 * simple, describing a single instance of (object, action, actor).  If
 * multiple actors are desired, such as groups 10, 11, and 12 are allowed to
 * READ Item 13, you simply create a {@code ResourcePolicy} for each group.
 *
 * <h2>Built-in groups</h2>
 * The install process should create two built-in groups - {@code Anonymous}
 * for anonymous/public access, and {@code Administrators} for administrators.
 * Group {@code Anonymous} allows anyone access, even if not authenticated.
 * Group {@code Administrators}' members have super-user rights,
 * and are allowed to do any action to any object.
 *
 * <h2>Policy types
 * Policies have a "type" used to distinguish policies which are applied for
 * specific purposes.
 * <dl>
 * <dt>CUSTOM</dt>
 * <dd>These are created and assigned explicitly by users.</dd>
 * <dt>INHERITED</dt>
 * <dd>These are copied from a containing object's default policies.</dd>
 * <dt>SUBMISSION</dt>
 * <dd>These are applied during submission to give the submitter access while
 * composing a submission.</dd>
 * <dt>WORKFLOW</dt>
 * <dd>These are automatically applied during workflow, to give curators
 * access to submissions in their curation queues.  They usually have an
 * automatically-created workflow group as the actor.</dd>
 *
 * <h2>Start and End dates</h2>
 * A policy may have a start date and/or an end date.  The policy is
 * considered not valid before the start date or after the end date.  No date
 * means do not apply the related test.  For example, embargo until a given
 * date can be expressed by a READ policy with a given start date, and a
 * limited-time offer by a READ policy with a given end date.
 *
 * @author dstuve
 * @author mwood
 */
package org.dspace.authorize;
