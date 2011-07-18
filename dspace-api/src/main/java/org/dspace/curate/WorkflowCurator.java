/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import org.dspace.content.Item;
import java.util.Arrays;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

// Warning - static import ahead!
import static javax.xml.stream.XMLStreamConstants.*;

/**
 * WorkflowCurator manages interactions between curation and workflow.
 * Specifically, it is invoked in WorkflowManager to allow the
 * performance of curation tasks during workflow.
 * 
 * @author richardrodgers
 */
public class WorkflowCurator {
    
      /** log4j logger */
    private static Logger log = Logger.getLogger(WorkflowCurator.class);
    
    private static File cfgFile = new File(ConfigurationManager.getProperty("dspace.dir") +
                                    File.separator + "config" + File.separator +
                                    "workflow-curation.xml");
    
    private static Map<String, TaskSet> tsMap = new HashMap<String, TaskSet>();
    
    private static final String[] flowSteps = { "step1", "step2", "step3", "archive" };
    
    static {
        try {
            loadTaskConfig();
        } catch (IOException e) {
            // debug e.printStackTrace();
            log.fatal("Unable to load config: " + cfgFile.getAbsolutePath());
        }
    }
    
    public static boolean needsCuration(WorkflowItem wfi) {
       return getFlowStep(wfi) != null; 
    }
  
    /**
     * Determines and executes curation on a Workflow item.
     * 
     * @param c the context
     * @param wfi the workflow item
     * @return true if curation was completed or not required,
     *         false if tasks were queued for later completion,
     *         or item was rejected
     * @throws AuthorizeException
     * @throws IOException
     * @throws SQLException
     */
    public static boolean doCuration(Context c, WorkflowItem wfi)
            throws AuthorizeException, IOException, SQLException {
        FlowStep step = getFlowStep(wfi);
        if (step != null) {
            Curator curator = new Curator();
            // are we going to perform, or just put on queue?
            if (step.queue != null) {
                for (Task task : step.tasks) {
                    curator.addTask(task.name);
                }
                curator.queue(c, String.valueOf(wfi.getID()), step.queue);
                wfi.update();
                return false;
            } else {
                return curate(curator, c, wfi);
            }
        }
        return true;
    }
    
    /**
     * Determines and executes curation of a Workflow item.
     * 
     * @param c the user context
     * @param wfId the workflow id
     * @throws AuthorizeException
     * @throws IOException
     * @throws SQLException
     */
    public static boolean curate(Curator curator, Context c, String wfId)
            throws AuthorizeException, IOException, SQLException {
        WorkflowItem wfi = WorkflowItem.find(c, Integer.parseInt(wfId));
        if (wfi != null) {
            if (curate(curator, c, wfi)) {
                WorkflowManager.advance(c, wfi, c.getCurrentUser(), false, true);
                return true;
            }
        } else {
            log.warn(LogManager.getHeader(c, "No workflow item found for id: " + wfId, null));
        }
        return false;
    }
    
    public static boolean curate(Curator curator, Context c, WorkflowItem wfi) 
            throws AuthorizeException, IOException, SQLException {
        FlowStep step = getFlowStep(wfi);
        if (step != null) {
            // assign collection to item in case task needs it
            Item item = wfi.getItem();
            item.setOwningCollection(wfi.getCollection());
            for (Task task : step.tasks) {
                curator.addTask(task.name);
                curator.curate(item);
                int status = curator.getStatus(task.name);
                String result = curator.getResult(task.name);
                String action = "none";
                if (status == Curator.CURATE_FAIL) {
                    // task failed - notify any contacts the task has assigned
                    if (task.powers.contains("reject")) {
                        action = "reject";
                    }
                    notifyContacts(c, wfi, task, "fail", action, result);
                    // if task so empowered, reject submission and terminate
                    if ("reject".equals(action)) {
                        WorkflowManager.reject(c, wfi, c.getCurrentUser(),
                                                task.name + ": " + result);
                        return false;
                    }
                } else if (status == Curator.CURATE_SUCCESS) {
                    if (task.powers.contains("approve")) {
                        action = "approve";
                    }
                    notifyContacts(c, wfi, task, "success", action, result);
                    if ("approve".equals(action)) {
                        // cease further task processing and advance submission
                        return true;
                    }
                } else if (status == Curator.CURATE_ERROR) {
                    notifyContacts(c, wfi, task, "error", action, result);
                }
                curator.clear();
            }
        }
        return true;
    }
    
    private static void notifyContacts(Context c, WorkflowItem wfi, Task task,
                                       String status, String action, String message)
            throws AuthorizeException, IOException, SQLException  {
        EPerson[] epa = resolveContacts(c, task.getContacts(status), wfi);
        if (epa.length > 0) {
            WorkflowManager.notifyOfCuration(c, wfi, epa, task.name, action, message);
        }
    }
    
    private static EPerson[] resolveContacts(Context c, List<String> contacts,
                                             WorkflowItem wfi) 
                    throws AuthorizeException, IOException, SQLException {
        List<EPerson> epList = new ArrayList<EPerson>();
        for (String contact : contacts) {
            // decode contacts
            if ("$flowgroup".equals(contact)) {
                // special literal for current flowgoup
                int step = state2step(wfi.getState());
                // make sure this step exists
                if (step < 4) {
                    Group wfGroup = wfi.getCollection().getWorkflowGroup(step);
                    if (wfGroup != null) {
                        epList.addAll(Arrays.asList(Group.allMembers(c, wfGroup)));
                    }
                }
            } else if ("$colladmin".equals(contact)) {
                Group adGroup = wfi.getCollection().getAdministrators();
                if (adGroup != null) {
                    epList.addAll(Arrays.asList(Group.allMembers(c, adGroup)));
                }
            } else if ("$siteadmin".equals(contact)) {
                EPerson siteEp = EPerson.findByEmail(c, 
                        ConfigurationManager.getProperty("mail.admin"));
                if (siteEp != null) {
                    epList.add(siteEp);
                }
            } else if (contact.indexOf("@") > 0) {
                // little shaky heuristic here - assume an eperson email name
                EPerson ep = EPerson.findByEmail(c, contact);
                if (ep != null) {
                    epList.add(ep);
                }
            } else {
                // assume it is an arbitrary group name
                Group group = Group.findByName(c, contact);
                if (group != null) {
                    epList.addAll(Arrays.asList(Group.allMembers(c, group)));
                } 
            }
        }
        return epList.toArray(new EPerson[epList.size()]);
    }
    
    private static FlowStep getFlowStep(WorkflowItem wfi) {
        Collection coll = wfi.getCollection();
        String key = tsMap.containsKey(coll.getHandle()) ? coll.getHandle() : "default";
        TaskSet ts = tsMap.get(key);
        if (ts != null) {
            int myStep = state2step(wfi.getState());
            for (FlowStep fstep : ts.steps) {
                if (fstep.step == myStep) {
                    return fstep;
                }
            }
        }
        return null;
    }
    
    private static int state2step(int state) {
        if (state <= WorkflowManager.WFSTATE_STEP1POOL)
        {
            return 1;
        }
        if (state <= WorkflowManager.WFSTATE_STEP2POOL)
        {
            return 2;
        }
        if (state <= WorkflowManager.WFSTATE_STEP3POOL)
        {
            return 3;
        }
        return 4;
    }
    
    private static int stepName2step(String name) {
        for (int i = 0; i < flowSteps.length; i++) {
            if (flowSteps[i].equals(name)) {
                return i + 1;
            }
        }
        // invalid stepName - log
        log.warn("Invalid step: '" + name + "' provided");
        return -1;
    }
    
    private static void loadTaskConfig() throws IOException {
        Map<String, String> collMap = new HashMap<String, String>();
        Map<String, TaskSet> setMap = new HashMap<String, TaskSet>();
        TaskSet taskSet = null;
        FlowStep flowStep = null;
        Task task = null;
        String type = null;
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(
                                     new FileInputStream(cfgFile), "UTF-8");
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == START_ELEMENT) {
                    String eName = reader.getLocalName();
                    if ("mapping".equals(eName)) {
                        collMap.put(reader.getAttributeValue(0),
                                    reader.getAttributeValue(1));
                    } else if ("taskset".equals(eName)) {
                        taskSet = new TaskSet(reader.getAttributeValue(0));
                    } else if ("flowstep".equals(eName)) {
                        int count = reader.getAttributeCount();
                        String queue = (count == 2) ?
                                       reader.getAttributeValue(1) : null;
                        flowStep = new FlowStep(reader.getAttributeValue(0), queue);       
                    } else if ("task".equals(eName)) {
                        task = new Task(reader.getAttributeValue(0));
                    } else if ("workflow".equals(eName)) {
                        type = "power";
                    } else if ("notify".equals(eName)) {
                        type = reader.getAttributeValue(0);
                    }
                } else if (event == CHARACTERS) {
                    if (task != null) {
                        if ("power".equals(type)) {
                            task.addPower(reader.getText()); 
                        } else {
                            task.addContact(type, reader.getText());
                        }
                    }
                } else if (event == END_ELEMENT) {
                    String eName = reader.getLocalName();
                    if ("task".equals(eName)) {
                        flowStep.addTask(task);
                        task = null;
                    } else if ("flowstep".equals(eName)) {
                        taskSet.addStep(flowStep);
                    } else if ("taskset".equals(eName)) {
                        setMap.put(taskSet.setName, taskSet);
                    }
                }
            }
            reader.close();
            // stitch maps together
            for (Map.Entry<String, String> collEntry : collMap.entrySet()) {
                if (! "none".equals(collEntry.getValue()) && setMap.containsKey(collEntry.getValue())) {
                    tsMap.put(collEntry.getKey(), setMap.get(collEntry.getValue()));
                }
            }
        } catch (XMLStreamException xsE) {
            throw new IOException(xsE.getMessage(), xsE);
        }
    }
    
    private static class TaskSet {
        public String setName = null;
        public List<FlowStep> steps = null;
        
        public TaskSet(String setName) {
            this.setName = setName;
            steps = new ArrayList<FlowStep>();
        }
        
        public void addStep(FlowStep step) {
            steps.add(step);
        }
    }
    
    private static class FlowStep {
        public int step = -1;
        public String queue = null;
        public List<Task> tasks = null;
        
        public FlowStep(String stepStr, String queueStr) {
            this.step = stepName2step(stepStr);
            this.queue = queueStr;
            tasks = new ArrayList<Task>();
        }
        
        public void addTask(Task task) {
            tasks.add(task);
        }
    }
    
    private static class Task {
        public String name = null;
        public List<String> powers = new ArrayList<String>();
        public Map<String, List<String>> contacts = new HashMap<String, List<String>>();
        
        public Task(String name) {
            this.name = name;
        }
        
        public void addPower(String power) {
            powers.add(power);
        }
        
        public void addContact(String status, String contact) {
            List<String> sContacts = contacts.get(status);
            if (sContacts == null) {
                sContacts = new ArrayList<String>();
                contacts.put(status, sContacts);
            }  
            sContacts.add(contact);
        }
        
        public List<String> getContacts(String status) {
            List<String> ret = contacts.get(status);
            return (ret != null) ? ret : new ArrayList<String>();
        }
    }
}
