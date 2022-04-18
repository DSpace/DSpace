/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

/**
 * Represent the mapping between collection workflows and curation tasks.
 * This mapping is configured in {@code [DSpace]/config/workflow-curation.xml}.
 * Meant to be used as a singleton injected by a DI container such as Spring.
 *
 * <p>Adapted from {@link org.dspace.curate.WorkflowCuratorServiceImpl}.
 *
 * @author mwood
 */
public class CurationTaskConfig {
    /** Name of the TaskSet that matches an unconfigured {@link TaskSet} name. */
    public static final String DEFAULT_TASKSET_NAME = "default";

    private final Map<String, TaskSet> collectionTasksetMap;

    /**
     * @param configurationDocument the external representation of the workflow
     *                              curation configuration.
     * @throws IOException if the configuration file cannot be opened.
     * @throws JAXBException passed through:  configuration syntax or semantic error.
     * @throws SAXException passed through:  configuration lexical error.
     */
    public CurationTaskConfig(InputStream configurationDocument)
            throws JAXBException, SAXException, IOException {
        collectionTasksetMap = loadTaskConfig(configurationDocument);
    }

    /**
     * Find a TaskSet by name.
     *
     * @param setName name of the sought TaskSet:  collection handle or "default".
     * @return the named TaskSet, or the default TaskSet if not found, or
     *          an empty TaskSet (zero steps) if there is no default either.
     */
    @NotNull
    public TaskSet findTaskSet(@NotNull String setName) {
        if (collectionTasksetMap.containsKey(setName)) {
            return collectionTasksetMap.get(setName);
        } else if (collectionTasksetMap.containsKey(DEFAULT_TASKSET_NAME)) {
            return collectionTasksetMap.get(DEFAULT_TASKSET_NAME);
        } else {
            return new TaskSet("", Collections.EMPTY_LIST);
        }
    }

    /**
     * Is this task set name defined?
     *
     * @param name name of the task set sought.
     * @return true if a set by that name is known.
     */
    public boolean containsKey(@NotNull String name) {
        return collectionTasksetMap.containsKey(name);
    }

    /**
     * Read an XML task configuration and digest it into mappings among
     * Collections, task sets, flow steps, and tasks.
     *
     * @param configuration XML document containing the configuration.
     * @throws IOException if the configuration cannot be opened or read.
     * @throws JAXBException on workflow-curation syntactic or semantic error.
     * @throws SAXException on XML lexical error.
     */
    private Map<String, TaskSet> loadTaskConfig(InputStream configuration)
            throws JAXBException, SAXException, IOException {
        Map<String, TaskSet> workflowConfiguration = new HashMap<>();

        // Instantiate a parser for XML workflow-curation configurations.
        JAXBContext jaxbContext = JAXBContext.newInstance(WorkflowCuration.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        // Give our configuration schema to the parser.
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(CurationTaskConfig.class.getResource("workflow-curation.xsd"));
        unmarshaller.setSchema(schema);

        // Parse the configuration.
        WorkflowCuration workflows;
        try (Reader configurationReader
                = new InputStreamReader(configuration, StandardCharsets.UTF_8)) {
            workflows = (WorkflowCuration) unmarshaller.unmarshal(configurationReader);
        }

        // Build the mapping from Collection Handle to TaskSet.
        for (MappingType mapping : workflows.getTasksetMap().getMapping()) {
            List<FlowStep> flowsteps = new ArrayList<>(mapping.getTaskset().getFlowstep().size());
            for (FlowstepType step : mapping.getTaskset().getFlowstep()) {
                FlowStep flowstep = new FlowStep(step.getName(), step.getQueue());
                for (TaskType task : step.getTask()) {
                    Task stepTask = new Task(task.getName());
                    for (Object thing : task.getWorkflowOrNotify()) {
                        if (thing instanceof WorkflowType) {
                            WorkflowType action = (WorkflowType) thing;
                            stepTask.addPower(action.getValue().value());
                        } else if (thing instanceof NotifyType) {
                            NotifyType notify = (NotifyType) thing;
                            stepTask.addContact(notify.getOn().value(), notify.getValue());
                        } else {
                            // SNH this branch is forbidden by the schema.
                        }
                    }
                    flowstep.addTask(stepTask);
                }
                flowsteps.add(flowstep);
            }

            workflowConfiguration.put(mapping.getCollectionHandle(),
                    new TaskSet(mapping.getTaskset().getName(), flowsteps));
        }

        return workflowConfiguration;
    }
}
