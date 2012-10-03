package org.dspace.content.crosswalk;

import org.dspace.core.SelfNamedPlugin;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.authorize.AuthorizeException;
import org.jdom.Element;
import org.apache.log4j.Logger;

import java.util.*;
import java.io.*;
import java.sql.SQLException;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 20-jan-2010
 * Time: 10:58:39
 *
 * A dissemination crosswalk to go from dublin core to dryad dublin core
 */
public class DDCCrosswalk extends SelfNamedPlugin implements IngestionCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DDCCrosswalk.class);

    // map of qdc to JDOM Element
    private Map<String, String> ddc2element = new HashMap<String, String>();

    // map of JDOM Element to qdc DCValue
    private Map<String, String> element2ddc = new HashMap<String, String>();
    private boolean init = false;

    private void init() throws IOException, CrosswalkInternalException {
        if(init)
            return;
        init = true;


        // grovel DSpace configuration for namespaces
        // read properties
        String parent = ConfigurationManager.getProperty("dspace.dir") +
            File.separator + "config" + File.separator;
        File propsFile = new File(parent, "crosswalks" +  File.separator + "DDC.properties");
        Properties ddcProps = new Properties();
        FileInputStream pfs = null;
        try
        {
            pfs = new FileInputStream(propsFile);
            ddcProps.load(pfs);
        } finally {
            if (pfs != null)
                try {
                    pfs.close();
                } catch (IOException ioe){
                    //Ignore }
                }
        }

        Enumeration pe = ddcProps.propertyNames();
        while (pe.hasMoreElements())
        {
            String dc = (String)pe.nextElement();
            String ddc = ddcProps.getProperty(dc);
            ddc2element.put(ddc, dc);
            element2ddc.put(dc, ddc);
            log.debug("Building Maps: ddc=\""+ dc +"\", element=\""+ddc +"\"");
        }

    }


    public void ingest(Context context, DSpaceObject dso, List metadata) throws CrosswalkException, IOException, SQLException, AuthorizeException {
        init();
        List<Element> elements = metadata;
        Element wrapper = new Element("wrap",elements.get(0).getNamespace());
        wrapper.addContent(elements);

        ingest(context,dso,wrapper);
    }

    public void ingest(Context context, DSpaceObject dso, Element root) throws CrosswalkException, IOException, SQLException, AuthorizeException {
        init();

        if (dso.getType() != Constants.ITEM)
            throw new CrosswalkObjectNotSupported("DIMIngestionCrosswalk can only crosswalk an Item.");

        if (root == null) {
        	System.err.println("The element received by ingest was null");
        	return;
        }

        Item item = (Item) dso;

        List<Element> metadata = root.getChildren();
        for (Element element : metadata) {
            String ddc = element2ddc.get(element.getNamespacePrefix() + "." + element.getName());
            if(ddc != null){
                item.addMetadata(ddc.split("\\.")[0], ddc.split("\\.")[1], null, null, element.getValue());
            }
        }
    }
}
