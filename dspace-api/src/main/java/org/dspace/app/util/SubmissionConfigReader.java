/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.File;
import java.util.*;
import javax.servlet.ServletException;
import org.xml.sax.SAXException;
import org.w3c.dom.*;
import javax.xml.parsers.*;

import org.apache.log4j.Logger;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Item Submission configuration generator for DSpace. Reads and parses the
 * installed submission process configuration file, item-submission.xml, from
 * the configuration directory. This submission process definition details the
 * ordering of the steps (and number of steps) that occur during the Item
 * Submission Process. There may be multiple Item Submission processes defined,
 * where each definition is assigned a unique name.
 * 
 * The file also specifies which collections use which Item Submission process.
 * At a minimum, the definitions file must define a default mapping from the
 * placeholder collection # to the distinguished submission process 'default'.
 * Any collections that use a custom submission process are listed paired with
 * the name of the item submission process they use.
 * 
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * 
 * @author Tim Donohue based on DCInputsReader by Brian S. Hughes
 * @version $Revision$
 */

public class SubmissionConfigReader
{
    /**
     * The ID of the default collection. Will never be the ID of a named
     * collection
     */
    public static final String DEFAULT_COLLECTION = "default";

    /** Prefix of the item submission definition XML file */
    static final String SUBMIT_DEF_FILE_PREFIX = "item-submission";
    
    /** Suffix of the item submission definition XML file */
    static final String SUBMIT_DEF_FILE_SUFFIX = ".xml";

    /** log4j logger */
    private static Logger log = Logger.getLogger(SubmissionConfigReader.class);

	/** The fully qualified pathname of the directory containing the Item Submission Configuration file */
    private String configDir = DSpaceServicesFactory.getInstance()
            .getConfigurationService().getProperty("dspace.dir")
            + File.separator + "config" + File.separator;
            
    /**
     * Hashmap which stores which submission process configuration is used by
     * which collection, computed from the item submission config file
     * (specifically, the 'submission-map' tag)
     */
    private Map<String, String> collectionToSubmissionConfig = null;

    /**
     * Reference to the global submission step definitions defined in the
     * "step-definitions" section
     */
    private Map<String, Map<String, String>> stepDefns = null;

    /**
     * Reference to the item submission definitions defined in the
     * "submission-definitions" section
     */
    private Map<String, List<Map<String, String>>> submitDefns = null;

    /**
     * Mini-cache of last SubmissionConfig object requested (so that we don't
     * always reload from scratch)
     */
    private SubmissionConfig lastSubmissionConfig = null;

    /**
     * Load Submission Configuration from the
     * item-submission.xml configuration file 
     * @throws ServletException if servlet error
     */
    public SubmissionConfigReader() throws ServletException
    {
        buildInputs(configDir + SUBMIT_DEF_FILE_PREFIX + SUBMIT_DEF_FILE_SUFFIX);
    }

    /**
     * Parse an XML encoded item submission configuration file.
     * <P>
     * Creates two main hashmaps:
     * <ul>
     * <li>Hashmap of Collection to Submission definition mappings -
     * defines which Submission process a particular collection uses
     * <li>Hashmap of all Submission definitions.  List of all valid
     * Submision Processes by name.
     * </ul>
     */
    private void buildInputs(String fileName) throws ServletException
    {
        collectionToSubmissionConfig = new HashMap<String, String>();
        submitDefns = new HashMap<String, List<Map<String, String>>>();

        String uri = "file:" + new File(fileName).getAbsolutePath();

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder db = factory.newDocumentBuilder();
            Document doc = db.parse(uri);
            doNodes(doc);
        }
        catch (FactoryConfigurationError fe)
        {
            throw new ServletException(
                    "Cannot create Item Submission Configuration parser", fe);
        }
        catch (Exception e)
        {
            throw new ServletException(
                    "Error creating Item Submission Configuration: " + e);
        }
    }

    /**
     * Returns the Item Submission process config used for a particular
     * collection, or the default if none is defined for the collection
     * 
     * @param collectionHandle
     *            collection's unique Handle
     * @param isWorkflow
     *            whether or not we are loading the submission process for a
     *            workflow
     * @return the SubmissionConfig representing the item submission config
     * 
     * @throws ServletException
     *             if no default submission process configuration defined
     */
    public SubmissionConfig getSubmissionConfig(String collectionHandle,
            boolean isWorkflow) throws ServletException
    {
        // get the name of the submission process config for this collection
        String submitName = collectionToSubmissionConfig
                .get(collectionHandle);
        if (submitName == null)
        {
            submitName = collectionToSubmissionConfig
                    .get(DEFAULT_COLLECTION);
        }
        if (submitName == null)
        {
            throw new ServletException(
                    "No item submission process configuration designated as 'default' in 'submission-map' section of 'item-submission.xml'.");
        }

        log.debug("Loading submission process config named '" + submitName
                + "'");

        // check mini-cache, and return if match
        if (lastSubmissionConfig != null
                && lastSubmissionConfig.getSubmissionName().equals(submitName)
                && lastSubmissionConfig.isWorkflow() == isWorkflow)
        {
            log.debug("Found submission process config '" + submitName
                    + "' in cache.");

            return lastSubmissionConfig;
        }

        // cache miss - construct new SubmissionConfig
        List<Map<String, String>> steps = submitDefns.get(submitName);

        if (steps == null)
        {
            throw new ServletException(
                    "Missing the Item Submission process config '" + submitName
                            + "' (or unable to load) from 'item-submission.xml'.");
        }

        log.debug("Submission process config '" + submitName
                + "' not in cache. Reloading from scratch.");

        lastSubmissionConfig = new SubmissionConfig(submitName, steps,
                isWorkflow);

        log.debug("Submission process config has "
                + lastSubmissionConfig.getNumberOfSteps() + " steps listed.");

        return lastSubmissionConfig;
    }

    /**
     * Returns a particular global step definition based on its ID.
     * <P>
     * Global step definitions are those defined in the {@code <step-definitions>}
     * section of the configuration file.
     * 
     * @param stepID
     *            step's identifier
     * 
     * @return the SubmissionStepConfig representing the step
     * 
     * @throws ServletException
     *             if no default submission process configuration defined
     */
    public SubmissionStepConfig getStepConfig(String stepID)
            throws ServletException
    {
        // We should already have the step definitions loaded
        if (stepDefns != null)
        {
            // retreive step info
            Map<String, String> stepInfo = stepDefns.get(stepID);

            if (stepInfo != null)
            {
                return new SubmissionStepConfig(stepInfo);
            }
        }

        return null;
    }

    /**
     * Process the top level child nodes in the passed top-level node. These
     * should correspond to the collection-form maps, the form definitions, and
     * the display/storage word pairs.
     */
    private void doNodes(Node n) throws SAXException, ServletException
    {
        if (n == null)
        {
            return;
        }
        Node e = getElement(n);
        NodeList nl = e.getChildNodes();
        int len = nl.getLength();
        boolean foundMap = false;
        boolean foundStepDefs = false;
        boolean foundSubmitDefs = false;
        for (int i = 0; i < len; i++)
        {
            Node nd = nl.item(i);
            if ((nd == null) || isEmptyTextNode(nd))
            {
                continue;
            }
            String tagName = nd.getNodeName();
            if (tagName.equals("submission-map"))
            {
                processMap(nd);
                foundMap = true;
            }
            else if (tagName.equals("step-definitions"))
            {
                processStepDefinition(nd);
                foundStepDefs = true;
            }
            else if (tagName.equals("submission-definitions"))
            {
                processSubmissionDefinition(nd);
                foundSubmitDefs = true;
            }
            // Ignore unknown nodes
        }
        if (!foundMap)
        {
            throw new ServletException(
                    "No collection to item submission map ('submission-map') found in 'item-submission.xml'");
        }
        if (!foundStepDefs)
        {
            throw new ServletException("No 'step-definitions' section found in 'item-submission.xml'");
        }
        if (!foundSubmitDefs)
        {
            throw new ServletException(
                    "No 'submission-definitions' section found in 'item-submission.xml'");
        }
    }

    /**
     * Process the submission-map section of the XML file. Each element looks
     * like: <name-map collection-handle="hdl" submission-name="name" /> Extract
     * the collection handle and item submission name, put name in hashmap keyed
     * by the collection handle.
     */
    private void processMap(Node e) throws SAXException
    {
        NodeList nl = e.getChildNodes();
        int len = nl.getLength();
        for (int i = 0; i < len; i++)
        {
            Node nd = nl.item(i);
            if (nd.getNodeName().equals("name-map"))
            {
                String id = getAttribute(nd, "collection-handle");
                String value = getAttribute(nd, "submission-name");
                String content = getValue(nd);
                if (id == null)
                {
                    throw new SAXException(
                            "name-map element is missing collection-handle attribute in 'item-submission.xml'");
                }
                if (value == null)
                {
                    throw new SAXException(
                            "name-map element is missing submission-name attribute in 'item-submission.xml'");
                }
                if (content != null && content.length() > 0)
                {
                    throw new SAXException(
                            "name-map element has content in 'item-submission.xml', it should be empty.");
                }
                collectionToSubmissionConfig.put(id, value);
            } // ignore any child node that isn't a "name-map"
        }
    }

    /**
     * Process the "step-definition" section of the XML file. Each element is
     * formed thusly: <step id="unique-id"> ...step_fields... </step> The valid
     * step_fields are: heading, processing-servlet.
     * <P>
     * Extract the step information (from the step_fields) and place in a
     * HashMap whose key is the step's unique id.
     */
    private void processStepDefinition(Node e) throws SAXException,
            ServletException
    {
        stepDefns = new HashMap<String, Map<String, String>>();

        NodeList nl = e.getChildNodes();
        int len = nl.getLength();
        for (int i = 0; i < len; i++)
        {
            Node nd = nl.item(i);
            // process each step definition
            if (nd.getNodeName().equals("step"))
            {
                String stepID = getAttribute(nd, "id");
                if (stepID == null)
                {
                    throw new SAXException(
                            "step element has no 'id' attribute in 'item-submission.xml', which is required in the 'step-definitions' section");
                }
                else if (stepDefns.containsKey(stepID))
                {
                    throw new SAXException(
                            "There are two step elements with the id '" + stepID + "' in 'item-submission.xml'");
                }

                Map<String, String> stepInfo = processStepChildNodes("step-definition", nd);

                stepDefns.put(stepID, stepInfo);
            } // ignore any child that is not a 'step'
        }

        // Sanity check number of step definitions
        if (stepDefns.size() < 1)
        {
            throw new ServletException(
                    "step-definition section has no steps! A step with id='collection' is required in 'item-submission.xml'!");
        }

        // Sanity check to see that the required "collection" step is defined
        if (!stepDefns.containsKey(SubmissionStepConfig.SELECT_COLLECTION_STEP))
        {
            throw new ServletException(
                    "The step-definition section is REQUIRED to have a step with id='"
                            + SubmissionStepConfig.SELECT_COLLECTION_STEP
                            + "' in 'item-submission.xml'!  This step is used to ensure that a new item submission is assigned to a collection.");
        }

        // Sanity check to see that the required "complete" step is defined
        if (!stepDefns.containsKey(SubmissionStepConfig.COMPLETE_STEP))
        {
            throw new ServletException(
                    "The step-definition section is REQUIRED to have a step with id='"
                            + SubmissionStepConfig.COMPLETE_STEP
                            + "' in 'item-submission.xml'!  This step is used to perform all processing necessary at the completion of the submission (e.g. starting workflow).");
        }
    }

    /**
     * Process the "submission-definition" section of the XML file. Each element
     * is formed thusly: <submission-process name="submitName">...steps...</submit-process>
     * Each step subsection is formed: <step> ...step_fields... </step> (with
     * optional "id" attribute, to reference a step from the <step-definition>
     * section). The valid step_fields are: heading, class-name.
     * <P>
     * Extract the submission-process name and steps and place in a HashMap
     * whose key is the submission-process's unique name.
     */
    private void processSubmissionDefinition(Node e) throws SAXException,
            ServletException
    {
        int numSubmitProcesses = 0;
        List<String> submitNames = new ArrayList<String>();

        // find all child nodes of the 'submission-definition' node and loop
        // through
        NodeList nl = e.getChildNodes();
        int len = nl.getLength();
        for (int i = 0; i < len; i++)
        {
            Node nd = nl.item(i);

            // process each 'submission-process' node
            if (nd.getNodeName().equals("submission-process"))
            {
                numSubmitProcesses++;
                String submitName = getAttribute(nd, "name");
                if (submitName == null)
                {
                    throw new SAXException(
                            "'submission-process' element has no 'name' attribute in 'item-submission.xml'");
                }
                else if (submitNames.contains(submitName))
                {
                    throw new SAXException(
                            "There are two 'submission-process' elements with the name '"
                                    + submitName + "' in 'item-submission.xml'.");
                }
                submitNames.add(submitName);

                // the 'submission-process' definition contains steps
                List<Map<String, String>> steps = new ArrayList<Map<String, String>>();
                submitDefns.put(submitName, steps);

                // loop through all the 'step' nodes of the 'submission-process'
                NodeList pl = nd.getChildNodes();
                int lenStep = pl.getLength();
                for (int j = 0; j < lenStep; j++)
                {
                    Node nStep = pl.item(j);

                    // process each 'step' definition
                    if (nStep.getNodeName().equals("step"))
                    {
                        // check for an 'id' attribute
                        String stepID = getAttribute(nStep, "id");

                        Map<String, String> stepInfo;

                        // if this step has an id, load its information from the
                        // step-definition section
                        if ((stepID != null) && (stepID.length() > 0))
                        {
                            if (stepDefns.containsKey(stepID))
                            {
                                // load the step information from the
                                // step-definition
                                stepInfo = stepDefns.get(stepID);
                            }
                            else
                            {
                                throw new SAXException(
                                        "The Submission process config named "
                                                + submitName
                                                + " contains a step with id="
                                                + stepID
                                                + ".  There is no step with this 'id' defined in the 'step-definition' section of 'item-submission.xml'.");
                            }

                            // Ignore all children of a step element with an
                            // "id"
                        }
                        else
                        {
                            // get information about step from its children
                            // nodes
                            stepInfo = processStepChildNodes(
                                    "submission-process", nStep);
                        }

                        steps.add(stepInfo);

                    } // ignore any child that is not a 'step'
                }

                // sanity check number of steps
                if (steps.size() < 1)
                {
                    throw new ServletException(
                            "Item Submission process config named "
                                    + submitName + " has no steps defined in 'item-submission.xml'");
                }

                // ALL Item Submission processes MUST BEGIN with selecting a
                // Collection. So, automatically insert in the "collection" step
                // (from the 'step-definition' section)
                // Note: we already did a sanity check that this "collection"
                // step exists.
                steps.add(0, stepDefns
                        .get(SubmissionStepConfig.SELECT_COLLECTION_STEP));

                // ALL Item Submission processes MUST END with the
                // "Complete" processing step.
                // So, automatically append in the "complete" step
                // (from the 'step-definition' section)
                // Note: we already did a sanity check that this "complete"
                // step exists.
                steps.add(stepDefns
                        .get(SubmissionStepConfig.COMPLETE_STEP));

            }
        }
        if (numSubmitProcesses == 0)
        {
            throw new ServletException(
                    "No 'submission-process' elements/definitions found in 'item-submission.xml'");
        }
    }

    /**
     * Process the children of the "step" tag of the XML file. Returns a HashMap
     * of all the fields under that "step" tag, where the key is the field name,
     * and the value is the field value.
     * 
     */
    private Map<String, String> processStepChildNodes(String configSection, Node nStep)
            throws SAXException, ServletException
    {
        // initialize the HashMap of step Info
        Map<String, String> stepInfo = new HashMap<String, String>();

        NodeList flds = nStep.getChildNodes();
        int lenflds = flds.getLength();
        for (int k = 0; k < lenflds; k++)
        {
            // process each child node of a <step> tag
            Node nfld = flds.item(k);

            if (!isEmptyTextNode(nfld))
            {
                String tagName = nfld.getNodeName();
                String value = getValue(nfld);
                stepInfo.put(tagName, value);
            }
        }// end for each field

        // check for ID attribute & save to step info
        String stepID = getAttribute(nStep, "id");
        if (stepID != null && stepID.length() > 0)
        {
            stepInfo.put("id", stepID);
        }

        // look for REQUIRED 'step' information
        String missing = null;
        if (stepInfo.get("processing-class") == null)
        {
            missing = "'processing-class'";
        }
        if (missing != null)
        {
            String msg = "Required field " + missing
                    + " missing in a 'step' in the " + configSection
                    + " of the item submission configuration file ('item-submission.xml')";
            throw new SAXException(msg);
        }

        return stepInfo;
    }

    private Node getElement(Node nd)
    {
        NodeList nl = nd.getChildNodes();
        int len = nl.getLength();
        for (int i = 0; i < len; i++)
        {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE)
            {
                return n;
            }
        }
        return null;
    }

    private boolean isEmptyTextNode(Node nd)
    {
        boolean isEmpty = false;
        if (nd.getNodeType() == Node.TEXT_NODE)
        {
            String text = nd.getNodeValue().trim();
            if (text.length() == 0)
            {
                isEmpty = true;
            }
        }
        return isEmpty;
    }

    /**
     * Returns the value of the node's attribute named <name>
     */
    private String getAttribute(Node e, String name)
    {
        NamedNodeMap attrs = e.getAttributes();
        int len = attrs.getLength();
        if (len > 0)
        {
            int i;
            for (i = 0; i < len; i++)
            {
                Node attr = attrs.item(i);
                if (name.equals(attr.getNodeName()))
                {
                    return attr.getNodeValue().trim();
                }
            }
        }
        // no such attribute
        return null;
    }

    /**
     * Returns the value found in the Text node (if any) in the node list that's
     * passed in.
     */
    private String getValue(Node nd)
    {
        NodeList nl = nd.getChildNodes();
        int len = nl.getLength();
        for (int i = 0; i < len; i++)
        {
            Node n = nl.item(i);
            short type = n.getNodeType();
            if (type == Node.TEXT_NODE)
            {
                return n.getNodeValue().trim();
            }
        }
        // Didn't find a text node
        return null;
    }
}
