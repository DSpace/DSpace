/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package ar.edu.unlp.sedici.dspace.administer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public final class ImportUsers
{

	/**
     * The ID of the default collection. Will never be the ID of a named
     * collection
     */
    public static final String DEFAULT_COLLECTION = "default";

    /** Name of the form definition XML file  */
    static final String IMPORT_USERS = "import-users.xml";

    /** Keyname for storing dropdown value-pair set name */
    static final String TYPE_NAME = "users";

    /** The fully qualified pathname of the form definition XML file */
    private static String defsFile = ConfigurationManager.getProperty("dspace.dir")
            + File.separator + "config" + File.separator + IMPORT_USERS;

    /**
     * Reference to the collections to forms map, computed from the forms
     * definition file
     */
    private Map<String, String> users = null;

    /**
     * Reference to the forms definitions map, computed from the forms
     * definition file
     */
    private Map<String, List<List<Map<String, String>>>> formDefns  = null;

    /**
     * Reference to the value-pairs map, computed from the forms definition file
     */
    private Map<String, List<String>> valuePairs = null;    // Holds display/storage pairs
    
    /**
     * Mini-cache of last DCInputSet requested. If submissions are not typically
     * form-interleaved, there will be a modest win.
     */
    private DCInputSet lastInputSet = null;

    /**
     * Parse an XML encoded submission forms template file, and create a hashmap
     * containing all the form information. This hashmap will contain three top
     * level structures: a map between collections and forms, the definition for
     * each page of each form, and lists of pairs of values that populate
     * selection boxes.
     */

   


    


    private void buildInputs(String fileName)
         throws DCInputsReaderException
    {
        users = new HashMap<String, String>();
        
        String uri = "file:" + new File(fileName).getAbsolutePath();

        try
        {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setValidating(false);
                factory.setIgnoringComments(true);
                factory.setIgnoringElementContentWhitespace(true);
                
                DocumentBuilder db = factory.newDocumentBuilder();
                Document doc = db.parse(uri);
                doNodes(doc);
               // checkValues();
        }
        catch (FactoryConfigurationError fe)
        {
                throw new DCInputsReaderException("No puede importar los usuarios",fe);
        }
        catch (Exception e)
        {
                throw new DCInputsReaderException("El archivo xml no esta bien formado: "+e);
        }
    }
   
    public Iterator<String> getPairsNameIterator()
    {
        return valuePairs.keySet().iterator();
    }

    public List<String> getPairs(String name)
    {
        return valuePairs.get(name);
    }

    /**
     * Returns the set of DC inputs used for a particular collection, or the
     * default set if no inputs defined for the collection
     *
     * @param collectionHandle
     *            collection's unique Handle
     * @return DC input set
     * @throws DCInputsReaderException
     *             if no default set defined
     */
    public DCInputSet getInputs(String collectionHandle)
                throws DCInputsReaderException
    {
        String formName = users.get(collectionHandle);
        if (formName == null)
        {
                formName = users.get(DEFAULT_COLLECTION);
        }
        if (formName == null)
        {
                throw new DCInputsReaderException("No form designated as default");
        }
        // check mini-cache, and return if match
        if ( lastInputSet != null && lastInputSet.getFormName().equals( formName ) )
        {
                return lastInputSet;
        }
        // cache miss - construct new DCInputSet
        List<List<Map<String, String>>> pages = formDefns.get(formName);
        if ( pages == null )
        {
                throw new DCInputsReaderException("Missing the " + formName  + " form");
        }
        lastInputSet = new DCInputSet(formName, pages, valuePairs);
        return lastInputSet;
    }
    
    /**
     * Return the number of pages the inputs span for a desginated collection
     * @param  collectionHandle   collection's unique Handle
     * @return number of pages of input
     * @throws DCInputsReaderException if no default set defined
     */
    public int getNumberInputPages(String collectionHandle)
        throws DCInputsReaderException
    {
        return getInputs(collectionHandle).getNumberPages();
    }
    
    /**
     * Process the top level child nodes in the passed top-level node. These
     * should correspond to the collection-form maps, the form definitions, and
     * the display/storage word pairs.
     */
    private void doNodes(Node n)
                throws SAXException, DCInputsReaderException
    {
        if (n == null)
        {
                return;
        }
        Node e = getElement(n);
        NodeList nl = e.getChildNodes();
        int len = nl.getLength();
        boolean foundUser  = false;
        for (int i = 0; i < len; i++)
        {
                Node nd = nl.item(i);
                if ((nd == null) || isEmptyTextNode(nd))
                {
                        continue;
                }
                String tagName = nd.getNodeName();
                if (tagName.equals("user"))
                {
                        processUser(nd);
                        foundUser= true;
                }
        }
        if (!foundUser)
        {
                throw new DCInputsReaderException("No hay Usuarios");
        }
        
    }

    /**
     * Process the form-map section of the XML file.
     * Each element looks like:
     *   <name-map collection-handle="hdl" form-name="name" />
     * Extract the collection handle and form name, put name in hashmap keyed
     * by the collection handle.
     */
    private void processUser(Node e)
        throws SAXException
    {
        
    	NodeList nl = e.getChildNodes();
        int len = nl.getLength();
        String firstname="";
        String lastname ="";
        String email="";
        String password="";
        String sedici_eperson_id="";
        
        
        for (int i = 0; i < len; i++)
        {
                Node nd = nl.item(i);
                
                if (nd.getNodeName().equals("firstname"))
                {
                       firstname = getValue(nd);
                       
                }
                if (nd.getNodeName().equals("lastname"))
                {
                        lastname = getValue(nd);
                       
                }
                if (nd.getNodeName().equals("email"))
                {
                       email = getValue(nd);
                        
                       
                }
                if (nd.getNodeName().equals("password"))
                {
                         password = getValue(nd);
                        
                       
                }
                
                if (nd.getNodeName().equals("sedici_eperson_id"))
                {
                	sedici_eperson_id = getValue(nd);
                        
                       
                }
        }
        try {
			CreateUser cu=new CreateUser();
			cu.createUser(email, firstname, lastname, "", password,sedici_eperson_id);
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			//e1.printStackTrace();
			System.out.println(e1);
		}
    }

   
   

   

    

    

    /**
     * Check that all referenced value-pairs are present
     * and field is consistent
     *
     * Throws DCInputsReaderException if detects a missing value-pair.
     */

    private void checkValues()
                throws DCInputsReaderException
    {
        // Step through every field of every page of every form
        Iterator<String> ki = formDefns.keySet().iterator();
        while (ki.hasNext())
        {
                String idName = ki.next();
                List<List<Map<String, String>>> pages = formDefns.get(idName);
                for (int i = 0; i < pages.size(); i++)
                {
                        List<Map<String, String>> page = pages.get(i);
                        for (int j = 0; j < page.size(); j++)
                        {
                                Map<String, String> fld = page.get(j);
                                // verify reference in certain input types
                                String type = fld.get("input-type");
                    if (type.equals("dropdown")
                            || type.equals("qualdrop_value")
                            || type.equals("list"))
                                {
                                        String pairsName = fld.get(TYPE_NAME);
                                        List<String> v = valuePairs.get(pairsName);
                                        if (v == null)
                                        {
                                                String errString = "Cannot find value pairs for " + pairsName;
                                                throw new DCInputsReaderException(errString);
                                        }
                                }
                                // if visibility restricted, make sure field is not required
                                String visibility = fld.get("visibility");
                                if (visibility != null && visibility.length() > 0 )
                                {
                                        String required = fld.get("required");
                                        if (required != null && required.length() > 0)
                                        {
                                                String errString = "Field '" + fld.get("label") +
                                                                        "' is required but invisible";
                                                throw new DCInputsReaderException(errString);
                                        }
                                }
                        }
                }
        }
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
        //no such attribute
        return null;
    }

    /**
     * Returns the value found in the Text node (if any) in the
     * node list that's passed in.
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
	
	
	
    public static void main(String[] argv)
    	throws Exception
    {
    	CommandLineParser parser = new PosixParser();
    	Options options = new Options();
    	
    	ImportUsers iu = new ImportUsers();
    	
    	options.addOption("i", "importUsers", true, "Archivo de usuarios a importar");
    	
    	CommandLine line = parser.parse(options, argv);
    	BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    	System.out.print("Ingrese el nombre del archivo de usuarios a importar: ");
		System.out.flush();
    	iu.buildInputs(input.readLine());
    	
    	
    }
    
    /** 
     * constructor, which just creates and object with a ready context
     * 
     * @throws Exception
     */
    private ImportUsers()
    	throws  DCInputsReaderException
    {
    	
    }
    
    
    
   
    
}
