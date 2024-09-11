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
 * See {@link TaskQueue} and its relatives, {@link Curation} and its relatives
 * for more on queued curation.
 *
 * <p>Tasks may also be attached to a workflow step, so that a set of tasks is
 * applied to each uninstalled Item which passes through that step.  See
 * {@link org.dspace.curate.service.XmlWorkflowCuratorService}.
 *
 * <p>A task may return to the Curator a status code, a final status message,
 * and an optional report character stream.
 *
 * <p>The {@link Reporter} classes absorb strings of text and preserve it in
 * various ways.  A Reporter is a simple {@link Appendable} and makes no
 * assumptions about e.g. whether a string represents a complete line.  If you
 * want your report formatted, insert appropriate newlines and other whitespace
 * as needed.  Your tasks can emit marked-up text if you wish, but the stock
 * Reporter implementations make no attempt to render it.
 *
 * <p>Tasks may be annotated to inform the Curator of special properties.  See
 * {@link Distributive}, {@link Mutative}, {@link Suspendable} etc.
 */
package org.dspace.curate;
