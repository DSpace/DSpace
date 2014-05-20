/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package ar.edu.unlp.sedici.dspace.administer;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.app.util.DCInputsReaderException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public final class ImportUsers
{

    /**
     * Procesa el XML con la informacion de los usuarios a importar y los inserta en DSpace
     */
    private void buildInputs(String fileName) throws DCInputsReaderException
    {
        String uri = "file:" + new File(fileName).getAbsolutePath();

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			factory.setIgnoringElementContentWhitespace(true);

			DocumentBuilder db = factory.newDocumentBuilder();
			Document doc = db.parse(uri);
			doNodes(doc);
		} catch (FactoryConfigurationError fe) {
			throw new DCInputsReaderException("No puede importar los usuarios", fe);
		} catch (Exception e) {
			throw new DCInputsReaderException("El archivo xml no esta bien formado: " + e);
		}
    }
   
    /**
     * Procesa el primer nivel de elementos del XML. Esto son los elementos &lt;user&gt;
     */
    private void doNodes(Node n) throws SAXException, DCInputsReaderException
    {
		if (n == null) {
			return;
		}
		Node e = getElement(n);
		NodeList nl = e.getChildNodes();
		int len = nl.getLength();
		boolean foundUser = false;
		for (int i = 0; i < len; i++) {
			Node nd = nl.item(i);
			if ((nd == null) || isEmptyTextNode(nd)) {
				continue;
			}
			String tagName = nd.getNodeName();
			if (tagName.equals("user")) {
				processUser(nd);
				foundUser = true;
			}
		}
		if (!foundUser) {
			throw new DCInputsReaderException("No hay Usuarios para procesar en el archivo");
		}
    }

    /**
     * Recibe un nodo correspondiente a un elemento &lt;user&gt; y recorre sus hijos para 
     * recuperar los datos de un usuario. Luego invoca in creacion del usuario con estos datos.
     */
    private void processUser(Node e) throws SAXException
    {
		NodeList nl = e.getChildNodes();
		int len = nl.getLength();
		String firstname = "";
		String lastname = "";
		String email = "";
		String password = "";
		String groupName = "";
		String sedici_eperson_id = "";

		for (int i = 0; i < len; i++) {
			Node nd = nl.item(i);

			if (nd.getNodeName().equals("firstname")) {
				firstname = getValue(nd);
			} else if (nd.getNodeName().equals("lastname")) {
				lastname = getValue(nd);
			} else if (nd.getNodeName().equals("email")) {
				email = getValue(nd);
			} else if (nd.getNodeName().equals("password")) {
				password = getValue(nd);
			} else if (nd.getNodeName().equals("sedici_eperson_id")) {
				sedici_eperson_id = getValue(nd);
			} else if (nd.getNodeName().equals("group")) {
				groupName = getValue(nd);
			}
		}
		
		try {
			CreateUser cu = new CreateUser();
			cu.createUser(email, firstname, lastname, "", password,	sedici_eperson_id, groupName);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			// e1.printStackTrace();
			System.out.println(e1);
		}
    }

    public static void main(String[] argv) throws Exception
    {
    	// Parseamos los parametros
    	CommandLineParser parser = new PosixParser();
    	Options options = new Options();
    	
    	options.addOption("f", "file", true, "Archivo de usuarios a importar");
    	CommandLine line = parser.parse(options, argv);

    	// Verificamos que se proporciono el nombre del archivo a procesar
    	if(!line.hasOption("f")) {
    		System.err.println("El parametro -f es obligatorio");
    		System.exit(1);
    	}
    	
    	String filename = line.getOptionValue("f");

    	// Iniciamos el procesamiento
    	ImportUsers iu = new ImportUsers();
		iu.buildInputs(filename);
    }
    
    /** 
     * constructor, which just creates and object with a ready context
     * 
     * @throws Exception
     */
    private ImportUsers() throws  DCInputsReaderException
    {
    	
    }
    
    /* Metodos para la manipulacion del XML */
    
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

}
