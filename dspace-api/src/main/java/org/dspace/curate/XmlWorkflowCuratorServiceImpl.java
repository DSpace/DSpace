/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.curate.service.XmlWorkflowCuratorService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.workflow.CurationTaskConfig;
import org.dspace.workflow.FlowStep;
import org.dspace.workflow.Task;
import org.dspace.workflow.TaskSet;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.RoleMembers;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Manage interactions between curation and workflow.  A curation task can be
 * attached to a workflow step, to be executed during the step.
 *
 * <p>
 * <strong>NOTE:</strong> when run in workflow, curation tasks <em>run with
 * authorization disabled</em>.
 *
 * @see CurationTaskConfig
 * @author mwood
 */
@Service
public class XmlWorkflowCuratorServiceImpl
        implements XmlWorkflowCuratorService {
    private static final Logger LOG = LogManager.getLogger();

    @Autowired(required = true)
    protected XmlWorkflowFactory workflowFactory;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected GroupService groupService;

    @Autowired(required = true)
    protected EPersonService ePersonService;

    @Autowired(required = true)
    protected CollectionService collectionService;

    @Autowired(required = true)
    protected ClaimedTaskService claimedTaskService;

    @Autowired(required = true)
    protected CurationTaskConfig curationTaskConfig;

    @Autowired(required = true)
    protected XmlWorkflowService workflowService;

    @Autowired(required = true)
    protected XmlWorkflowItemService workflowItemService;

    /** A sink for curation task reports. */
    private final StringBuilder reporter = new StringBuilder();

    @Override
    public boolean needsCuration(Context c, XmlWorkflowItem wfi)
            throws SQLException, IOException {
        return getFlowStep(c, wfi) != null;
    }

    @Override
    public boolean doCuration(Context c, XmlWorkflowItem wfi)
            throws AuthorizeException, IOException, SQLException {
        Curator curator = new Curator();
        curator.setReporter(reporter);
        c.turnOffAuthorisationSystem();
        boolean wasAnonymous = false;
        if (null == c.getCurrentUser()) { // We need someone to email
            wasAnonymous = true;
            c.setCurrentUser(ePersonService.getSystemEPerson(c));
        }
        boolean failedP = curate(curator, c, wfi);
        if (wasAnonymous) {
            c.setCurrentUser(null);
        }
        c.restoreAuthSystemState();
        return failedP;
    }

    @Override
    public boolean curate(Curator curator, Context c, String wfId)
            throws AuthorizeException, IOException, SQLException {
        XmlWorkflowItem wfi = workflowItemService.find(c, Integer.parseInt(wfId));
        if (wfi != null) {
            return curate(curator, c, wfi);
        } else {
            LOG.warn(LogHelper.getHeader(c, "No workflow item found for id: {}", null), wfId);
        }
        return false;
    }

    @Override
    public boolean curate(Curator curator, Context c, XmlWorkflowItem wfi)
            throws AuthorizeException, IOException, SQLException {
        FlowStep step = getFlowStep(c, wfi);

        if (step != null) {
            // assign collection to item in case task needs it
            Item item = wfi.getItem();
            item.setOwningCollection(wfi.getCollection());
            for (Task task : step.tasks) {
                curator.addTask(task.name);
                // Check whether the task is configured to be queued rather than automatically run
                if (StringUtils.isNotEmpty(step.queue)) {
                    // queue attribute has been set in the FlowStep configuration: add task to configured queue
                    curator.queue(c, item.getID().toString(), step.queue);
                } else {
                    // Task is configured to be run automatically
                    curator.curate(c, item);
                    int status = curator.getStatus(task.name);
                    String result = curator.getResult(task.name);
                    String action = "none";
                    switch (status) {
                        case Curator.CURATE_FAIL:
                            // task failed - notify any contacts the task has assigned
                            if (task.powers.contains("reject")) {
                                action = "reject";
                            }
                            notifyContacts(c, wfi, task, "fail", action, result);
                            // if task so empowered, reject submission and terminate
                            if ("reject".equals(action)) {
                                workflowService.sendWorkflowItemBackSubmission(c, wfi,
                                        c.getCurrentUser(), null,
                                        task.name + ": " + result);
                                return false;
                            }
                            break;
                        case Curator.CURATE_SUCCESS:
                            if (task.powers.contains("approve")) {
                                action = "approve";
                            }
                            notifyContacts(c, wfi, task, "success", action, result);
                            if ("approve".equals(action)) {
                                // cease further task processing and advance submission
                                return true;
                            }
                            break;
                        case Curator.CURATE_ERROR:
                            notifyContacts(c, wfi, task, "error", action, result);
                            break;
                        default:
                            break;
                    }
                }
                curator.clear();
            }

            // Record any reporting done by the tasks.
            if (reporter.length() > 0) {
                LOG.info("Curation tasks over item {} for step {} report:%n{}",
                    () -> wfi.getItem().getID(),
                    () -> step.step,
                    () -> reporter.toString());
            }
        }
        return true;
    }

    /**
     * Find the flow step occupied by a work flow item.
     * @param c session context.
     * @param wfi the work flow item in question.
     * @return the current flow step for the item, or null.
     * @throws SQLException
     * @throws IOException
     */
    protected FlowStep getFlowStep(Context c, XmlWorkflowItem wfi)
            throws SQLException, IOException {
        if (claimedTaskService.find(c, wfi).isEmpty()) { // No claimed tasks:  assume first step
            Collection coll = wfi.getCollection();
            String taskSetName = curationTaskConfig.containsKey(coll.getHandle()) ?
                    coll.getHandle() : CurationTaskConfig.DEFAULT_TASKSET_NAME;
            TaskSet ts = curationTaskConfig.findTaskSet(taskSetName);
            return ts.steps.isEmpty() ? null : ts.steps.get(0);
        }
        ClaimedTask claimedTask
                = claimedTaskService.findByWorkflowIdAndEPerson(c, wfi, c.getCurrentUser());
        if (claimedTask != null) {
            Collection coll = wfi.getCollection();
            String taskSetName = curationTaskConfig.containsKey(coll.getHandle()) ?
                    coll.getHandle() : CurationTaskConfig.DEFAULT_TASKSET_NAME;
            TaskSet ts = curationTaskConfig.findTaskSet(taskSetName);
            for (FlowStep fstep : ts.steps) {
                if (fstep.step.equals(claimedTask.getStepID())) {
                    return fstep;
                }
            }
        }
        return null;
    }

    /**
     * Send email to people who should be notified when curation tasks are run.
     *
     * @param c session context.
     * @param wfi the work flow item being curated.
     * @param task the curation task being applied.
     * @param status status returned by the task.
     * @param action action to be taken as a result of task status.
     * @param message anything the code wants to say about the task run.
     * @throws AuthorizeException passed through.
     * @throws IOException passed through.
     * @throws SQLException passed through.
     */
    protected void notifyContacts(Context c, XmlWorkflowItem wfi,
            Task task,
            String status, String action, String message)
            throws AuthorizeException, IOException, SQLException {
        List<EPerson> epa = resolveContacts(c, task.getContacts(status), wfi);
        if (!epa.isEmpty()) {
            workflowService.notifyOfCuration(c, wfi, epa, task.name, action, message);
        } else {
            LOG.warn("No contacts were found for workflow item {}:  "
                    + "task {} returned action {} with message {}",
                    wfi.getID(), task.name, action, message);
        }
    }

    /**
     * Develop a list of EPerson from a list of perhaps symbolic "contact" names.
     *
     * @param c session context.
     * @param contacts the list of concrete and symbolic groups to resolve.
     * @param wfi the work flow item associated with these groups via its current work flow step.
     * @return the EPersons associated with the current state of {@code wfi}
     * @throws AuthorizeException passed through.
     * @throws IOException passed through.
     * @throws SQLException passed through.
     */
    protected List<EPerson> resolveContacts(Context c, List<String> contacts,
                                             XmlWorkflowItem wfi)
                    throws AuthorizeException, IOException, SQLException {
        List<EPerson> epList = new ArrayList<>();
        for (String contact : contacts) {
            // decode contacts
            if ("$flowgroup".equals(contact)) {
                // special literal for current flowgoup
                String stepID = getFlowStep(c, wfi).step;
                Step step;
                try {
                    Workflow workflow = workflowFactory.getWorkflow(wfi.getCollection());
                    step = workflow.getStep(stepID);
                } catch (WorkflowConfigurationException e) {
                    LOG.error("Failed to locate current workflow step for workflow item {}",
                            String.valueOf(wfi.getID()), e);
                    return epList;
                }
                Role role = step.getRole();
                if (null != role) {
                    RoleMembers roleMembers = role.getMembers(c, wfi);
                    for (EPerson ep : roleMembers.getEPersons()) {
                        epList.add(ep);
                    }
                    for (Group group : roleMembers.getGroups()) {
                        epList.addAll(group.getMembers());
                    }
                } else {
                    epList.add(ePersonService.getSystemEPerson(c));
                }
            } else if ("$colladmin".equals(contact)) {
                // special literal for collection administrators
                Group adGroup = wfi.getCollection().getAdministrators();
                if (adGroup != null) {
                    epList.addAll(groupService.allMembers(c, adGroup));
                }
            } else if ("$siteadmin".equals(contact)) {
                // special literal for site administrator
                EPerson siteEp = ePersonService.findByEmail(c,
                        configurationService.getProperty("mail.admin"));
                if (siteEp != null) {
                    epList.add(siteEp);
                }
            } else if (contact.indexOf("@") > 0) {
                // little shaky heuristic here - assume an eperson email name
                EPerson ep = ePersonService.findByEmail(c, contact);
                if (ep != null) {
                    epList.add(ep);
                }
            } else {
                // assume it is an arbitrary group name
                Group group = groupService.findByName(c, contact);
                if (group != null) {
                    epList.addAll(groupService.allMembers(c, group));
                }
            }
        }
        return epList;
    }
}
