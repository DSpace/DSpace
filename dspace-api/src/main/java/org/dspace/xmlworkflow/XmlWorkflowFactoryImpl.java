/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.dspace.content.Collection;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.UserSelectionActionConfig;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * The workflowfactory is responsible for parsing the
 * workflow xml file and is used to retrieve the workflow for
 * a certain collection
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class XmlWorkflowFactoryImpl implements XmlWorkflowFactory {

    private Logger log = Logger.getLogger(XmlWorkflowFactoryImpl.class);

    @Autowired(required = true)
    protected ConfigurationService configurationService;
    protected HashMap<String, Workflow> workflowCache;
    protected String path;

    @PostConstruct
    protected void init()
    {
        path = configurationService.getProperty("dspace.dir")+ File.separator + "config" + File.separator + "workflow.xml";
    }
//    private static String pathActions = ConfigurationManager.getProperty("dspace.dir")+"/config/workflow-actions.xml";

    protected XmlWorkflowFactoryImpl()
    {

    }

    @Override
    public Workflow getWorkflow(Collection collection) throws IOException, WorkflowConfigurationException {
        //Initialize our cache if we have none
        if(workflowCache == null)
            workflowCache = new HashMap<>();

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
                throw new WorkflowConfigurationException("Error while retrieving workflow for the following collection: " + collection.getHandle());
            }
        }else{
            return workflowCache.get(collection.getHandle());
        }
    }

    protected Step createFirstStep(Workflow workflow, Node workflowNode) throws TransformerException, WorkflowConfigurationException {
        String firstStepID = workflowNode.getAttributes().getNamedItem("start").getTextContent();
        Node stepNode = XPathAPI.selectSingleNode(workflowNode, "step[@id='"+firstStepID+"']");
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



    protected Map<Integer, String> getStepOutcomes(Node stepNode) throws TransformerException, WorkflowConfigurationException {
        try{
            NodeList outcomesNodeList = XPathAPI.selectNodeList(stepNode, "outcomes/step");
            Map<Integer, String> outcomes = new HashMap<Integer, String>();
            //Add our outcome, should it be null it will be interpreted as the end of the line (last step)
            for(int i = 0; i < outcomesNodeList.getLength(); i++){
                Node outcomeNode = outcomesNodeList.item(i);
                int index = Integer.parseInt(outcomeNode.getAttributes().getNamedItem("status").getTextContent());
                if(index < 0){
                    throw new WorkflowConfigurationException("Outcome configuration error for step: "+stepNode.getAttributes().getNamedItem("id").getTextContent());
                }
                outcomes.put(index, outcomeNode.getTextContent());
            }
            return outcomes;
        }catch(Exception e){
            log.error("Outcome configuration error for step: " + stepNode.getAttributes().getNamedItem("id").getTextContent(), e);
            throw new WorkflowConfigurationException("Outcome configuration error for step: "+stepNode.getAttributes().getNamedItem("id").getTextContent());
        }
    }
    protected int getNbRequiredUser(Node stepnode){
        if(stepnode.getAttributes().getNamedItem("requiredUsers")!=null){
            return Integer.parseInt(stepnode.getAttributes().getNamedItem("requiredUsers").getTextContent());
        }
        return 1;
    }
    private static List<String> getStepActionConfigs(Node stepNode) throws TransformerException {
        NodeList actionConfigNodes = XPathAPI.selectNodeList(stepNode, "actions/action");
        List<String> actionConfigIDs = new ArrayList<String>();
        for(int i = 0; i < actionConfigNodes.getLength(); i++){
            actionConfigIDs.add(actionConfigNodes.item(i).getAttributes().getNamedItem("id").getTextContent());
        }
        return actionConfigIDs;
    }

    @Override
    public Step createStep(Workflow workflow, String stepID) throws WorkflowConfigurationException, IOException {
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
            log.error("Error while creating step with :" + stepID, e);
            throw new WorkflowConfigurationException("Step: " + stepID + " does not exist for workflow: "+workflow.getID());
        }


    }
     protected UserSelectionActionConfig createUserAssignmentActionConfig(String userSelectionActionID) {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(userSelectionActionID, UserSelectionActionConfig.class);
    }

    @Override
    public WorkflowActionConfig createWorkflowActionConfig(String actionID){
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(actionID, WorkflowActionConfig.class);
    }

    protected LinkedHashMap<String, Role> getRoles(Node workflowNode) throws WorkflowConfigurationException {
        NodeList roleNodes = null;
        try{
            roleNodes = XPathAPI.selectNodeList(workflowNode, "roles/role");
        }catch (Exception e){
            log.error("Error while resolving nodes", e);
            throw new WorkflowConfigurationException("Error while retrieving roles");
        }
        LinkedHashMap<String, Role> roles = new LinkedHashMap<String, Role>();
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
