/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 * DSpace has a simple workflow system, which models the workflows
 * as named steps:  SUBMIT, arbitrary named steps that you define, and ARCHIVE.
 * When an item is submitted to DSpace, it is in the SUBMIT state.  If there
 * are no intermediate states defined, then it proceeds directly to ARCHIVE and
 * is put into the main DSpace archive.  Otherwise it advances through a list of
 * steps until it is either rejected back to the submitter's workspace or
 * accepted (installed in the archive).
 * 
 * <p>
 * A submitted Item is wrapped by a {@link WorkflowItem} which carries
 * information specific to the submission (such as the target Collection).
 *
 * <p>
 * EPerson groups may be assigned to the intermediate steps, where they are
 * expected to act on the item at those steps.  For example, if a Collection's
 * owners desire a review step, they would create a Group of reviewers, and
 * assign that Group to a step having a review action.  The members of that
 * step's Group will receive emails asking them to review the submission, and
 * will need to perform an action on the item before it can be rejected
 * back to the submitter or advanced to the next step.
 * 
 * <p>
 * {@link org.dspace.curate.CurationTask Curation Tasks} can be attached to a
 * workflow step so that an Item entering that step is processed by one or more
 * Curation Tasks.  See {@link org.dspace.curate.service.XmlWorkflowCuratorService}.
 * 
 * @author dstuve
 */
package org.dspace.workflow;
