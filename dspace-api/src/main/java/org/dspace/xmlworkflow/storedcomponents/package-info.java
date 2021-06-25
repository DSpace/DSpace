/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * The state of an item in a workflow is persisted in a number of objects.
 * 
 * <p>An {@link XmlWorkflowItem} represents the Item which it wraps, until the
 * Item is installed in a Collection at the conclusion of the workflow.
 * 
 * <p>A {@link PoolTask} represents a unit of work to be performed on a workflow
 * item by one or more members of a {@link org.dspace.xmlworkflow.Role Role}.
 * The item is given PoolTasks when it enters a workflow step.  Role members
 * may claim the tasks.
 * 
 * <p>A {@link ClaimedTask} represents a task that has been claimed by a Role
 * member for execution.  More than one member can claim a task if it requires
 * more than one (for example, if two approvals are required).
 */
package org.dspace.xmlworkflow.storedcomponents;
