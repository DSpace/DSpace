/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.cocoon.ProcessingException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseLabel;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

public class LicensePage extends AbstractDSpaceTransformer {
	
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

	
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
    	pageMeta.addMetadata("title").addContent("Available Licenses");
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent("Available Licenses");
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException, ProcessingException {
	  	IFunctionalities licenseManager = DSpaceApi.getFunctionalityManager();
	  	licenseManager.openSession();
  		List<LicenseDefinition> license_defs = licenseManager.getAllLicenses();
  		
    	Division division = body.addDivision("licenses");
    	
    	division.setHead("Available Licenses");
  		
  		for (LicenseDefinition license_def : license_defs) {
  			org.dspace.app.xmlui.wing.element.List license = division.addList("license_" + license_def.getID());
  			license.addItem("name", null).addContent(license_def.getName());
  			license.addItem("definition", null).addContent(license_def.getDefinition());
  			
  			org.dspace.app.xmlui.wing.element.List license_details = license.addList("license_details", org.dspace.app.xmlui.wing.element.List.TYPE_GLOSS);
  			license_details.addLabel("Source");
  			license_details.addItem("definition", null).addXref(license_def.getDefinition(), license_def.getDefinition());
  			license_details.addLabel("Labels");
  			Item labels = license_details.addItem("labels", null);
  			labels.addHighlight("label label-" + license_def.getLicenseLabel().getLabel()).addContent(license_def.getLicenseLabel().getTitle());
  			
  			for(LicenseLabel label : license_def.getLicenseLabelExtendedMappings()) {
  				labels.addHighlight("label label-default").addContent(label.getTitle());
  			}
  			
  			license_details.addLabel("Extra Information Required");
  			String requiredInfo = license_def.getRequiredInfo();
  			if(requiredInfo!=null) {
  				Item extra = license_details.addItem("extra", null);
				for(String info : requiredInfo.split(",")) {
					info.trim();
					extra.addHighlight("label label-default").addContent(info);
				}  				
  			} else {
  				license_details.addItem("NONE");
  			}
  			
    	}
  		licenseManager.closeSession();
    }

}


