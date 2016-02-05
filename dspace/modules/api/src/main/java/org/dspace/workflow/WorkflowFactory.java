package org.dspace.workflow;

import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.workflow.actions.UserSelectionActionConfig;
import org.dspace.workflow.actions.WorkflowActionConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 3-aug-2010
 * Time: 13:51:18
 * To change this template use File | Settings | File Templates.
 */
public class WorkflowFactory {

    private static Logger log = Logger.getLogger(WorkflowFactory.class);

    private static HashMap<String, Workflow> workflowCache;
    private static String path = ConfigurationManager.getProperty("dspace.dir")+"/config/workflow.xml";
    private static String pathActions = ConfigurationManager.getProperty("dspace.dir")+"/config/workflow-actions.xml";

    //TODO: Depending on the role system, create one wf object per collection
    public static Workflow getWorkflow(Collection collection) throws IOException, WorkflowConfigurationException {
        //Initialize our cache if we have none
        if(workflowCache == null)
            workflowCache = new HashMap<String, Workflow>();

        // Attempt to retrieve our workflow object
        if(workflowCache.get(collection.getHandle())==null){
            try{
                // No workflow cache found for the collection, check if we have a workflowId for this collection
                File xmlFile = new File(path);
                Document input = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
                Node mainNode = input.getFirstChild();
                Node workflowMap = XPathAPI.selectSingleNode(mainNode, "//workflow-map/name-map[@collection='"+collection.getHandle()+"']");
                if(workflowMap==null){
                    //No workflowId found for this collection, so retrieve & use the default workflow
                    if(workflowCache.get("default")==null){
                        String workflowID = XPathAPI.selectSingleNode(mainNode, "//workflow-map/name-map[@collection='default']").getAttributes().getNamedItem("workflow").getTextContent();
                        if(workflowID==null){
                            throw new WorkflowConfigurationException("No mapping is present for collection with handle:" + collection.getHandle());
                        }
                        Node workflowNode = XPathAPI.selectSingleNode(mainNode, "//workflow[@id='"+workflowID+"']");
                        Workflow wf = new Workflow(workflowID, getRoles(workflowNode));
                        Step step = createFirstStep(wf, workflowNode);
                        wf.setFirstStep(step);
                        workflowCache.put("default", wf);
                        workflowCache.put(collection.getHandle(), wf);
                        return wf;
                    }else{
                        return workflowCache.get("default");
                    }

                }else{
                    //We have a workflowID so retrieve it & resolve it to a workflow, also store it in our cache
                    String workflowID = workflowMap.getAttributes().getNamedItem("workflow").getTextContent();

                    Node workflowNode = XPathAPI.selectSingleNode(mainNode, "//workflow[@id='"+workflowID+"']");
                    Workflow wf = new Workflow(workflowID, getRoles(workflowNode));
                    Step step = createFirstStep(wf, workflowNode);
                    wf.setFirstStep(step);
                    workflowCache.put(collection.getHandle(), wf);
                    return wf;
                }
            } catch (Exception e){
                log.error("Error while retrieving workflow for collection: " + collection.getHandle(), e);
                throw new WorkflowConfigurationException("Error while retrieving workflow for the following collection: " + collection.getHandle(), e);
            }
        }else{
            return workflowCache.get(collection.getHandle());
        }
    }

    private static Step createFirstStep(Workflow workflow, Node workflowNode) throws TransformerException, WorkflowConfigurationException {
        String firstStepID = workflowNode.getAttributes().getNamedItem("start").getTextContent();
        Node stepNode = XPathAPI.selectSingleNode(workflowNode, "//step[@id='"+firstStepID+"']");
        if(stepNode == null){
            throw new WorkflowConfigurationException("First step does not exist for workflow: "+workflowNode.getAttributes().getNamedItem("id").getTextContent());
        }
        Node roleNode = stepNode.getAttributes().getNamedItem("role");
        Role role = null;
        if(roleNode != null)
            role = workflow.getRoles().get(roleNode.getTextContent());
        String userSelectionActionID = stepNode.getAttributes().getNamedItem("userSelectionMethod").getTextContent();
        UserSelectionActionConfig userSelection = createUserAssignmentActionConfig(userSelectionActionID);
        return new Step(firstStepID, workflow, role, userSelection, getStepActionConfigs(stepNode), getStepOutcomes(stepNode), getNbRequiredUser(stepNode));
    }



    private static List<String> getStepOutcomes(Node stepNode) throws TransformerException, WorkflowConfigurationException {
        try{
            String outcome = null;
            if(stepNode.getAttributes().getNamedItem("nextStep") != null)
            {
                outcome = stepNode.getAttributes().getNamedItem("nextStep").getTextContent();
            }

            NodeList alternativeOutcomes = XPathAPI.selectNodeList(stepNode, "alternativeOutcome/step");
            List<String> outcomes = new ArrayList<String>();
            //Add our outcome, should it be null it will be interpreted as the end of the line (last step)
            outcomes.add(outcome);
            for(int i = 0; i < alternativeOutcomes.getLength(); i++){
                Node alternativeOutcome = alternativeOutcomes.item(i);
                int index = Integer.parseInt(alternativeOutcome.getAttributes().getNamedItem("status").getTextContent());
                if(index <= 0){
                    throw new WorkflowConfigurationException("Outcome configuration error for step: "+stepNode.getAttributes().getNamedItem("id").getTextContent());
                }
                outcomes.add(index, alternativeOutcome.getTextContent());
            }
            return outcomes;
        }catch(Exception e){
            log.error("Outcome configuration error for step: " + stepNode.getAttributes().getNamedItem("id").getTextContent());
            throw new WorkflowConfigurationException("Outcome configuration error for step: "+stepNode.getAttributes().getNamedItem("id").getTextContent());
        }
    }
    private static int getNbRequiredUser(Node stepnode){
        if(stepnode.getAttributes().getNamedItem("requiredUsers")!=null){
            return Integer.parseInt(stepnode.getAttributes().getNamedItem("requiredUsers").getTextContent());
        }
        return 1;
    }
    private static List<String> getStepActionConfigs(Node stepNode) throws TransformerException {
        NodeList actionConfigNodes = XPathAPI.selectNodeList(stepNode, "action");
        List<String> actionConfigIDs = new ArrayList<String>();
        for(int i = 0; i < actionConfigNodes.getLength(); i++){
            actionConfigIDs.add(actionConfigNodes.item(i).getAttributes().getNamedItem("id").getTextContent());
        }
        return actionConfigIDs;
    }

    public static List<String> getAllStepIds(Workflow workflow) throws WorkflowConfigurationException {
        List<String> result = new ArrayList<String>();
        try{
            File xmlFile = new File(path);
            Document input = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            Node mainNode = input.getFirstChild();
            NodeList stepNodes = XPathAPI.selectNodeList(mainNode, "//workflow[@id='"+workflow.getID()+"']/step");

            for(int i = 0; i < stepNodes.getLength(); i++){
                Node stepNode = stepNodes.item(i);
                result.add(stepNode.getAttributes().getNamedItem("id").getTextContent());
            }
        }catch (Exception e){
            log.error("Error while getting step ids steps");
            throw new WorkflowConfigurationException("Step does not exist for workflow: "+workflow.getID());
        }

        return result;
    }

    public static Step createStep(Workflow workflow, String stepID) throws WorkflowConfigurationException, IOException {
        try{
            File xmlFile = new File(path);
            Document input = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            Node mainNode = input.getFirstChild();
            Node stepNode = XPathAPI.selectSingleNode(mainNode, "//workflow[@id='"+workflow.getID()+"']/step[@id='"+stepID+"']");

            if(stepNode == null){
                throw new WorkflowConfigurationException("Step does not exist for workflow: "+workflow.getID());
            }
            Node roleNode = stepNode.getAttributes().getNamedItem("role");
            Role role = null;
            if(roleNode != null)
                role = workflow.getRoles().get(roleNode.getTextContent());
            String userSelectionActionID = stepNode.getAttributes().getNamedItem("userSelectionMethod").getTextContent();
            UserSelectionActionConfig userSelection = createUserAssignmentActionConfig(userSelectionActionID);
            return new Step(stepID, workflow, role, userSelection, getStepActionConfigs(stepNode), getStepOutcomes(stepNode), getNbRequiredUser(stepNode));

        }catch (Exception e){
            log.error("Error while creating step with :" + stepID);
            throw new WorkflowConfigurationException("Step does not exist for workflow: "+workflow.getID());
        }


    }
     private static UserSelectionActionConfig createUserAssignmentActionConfig(String userSelectionActionID) {
        ApplicationContext applicationContext = new FileSystemXmlApplicationContext("file:" + pathActions);
        UserSelectionActionConfig config = (UserSelectionActionConfig) applicationContext.getBean(userSelectionActionID, UserSelectionActionConfig.class);
        return config;
    }

    public static WorkflowActionConfig createWorkflowActionConfig(String actionID){
        ApplicationContext applicationContext = new FileSystemXmlApplicationContext("file:" + pathActions);
        WorkflowActionConfig config = (WorkflowActionConfig) applicationContext.getBean(actionID, WorkflowActionConfig.class);
        return config;
    }

    private static HashMap<String, Role> getRoles(Node workflowNode) throws WorkflowConfigurationException {
        NodeList roleNodes = null;
        try{
            roleNodes = XPathAPI.selectNodeList(workflowNode, "roles/role");
        }catch (Exception e){
            log.error("Error while resolving nodes", e);
            throw new WorkflowConfigurationException("Error while retrieving roles");
        }
        HashMap<String, Role> roles = new HashMap<String, Role>();
        for(int i = 0; i < roleNodes.getLength(); i++){
            String roleID = roleNodes.item(i).getAttributes().getNamedItem("id").getTextContent();
            String roleName = roleNodes.item(i).getAttributes().getNamedItem("name").getTextContent();
            Node descriptionNode = roleNodes.item(i).getAttributes().getNamedItem("description");
            String roleDescription = null;
            if(descriptionNode != null)
                roleDescription = descriptionNode.getTextContent();

            Node scopeNode = roleNodes.item(i).getAttributes().getNamedItem("scope");
            String roleScope = null;
            if(scopeNode != null)
                roleScope = scopeNode.getTextContent();

            Node internalNode = roleNodes.item(i).getAttributes().getNamedItem("internal");
            String roleInternal;
            boolean internal = false;
            if(internalNode != null){
                roleInternal = internalNode.getTextContent();
                internal = Boolean.parseBoolean(roleInternal);

            }

            Role.Scope scope;
            if(roleScope == null || roleScope.equalsIgnoreCase("collection"))
                scope = Role.Scope.COLLECTION;
            else
            if(roleScope.equalsIgnoreCase("item"))
                scope = Role.Scope.ITEM;
            else
            if(roleScope.equalsIgnoreCase("repository"))
                scope = Role.Scope.REPOSITORY;
            else
                throw new WorkflowConfigurationException("An invalid role scope has been specified it must either be item or collection.");

            Role role = new Role(roleID, roleName, roleDescription,internal, scope);
            roles.put(roleID, role);
        }
        return roles;
    }

}
