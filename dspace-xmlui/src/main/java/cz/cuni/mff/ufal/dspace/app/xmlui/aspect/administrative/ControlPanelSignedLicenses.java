/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseResourceUserAllowance;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserMetadata;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserRegistration;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

public class ControlPanelSignedLicenses extends AbstractControlPanelTab 
{
	
	private static Logger log = Logger.getLogger(ControlPanelSignedLicenses.class);
	private static final int MAX_TO_SHOW = 50;
	
	//
	//
	//

	@Override
	public void addBody(Map objectModel, Division div) throws WingException, SQLException 
	{
		Division div_main = div.addDivision("unpublished_items", "well well-light");

		// header
		div_main.setHead(String.format("SIGNED LICENSES (showing only first #%d)", MAX_TO_SHOW) );
		
		create_table( div_main );

    }

	
	private Table create_table(Division div) throws WingException 
	{
	    String baseURL = contextPath+"/admin/epeople?";
		// table
		Table wftable = div.addTable("workspace_items", 1, 4, "table-condensed");
		
		try {
			Row wfhead = wftable.addRow("", Row.ROLE_HEADER, "font_smaller");

			// table items - because of GUI not all columns could be shown
            wfhead.addCellContent("ID");
            wfhead.addCellContent("DATE");
			wfhead.addCellContent("USERNAME");
			wfhead.addCellContent("LICENSE");
			wfhead.addCellContent("ITEM");
			wfhead.addCellContent("BITSTREAM");
			wfhead.addCellContent("EXTRA METADATA");
			
			IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
			functionalityManager.openSession();
			java.util.List<LicenseResourceUserAllowance> licenses = functionalityManager.getSignedLicensesByDate();
			
			// hack for group by /////////
			
			/* we don't need group by now
			 * 
			List<String> keys = new ArrayList<String>();
			List<LicenseResourceUserAllowance> licenses_group_by = new ArrayList<LicenseResourceUserAllowance>();
			for (LicenseResourceUserAllowance license : licenses) {
				String createdOn = DateFormatUtils.format(license.getCreatedOn(), "ddMMyyyyhhmm");
				String epersonID = "" + license.getUserRegistration().getEpersonId();
				String licenseID = "" + license.getLicenseResourceMapping().getLicenseDefinition().getLicenseId();
				String key = createdOn + ":" + epersonID + ":" + licenseID;
				if(!keys.contains(key)) {
					keys.add(key);
					licenses_group_by.add(license);
				}
				if(licenses_group_by.size()>=MAX_TO_SHOW) break;
			} */
			
			/////////////////////////////
			
			int cnt = 1;
			for (LicenseResourceUserAllowance license : licenses) 
			{				
				int bitstreamID = license.getLicenseResourceMapping().getBitstreamId();
				LicenseDefinition ld = license.getLicenseResourceMapping().getLicenseDefinition();
                UserRegistration ur = license.getUserRegistration();
				Date signingDate = license.getCreatedOn();
				
                Row r = wftable.addRow(null, null, "font_smaller bold");
                String id = DateFormatUtils.format(signingDate, "yyyyMMddhhmmss")
                		+ "-" + ur.getEpersonId()
                		+ "-" + bitstreamID; 
                r.addCellContent(id);
                
                r.addCellContent(DateFormatUtils.format(signingDate, "yyyy-MM-dd hh:mm:ss"));
                
                String eperson_id = String.format("%d [%s]", ur.getEpersonId(), ur.getEmail() );
                String url = baseURL+"submit_edit&epersonID="+ur.getEpersonId();
                r.addCell().addXref(url, eperson_id);                
                
                r.addCell().addXref(ld.getDefinition(), ld.getName());
                                
                Bitstream bitstream = Bitstream.find(context, bitstreamID);
                Item item = (Item)bitstream.getParentObject();

                String base = ConfigurationManager.getProperty("dspace.url");
    			StringBuffer itemLink = new StringBuffer().append(base)
						  .append(base.endsWith("/") ? "" : "/")
						  .append("/handle/")
						  .append(item.getHandle());
    			
                r.addCell().addXref(itemLink.toString(), "" + item.getID());
                
    			StringBuffer bitstreamLink = new StringBuffer().append(base)
							  .append(base.endsWith("/") ? "" : "/")
							  .append("bitstream/handle/")
							  .append(item.getHandle())
							  .append("/")
							  .append(URLEncoder.encode(bitstream.getName(), "UTF8"))					
							  .append("?sequence=").append(bitstream.getSequenceID());                
                r.addCell().addXref(bitstreamLink.toString(), "" + bitstream.getID());
                
                Cell c = r.addCell();
                List<UserMetadata> extraMetaData = functionalityManager.getUserMetadata_License(ur.getEpersonId(), license.getTransactionId());
                for(UserMetadata metadata : extraMetaData) {
                	c.addHighlight("label label-info font_smaller").addContent(metadata.getMetadataKey() + ": " +metadata.getMetadataValue());
                }
                
                if(++cnt > MAX_TO_SHOW) {
                	break;
                }
			}
			
			functionalityManager.closeSession();
			
		}catch( IllegalArgumentException e1 ) {
			wftable.setHead( "No items - " + e1.getMessage() );
		}catch( Exception e2 ) {
			wftable.setHead( "Exception - " + e2.toString() );
		}
		
		return wftable;
	}
}