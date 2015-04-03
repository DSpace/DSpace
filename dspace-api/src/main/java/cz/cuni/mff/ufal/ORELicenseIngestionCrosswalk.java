/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom.Element;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

/**
 * UFAL Licenses ingestion crosswalk
 * <p>
 * After successful ORE ingestion of an item, this class will add licensing information in UFAL database
 * based on the metadata information of dc.rights and dc.rights.uri
 *
 * @author Amir Kamran
 */
@SuppressWarnings("deprecation")
public class ORELicenseIngestionCrosswalk implements IngestionCrosswalk{

    /** log4j category */
    private static Logger log = Logger.getLogger(ORELicenseIngestionCrosswalk.class);	
	
	@Override
	public void ingest(Context context, DSpaceObject dso, List<Element> metadata)
			throws CrosswalkException, IOException, SQLException,
			AuthorizeException {

		// If this list contains only the root already, just pass it on
        if (metadata.size() == 1) {
			ingest(context, dso, metadata.get(0));
		}
		// Otherwise, wrap them up 
		else {
			Element wrapper = new Element("wrap", metadata.get(0).getNamespace());
			wrapper.addContent(metadata);

			ingest(context,dso,wrapper);
		}
		
	}

	@Override
	public void ingest(Context context, DSpaceObject dso, Element root)
			throws CrosswalkException, IOException, SQLException,
			AuthorizeException {

		if (dso.getType() != Constants.ITEM)
        {
            throw new CrosswalkObjectNotSupported("OREIngestionCrosswalk can only crosswalk an Item.");
        }
        Item item = (Item)dso;
        
        if (root == null) {
        	System.err.println("The element received by ingest was null");
        	return;
        }
       
        Metadatum[] dc_rights = item.getMetadata("dc", "rights", null, Item.ANY);
        Metadatum[] dc_rights_uri = item.getMetadata("dc", "rights", "uri", Item.ANY);
        
        String licenseName = null;
        String licenseURI = null;
        
        if(dc_rights!=null && dc_rights.length!=0) {
        	licenseName = dc_rights[0].value;
        	licenseURI = dc_rights_uri[0].value;
        } 
                
        IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
        functionalityManager.openSession();
        
        LicenseDefinition license = functionalityManager.getLicenseByDefinition(licenseURI);
        
        /* If license not found we should create one (not yet done)
         * Needs to see the metashare data properly */
        
        if(license != null) {
        	
        	log.info("License found - " + license.getName());
        	
	        Bundle[] bundles = item.getBundles("ORIGINAL");              
	        
	        for(Bundle bundle : bundles){
	        	Bitstream[] bitstreams = bundle.getBitstreams();
				for (Bitstream bitstream : bitstreams) {
					functionalityManager.attachLicense(license.getLicenseId(), bitstream.getID());
				}        	
	        }
        } else {
        	log.info("No license information detected for " + item.getHandle());
        }
        
        functionalityManager.closeSession();
		
	}

}

