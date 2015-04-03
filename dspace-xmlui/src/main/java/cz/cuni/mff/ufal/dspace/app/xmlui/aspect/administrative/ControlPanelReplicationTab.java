/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;
	
import java.util.Map;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;

public class ControlPanelReplicationTab extends AbstractControlPanelTab {
	
	private Request request;

	@Override
	public void addBody(Map objectModel, Division mainDiv) throws WingException {
		
		request = ObjectModelHelper.getRequest(objectModel);

		Division div = mainDiv.addDivision("replication_div");
		
		div.setHead("Replication Service");
		
		ControlPanelReplicationTabHelper.showTabs(div, request, context);		

	}			
	
}

