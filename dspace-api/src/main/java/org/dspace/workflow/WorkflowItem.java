/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;


import org.dspace.content.InProgressSubmission;

/**
 * Interface representing a workflowitem, each workflowItem implementation must implement this interface.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface WorkflowItem extends InProgressSubmission {

}
