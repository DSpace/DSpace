/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.sql.SQLException;
import java.util.Map;

import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.embargo.EmbargoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parse.conditions.AddConditionParser;

public class ControlPanelEmbargo extends AbstractControlPanelTab {

    private static Logger log = LoggerFactory.getLogger(ControlPanelEmbargo.class);

	@Override
	public void addBody(Map objectModel, Division div) throws WingException, SQLException 
	{
		ItemIterator item_iter = null;
		try {
			item_iter = EmbargoManager.getEmbargoedItems(context);
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		// header
		Division wfdivmain = div.addDivision("embargoed-items", "well well-light");
		wfdivmain.setHead("EMBARGO ITEMS");

		if ( item_iter == null ) {
			wfdivmain.addPara(null, "alert alert-error").addContent("Could not fetch embargoed items.");
			return;
		}
		
		if(item_iter.hasNext()) {

			// table
			Table wftable = wfdivmain.addTable("workspace_items", 1, 3);
			Row wfhead = wftable.addRow(Row.ROLE_HEADER);
			// table items - because of GUI not all columns could be shown
			wfhead.addCellContent("#");
			wfhead.addCellContent("NAME");
			wfhead.addCellContent("EMBARGO");
			
			int i = 0;
			while ( item_iter.hasNext() )
			{
				Row wsrow = wftable.addRow(Row.ROLE_DATA);
				wsrow.addCell().addContent( String.valueOf(i + 1) );
				
				Item item = item_iter.next();
				String handle = item.getHandle();
				wsrow.addCell().addXref( 
					String.format( "%s/handle/%s", contextPath, handle != null ? handle : "null" ), 
					item.getName() );
				DCDate date = null;
				try {
					date = EmbargoManager.getEmbargoTermsAsDate(context, item);
				} catch (Exception e) {
				}
				wsrow.addCell().addContent( date != null ? date.toString() : "null" );
				i++;
			}
		} else {
			wfdivmain.addPara(null, "alert alert-info").addContent("No embargo items found!");
		}
    }
}





