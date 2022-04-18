/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 * In DSpace, "curation" refers to the application of one or more "tasks" to one
 * or more model objects.  There are two fundamental classes to consider:
 *
 * <dl>
 *   <dt>{@link CurationTask}</dt>
 *   <dd>Code to be applied to model objects.  A task is invoked on a single
 *       object, and may analyze and/or modify it as required.</dd>
 *   <dt>{@link Curator}</dt>
 *   <dd>Applies tasks to model objects as requested.  See this class for
 *       details of the order of application of tasks to objects.</dd>
 * </dl>
 *
 * <p>Curation requests may be run immediately or queued for batch processing.
 *
 * <p>Tasks may also be attached to a workflow step, so that a set of tasks is
 * applied to each uninstalled Item which passes through that step.  See
 * {@link org.dspace.curate.service.XmlWorkflowCuratorService}.
 *
 * <p>A task may return to the Curator a status code, a final status message,
 * and an optional report character stream.
 */
package org.dspace.curate;
