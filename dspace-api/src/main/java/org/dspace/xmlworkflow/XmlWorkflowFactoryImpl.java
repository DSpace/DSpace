/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.dspace.content.Collection;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.UserSelectionActionConfig;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The workflowfactory is responsible for parsing the
 * workflow xml file and is used to retrieve the workflow for
 * a certain collection
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
public class XmlWorkflowFactoryImpl implements XmlWorkflowFactory {

    private Logger log = org.apache.logging.log4j.LogManager.getLogger(XmlWorkflowFactoryImpl.class);

    @Autowired(required = true)
    protected ConfigurationService configurationService;
    protected HashMap<String, Workflow> collectionHandleToWorkflowCache;
    protected HashMap<String, Workflow> workflowNameToWorkflowCache;
    protected List<Workflow> workflowCache;
    protected HashMap<String, List<String>> workflowNameToCollectionHandlesCache;
    protected String path;

    @PostConstruct
    protected void init() {
        path = configurationService
                .getProperty("dspace.dir") + File.separator + "config" + File.separator + "workflow.xml";
    }
//    private static String pathActions = ConfigurationManager.getProperty("dspace.dir")+"/config/workflow-actions.xml";

    protected XmlWorkflowFactoryImpl() {

    }

    @Override
    public Workflow getWorkflow(Collection collection) throws WorkflowConfigurationException {
        return this.getWorkflowById(collectionHandleToWorkflowCache, collection.getHandle(), "collection");
    }

    @Override
    public Workflow getWorkflowByName(String workflowName) throws WorkflowConfigurationException {
        return this.getWorkflowById(workflowNameToWorkflowCache, workflowName, "workflow");
    }

    /**
     * Tries to retrieve a workflow from the given cache by the given id, if it is not present it tries to resolve the
     * id to a workflow from the workflow.xml at path (by using the workflow-map with given xpathId)
     * If it gets resolved this mapping gets added to the given cache and the resolved workflow returned
     * If it can't be resolved, the default mapping gets returned (and added to cache if not already present)
     *
     * @param cache   Cache we are using for the id-workflow mapping
     * @param id      Id we're trying to resolve to a workflow
     * @param xpathId XpathId used to resolve the id to a workflow if it wasn't present in the cache
     * @return Corresponding workflow mapped to the given id
     * @throws WorkflowConfigurationException If no corresponding mapping or default can be found or error trying to
     *                                        to resolve to one
     */
    private Workflow getWorkflowById(HashMap<String, Workflow> cache, String id, String xpathId)
            throws WorkflowConfigurationException {
        //Initialize our cache if we have none
        if (cache == null) {
            cache = new HashMap<>();
        }

        // Attempt to retrieve our workflow object
        if (cache.get(id) == null) {
            try {
                // No workflow cache found for the id, check if we have a workflowId for this id
                File xmlFile = new File(path);
                Document input = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
                Node mainNode = input.getFirstChild();
                Node workflowMap = XPathAPI.selectSingleNode(mainNode,
                        "//workflow-map/name-map[@" + xpathId + "='" + id + "']");
                if (workflowMap == null) {
                    // No workflowId found by this id in cache, so retrieve & use the default workflow
                    if (cache.get("default") == null) {
                        String workflowID = XPathAPI
                                .selectSingleNode(mainNode, "//workflow-map/name-map[@" + xpathId + "='default']")
                                .getAttributes().getNamedItem("workflow").getTextContent();
                        if (workflowID == null) {
                            throw new WorkflowConfigurationException(
                                    "No workflow mapping is present for " + xpathId + ":" + id);
                        }
                        Node workflowNode = XPathAPI.selectSingleNode(mainNode, "//workflow[@id='" + workflowID + "']");
                        Workflow wf = new Workflow(workflowID, getRoles(workflowNode));
                        Step step = createFirstStep(wf, workflowNode);
                        wf.setFirstStep(step);
                        cache.put("default", wf);
                        cache.put(id, wf);
                        return wf;
                    } else {
                        return cache.get("default");
                    }

                } else {
                    // We have a workflowID so retrieve it & resolve it to a workflow, also store it in our cache
                    String workflowID = workflowMap.getAttributes().getNamedItem("workflow").getTextContent();

                    Node workflowNode = XPathAPI.selectSingleNode(mainNode, "//workflow[@id='" + workflowID + "']");
                    Workflow wf = new Workflow(workflowID, getRoles(workflowNode));
                    Step step = createFirstStep(wf, workflowNode);
                    wf.setFirstStep(step);
                    cache.put(id, wf);
                    return wf;
                }
            } catch (Exception e) {
                log.error("Error while retrieving workflow mapping of " + xpathId + ": " + id, e);
                throw new WorkflowConfigurationException(
                        "Error while retrieving workflow mapping of " + xpathId + ": " + id);
            }
        } else {
            return cache.get(id);
        }
    }

    /**
     * Creates a list of all configured workflows, or returns the cache of this if it was already created
     *
     * @return List of all configured workflows
     * @throws WorkflowConfigurationException
     */
    @Override
    public List<Workflow> getAllConfiguredWorkflows() throws WorkflowConfigurationException {
        // Initialize our cache if we have none
        if (workflowCache == null || workflowCache.size() == 0) {
            workflowCache = new ArrayList<>();
            try {
                // No workflow cache found; create it
                File xmlFile = new File(path);
                Document input = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
                Node mainNode = input.getFirstChild();
                NodeList allWorkflowNodes = XPathAPI.selectNodeList(mainNode, "//workflow-map/name-map");
                for (int i = 0; i < allWorkflowNodes.getLength(); i++) {
                    String workflowID =
                            allWorkflowNodes.item(i).getAttributes().getNamedItem("workflow").getTextContent();
                    Node workflowNode = XPathAPI.selectSingleNode(mainNode, "//workflow[@id='" + workflowID + "']");
                    Workflow wf = new Workflow(workflowID, getRoles(workflowNode));
                    Step step = createFirstStep(wf, workflowNode);
                    wf.setFirstStep(step);
                    workflowCache.add(wf);
                }
            } catch (Exception e) {
                log.error("Error while creating list of all configure workflows");
                throw new WorkflowConfigurationException("Error while creating list of all configure workflows");
            }
        }
        return workflowCache;
    }

    /**
     * Return a list of collections handles that are mapped to the given workflow in the workflow configuration.
     * Makes use of a cache so it only retrieves the workflowName->List<collectionHandle> if it's not cached
     *
     * @param workflowName Name of workflow we want the collections of that are mapped to is
     * @return List of collection handles mapped to the requested workflow
     * @throws WorkflowConfigurationException
     */
    @Override
    public List<String> getCollectionHandlesMappedToWorklow(String workflowName) throws WorkflowConfigurationException {
        // Initialize our cache if we have none
        if (workflowNameToCollectionHandlesCache == null) {
            workflowNameToCollectionHandlesCache = new HashMap<>();
        }
        // Attempt to retrieve the corresponding collections
        if (workflowNameToCollectionHandlesCache.get(workflowName) == null) {
            try {
                // No collections cached for this workflow, create it
                File xmlFile = new File(path);
                Document input = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
                Node mainNode = input.getFirstChild();
                NodeList allWorkflowNodes = XPathAPI.selectNodeList(mainNode, "//workflow-map/name-map" +
                        "[@workflow='" + workflowName + "']");
                List<String> collectionsHandlesMappedToThisWorkflow = new ArrayList<>();
                for (int i = 0; i < allWorkflowNodes.getLength(); i++) {
                    String collectionHandle =
                            allWorkflowNodes.item(i).getAttributes().getNamedItem("collection").getTextContent();
                    collectionsHandlesMappedToThisWorkflow.add(collectionHandle);
                }
                workflowNameToCollectionHandlesCache.put(workflowName, collectionsHandlesMappedToThisWorkflow);
            } catch (Exception e) {
                log.error("Error while getting collections mapped to this workflow: " + workflowName);
                throw new WorkflowConfigurationException(
                        "Error while getting collections mapped to this workflow: " + workflowName);
            }
        }
        return workflowNameToCollectionHandlesCache.get(workflowName);
    }

    protected Step createFirstStep(Workflow workflow, Node workflowNode)
            throws TransformerException, WorkflowConfigurationException {
        String firstStepID = workflowNode.getAttributes().getNamedItem("start").getTextContent();
        Node stepNode = XPathAPI.selectSingleNode(workflowNode, "step[@id='" + firstStepID + "']");
        if (stepNode == null) {
            throw new WorkflowConfigurationException(
                    "First step does not exist for workflow: " + workflowNode.getAttributes().getNamedItem("id")
                            .getTextContent());
        }
        Node roleNode = stepNode.getAttributes().getNamedItem("role");
        Role role = null;
        if (roleNode != null) {
            role = workflow.getRoles().get(roleNode.getTextContent());
        }
        String userSelectionActionID = stepNode.getAttributes().getNamedItem("userSelectionMethod").getTextContent();
        UserSelectionActionConfig userSelection = createUserAssignmentActionConfig(userSelectionActionID);
        return new Step(firstStepID, workflow, role, userSelection, getStepActionConfigs(stepNode),
                getStepOutcomes(stepNode), getNbRequiredUser(stepNode));
    }


    protected Map<Integer, String> getStepOutcomes(Node stepNode)
            throws TransformerException, WorkflowConfigurationException {
        try {
            NodeList outcomesNodeList = XPathAPI.selectNodeList(stepNode, "outcomes/step");
            Map<Integer, String> outcomes = new HashMap<Integer, String>();
            //Add our outcome, should it be null it will be interpreted as the end of the line (last step)
            for (int i = 0; i < outcomesNodeList.getLength(); i++) {
                Node outcomeNode = outcomesNodeList.item(i);
                int index = Integer.parseInt(outcomeNode.getAttributes().getNamedItem("status").getTextContent());
                if (index < 0) {
                    throw new WorkflowConfigurationException(
                            "Outcome configuration error for step: " + stepNode.getAttributes().getNamedItem("id")
                                    .getTextContent());
                }
                outcomes.put(index, outcomeNode.getTextContent());
            }
            return outcomes;
        } catch (Exception e) {
            log.error("Outcome configuration error for step: " +
                    stepNode.getAttributes().getNamedItem("id").getTextContent(), e);
            throw new WorkflowConfigurationException(
                    "Outcome configuration error for step: " + stepNode.getAttributes().getNamedItem("id")
                            .getTextContent());
        }
    }

    protected int getNbRequiredUser(Node stepnode) {
        if (stepnode.getAttributes().getNamedItem("requiredUsers") != null) {
            return Integer.parseInt(stepnode.getAttributes().getNamedItem("requiredUsers").getTextContent());
        }
        return 1;
    }

    private static List<String> getStepActionConfigs(Node stepNode) throws TransformerException {
        NodeList actionConfigNodes = XPathAPI.selectNodeList(stepNode, "actions/action");
        List<String> actionConfigIDs = new ArrayList<String>();
        for (int i = 0; i < actionConfigNodes.getLength(); i++) {
            actionConfigIDs.add(actionConfigNodes.item(i).getAttributes().getNamedItem("id").getTextContent());
        }
        return actionConfigIDs;
    }

    @Override
    public Step createStep(Workflow workflow, String stepID) throws WorkflowConfigurationException, IOException {
        try {
            File xmlFile = new File(path);
            Document input = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            Node mainNode = input.getFirstChild();
            Node stepNode = XPathAPI.selectSingleNode(mainNode, "//workflow[@id='" + workflow.getID()
                    + "']/step[@id='" + stepID + "']");

            if (stepNode == null) {
                throw new WorkflowConfigurationException("Step does not exist for workflow: " + workflow.getID());
            }
            Node roleNode = stepNode.getAttributes().getNamedItem("role");
            Role role = null;
            if (roleNode != null) {
                role = workflow.getRoles().get(roleNode.getTextContent());
            }
            String userSelectionActionID = stepNode.getAttributes().getNamedItem("userSelectionMethod")
                    .getTextContent();
            UserSelectionActionConfig userSelection = createUserAssignmentActionConfig(userSelectionActionID);
            return new Step(stepID, workflow, role, userSelection, getStepActionConfigs(stepNode),
                    getStepOutcomes(stepNode), getNbRequiredUser(stepNode));

        } catch (Exception e) {
            log.error("Error while creating step with :" + stepID, e);
            throw new WorkflowConfigurationException(
                    "Step: " + stepID + " does not exist for workflow: " + workflow.getID());
        }


    }

    protected UserSelectionActionConfig createUserAssignmentActionConfig(String userSelectionActionID) {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName(userSelectionActionID, UserSelectionActionConfig.class);
    }

    @Override
    public WorkflowActionConfig createWorkflowActionConfig(String actionID) {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName(actionID, WorkflowActionConfig.class);
    }

    protected LinkedHashMap<String, Role> getRoles(Node workflowNode) throws WorkflowConfigurationException {
        NodeList roleNodes = null;
        try {
            roleNodes = XPathAPI.selectNodeList(workflowNode, "roles/role");
        } catch (Exception e) {
            log.error("Error while resolving nodes", e);
            throw new WorkflowConfigurationException("Error while retrieving roles");
        }
        LinkedHashMap<String, Role> roles = new LinkedHashMap<String, Role>();
        for (int i = 0; i < roleNodes.getLength(); i++) {
            String roleID = roleNodes.item(i).getAttributes().getNamedItem("id").getTextContent();
            String roleName = roleNodes.item(i).getAttributes().getNamedItem("name").getTextContent();
            Node descriptionNode = roleNodes.item(i).getAttributes().getNamedItem("description");
            String roleDescription = null;
            if (descriptionNode != null) {
                roleDescription = descriptionNode.getTextContent();
            }

            Node scopeNode = roleNodes.item(i).getAttributes().getNamedItem("scope");
            String roleScope = null;
            if (scopeNode != null) {
                roleScope = scopeNode.getTextContent();
            }

            Node internalNode = roleNodes.item(i).getAttributes().getNamedItem("internal");
            String roleInternal;
            boolean internal = false;
            if (internalNode != null) {
                roleInternal = internalNode.getTextContent();
                internal = Boolean.parseBoolean(roleInternal);

            }

            Role.Scope scope;
            if (roleScope == null || roleScope.equalsIgnoreCase("collection")) {
                scope = Role.Scope.COLLECTION;
            } else if (roleScope.equalsIgnoreCase("item")) {
                scope = Role.Scope.ITEM;
            } else if (roleScope.equalsIgnoreCase("repository")) {
                scope = Role.Scope.REPOSITORY;
            } else {
                throw new WorkflowConfigurationException(
                        "An invalid role scope has been specified it must either be item or collection.");
            }

            Role role = new Role(roleID, roleName, roleDescription, internal, scope);
            roles.put(roleID, role);
        }
        return roles;
    }

}
