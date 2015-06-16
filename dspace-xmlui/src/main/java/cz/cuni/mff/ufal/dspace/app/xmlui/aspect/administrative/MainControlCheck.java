/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.util.Iterator;
import java.util.Map;

import cz.cuni.mff.ufal.dspace.handle.ConfigurableHandleIdentifierProvider;

import org.dspace.app.xmlui.aspect.administrative.FlowCurationUtils;
import org.dspace.app.xmlui.aspect.administrative.controlpanel.AbstractControlPanelTab;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandleManager;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

import cz.cuni.mff.ufal.dspace.handle.PIDConfiguration;

/**
 */
public class MainControlCheck extends AbstractControlPanelTab {

	protected HtmlHelper html = null;	
	
	@Override
	public void addBody(Map objectModel, Division div) throws WingException {

		div = div.addDivision( this.getClass().getSimpleName(), "control_check well well-light" );
		String siteHandle = ConfigurableHandleIdentifierProvider.completeHandle(
			PIDConfiguration.getDefaultPrefix(), "0");
		div.addHidden("site_handle").setValue(siteHandle);
		
		html = new HtmlHelper( div, web_link );		
		
		// all checks
		html.table( "checks" );
		html.table_header( new String[] { "Checks", null } );

		String checks[] = (new DSpace()).getSingletonService(
			ConfigurationService.class).getPropertyAsType(
			"controlpanel.controlpanel.checks", new String[] {});
		
		for ( String check : checks ) {
			check = check.trim();
			try {
				html.table_add( new String[] { check, "?tab=" + check }, HtmlHelper.header_class.THIS_IS_CHECK );
			} catch (Exception e) {
			}
		}
		
		// all curation
		html.table( "curation" );
		html.table_header( new String[] { "Curation", null } );
		FlowCurationUtils.setupCurationTasks();
        Iterator<String> innerIterator = FlowCurationUtils.allTasks.keySet().iterator();
        while (innerIterator.hasNext())
        {
        	String optionValue = innerIterator.next().trim();
        	String optionText = (String) FlowCurationUtils.allTasks.get(optionValue);
			html.table_add( new String[] { 
				optionText, "POST:"+optionValue}, 
				HtmlHelper.header_class.THIS_IS_CHECK );
        }
		
		html.button( "run-checks btn btn-repository", "Run all checks" );
	}
}




