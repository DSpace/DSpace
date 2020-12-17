/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.workflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * Represent the mapping between collection workflows and curation tasks.
 * This mapping is defined in {@code [DSpace]/config/workflow-curation.xml}.
 *
 * <p>Copied from {@link WorkflowCuratorServiceImpl}.
 *
 * @author mwood
 */
public class CurationTaskConfig
{
    private static final Map<String, TaskSet> tsMap = new HashMap<>();

    /**
     * Look up a TaskSet by name.
     *
     * @param setName name of the sought TaskSet:  collection handle or "default".
     * @return the named TaskSet, or the default TaskSet if not found, or
     *          {@code null} if there is no default either.
     * @throws IOException passed through.
     */
    static public TaskSet findTaskSet(String setName)
            throws IOException
    {
        if (tsMap.isEmpty())
        {
            ConfigurationService configurationService
                    = DSpaceServicesFactory.getInstance().getConfigurationService();
            File cfgFile = new File(configurationService.getProperty("dspace.dir") +
                                    File.separator + "config" + File.separator +
                                    "workflow-curation.xml");
            loadTaskConfig(cfgFile);
        }

        if (tsMap.containsKey(setName))
            return tsMap.get(setName);
        else
            return tsMap.get("default");
    }

    /**
     * Is this task set name defined?
     *
     * @param name name of the task set sought.
     * @return true if a set by that name is known.
     */
    public static boolean containsKey(String name)
    {
        return tsMap.containsKey(name);
    }

    @SuppressWarnings("null")
    static protected void loadTaskConfig(File cfgFile) throws IOException
    {
        final Map<String, String> collMap = new HashMap<>();
        final Map<String, TaskSet> setMap = new HashMap<>();
        TaskSet taskSet = null;
        FlowStep flowStep = null;
        Task task = null;
        String type = null;
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(
                                     new FileInputStream(cfgFile), "UTF-8");
            while (reader.hasNext())
            {
                switch (reader.next())
                {
                case START_ELEMENT:
                    {
                        String eName = reader.getLocalName();
                        if (null != eName) switch (eName)
                        {
                        case "mapping":
                            collMap.put(reader.getAttributeValue(null, "collection-handle"),
                                    reader.getAttributeValue(null, "taskset"));
                            break;
                        case "taskset":
                            taskSet = new TaskSet(reader.getAttributeValue(null, "name"));
                            break;
                        case "flowstep":
                            flowStep = new FlowStep(reader.getAttributeValue(null, "name"),
                                    reader.getAttributeValue(null, "queue"));
                            break;
                        case "task":
                            task = new Task(reader.getAttributeValue(null, "name"));
                            break;
                        case "workflow":
                            type = "power";
                            break;
                        case "notify":
                            type = reader.getAttributeValue(null, "on");
                            break;
                        default:
                            break;
                        }
                        break;
                    }
                case CHARACTERS:
                    if (task != null) {
                        if ("power".equals(type)) {
                            task.addPower(reader.getText());
                        } else {
                            task.addContact(type, reader.getText());
                        }
                    }
                    break;
                case END_ELEMENT:
                    {
                        String eName = reader.getLocalName();
                        if (null != eName) switch (eName)
                        {
                        case "task":
                            flowStep.addTask(task);
                            task = null;
                            break;
                        case "flowstep":
                            taskSet.addStep(flowStep);
                            break;
                        case "taskset":
                            setMap.put(taskSet.setName, taskSet);
                            break;
                        default:
                            break;
                        }
                        break;
                    }
                default:
                    break;
                }
            }
            reader.close();

            // stitch maps together
            for (Map.Entry<String, String> collEntry : collMap.entrySet())
            {
                if (! "none".equals(collEntry.getValue()) && setMap.containsKey(collEntry.getValue()))
                {
                    tsMap.put(collEntry.getKey(), setMap.get(collEntry.getValue()));
                }
            }
        } catch (XMLStreamException xsE) {
            throw new IOException(xsE.getMessage(), xsE);
        }
    }

    static public class TaskSet
    {
        public String setName = null;
        public List<FlowStep> steps = null;

        public TaskSet(String setName)
        {
            this.setName = setName;
            steps = new ArrayList<>();
        }

        public void addStep(FlowStep step)
        {
            steps.add(step);
        }
    }

    static public class FlowStep
    {
        public String step = null;
        public String queue = null;
        public List<Task> tasks = null;

        public FlowStep(String stepStr, String queueStr)
        {
            this.step = stepStr;
            this.queue = queueStr;
            tasks = new ArrayList<>();
        }

        public void addTask(Task task)
        {
            tasks.add(task);
        }
    }

    static public class Task
    {
        public String name = null;
        public List<String> powers = new ArrayList<>();
        public Map<String, List<String>> contacts = new HashMap<>();

        public Task(String name) { this.name = name; }

        public void addPower(String power) {
            powers.add(power);
        }

        public void addContact(String status, String contact)
        {
            List<String> sContacts = contacts.get(status);
            if (sContacts == null)
            {
                sContacts = new ArrayList<>();
                contacts.put(status, sContacts);
            }
            sContacts.add(contact);
        }

        public List<String> getContacts(String status)
        {
            List<String> ret = contacts.get(status);
            return (ret != null) ? ret : new ArrayList<String>();
        }
    }
}
