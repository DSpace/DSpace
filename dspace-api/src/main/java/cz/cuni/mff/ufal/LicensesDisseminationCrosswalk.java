/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.Constants;
import org.jdom.Element;
import org.jdom.Namespace;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseLabel;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

/**
 *
 * @author Michal Sedlak
 */
public class LicensesDisseminationCrosswalk implements DisseminationCrosswalk {
    
    /** log4j category */
    private static Logger log = cz.cuni.mff.ufal.Logger.getLogger(LicensesDisseminationCrosswalk.class);
    
    private static final Namespace namespaces[] = { Namespace.NO_NAMESPACE };
    
    public Namespace[] getNamespaces() {
        return namespaces;
    }

    public String getSchemaLocation() {
        return "";
    }
    
    public boolean canDisseminate(DSpaceObject dso)
    {
        return dso.getType() == Constants.ITEM;
    }

    public boolean preferList() {
        return false;
    }

    public List disseminateList(DSpaceObject dso) throws CrosswalkException, IOException, SQLException, AuthorizeException {
        List result = new ArrayList(1);
        result.add(disseminateElement(dso));
        return result;
    }

    public Element disseminateElement(DSpaceObject dso) throws CrosswalkException, IOException, SQLException, AuthorizeException {
        if (dso.getType() != Constants.ITEM)
            throw new CrosswalkObjectNotSupported("LicensesDisseminationCrosswalk can only crosswalk a Bitstream.");

        //Bitstream bitstream = (Bitstream)dso;
        Element license_el = new Element("license");
        
        Item item = (Item)dso;
        
        Metadatum[] dcValue = item.getMetadata("dc", "rights", "uri", Item.ANY);
        String licenseURI = "";
        
        
        if(dcValue!=null && dcValue.length!=0) {
        	licenseURI = dcValue[0].value;
        } 

        

    	IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
    	functionalityManager.openSession();
    
    	LicenseDefinition license = functionalityManager.getLicenseByDefinition(licenseURI);
    	
    	if(license!=null) {

        	LicenseLabel label = license.getLicenseLabel();
        	
            license_el.setAttribute("label", label.getLabel());
            license_el.setAttribute("label_title", label.getTitle());
            license_el.setAttribute("url", license.getDefinition());
            license_el.setText(license.getName());

            Element labels_el = new Element("labels");
            
            for(LicenseLabel extendedLabel : license.getSortedLicenseLabelExtendedMappings()) {
            
            	Element label_el = new Element("label");
            	label_el.setAttribute("label", extendedLabel.getLabel());
            	label_el.setAttribute("label_title", extendedLabel.getTitle());
            	
            	labels_el.addContent(label_el);
            }            
            
            if(!labels_el.getContent().isEmpty())
            	license_el.addContent(labels_el);
    	}

    	functionalityManager.closeSession();
    	
        return license_el.getContent().isEmpty() ? null : license_el;
    }
}



