/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.io.File;
import java.util.Map;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.core.ConfigurationManager;

import cz.cuni.mff.ufal.dspace.IOUtils;

public class ControlPanelOAITab extends AbstractControlPanelTab {
	
	private Request request = null;
	private static Logger log = Logger.getLogger(ControlPanelPIDTab.class);
	private static final String base_url = ConfigurationManager.getProperty("dspace.baseUrl") + "/oai/request";
	private static final String dspace_dir = ConfigurationManager.getProperty("dspace.dir");
	
	@Override
	public void addBody(Map objectModel, Division div) throws WingException {

		request = ObjectModelHelper.getRequest(objectModel);
        
    	String ret ="";
    	
		List l = div.addList("control_panel-oai");
		l.addLabel("Base OAI-PMH url");
		l.addItemXref(base_url, base_url);
		l.addLabel();
		Item i = l.addItem();
		Button b = i.addButton("validate-oval");
		b.setValue("Validate using OVAL");

        l.addLabel();
		i = l.addItem();
		i.addXref("http://validator.oaipmh.com/", "http://validator.oaipmh.com/", "target_blank");

        if ( request.getParameter("validate-oval") != null) 
        {
	    	ret += String.format("Trying [%s]\n", base_url);
	    	ret += IOUtils.run( new File(dspace_dir+"/bin/"), 
	    			new String[] {"python", "./validators/oai_pmh/validate.py", base_url} );
			div.addDivision("oval-validation-result", "alert alert-info").addPara(ret);	    	
        }
		
	}

}

