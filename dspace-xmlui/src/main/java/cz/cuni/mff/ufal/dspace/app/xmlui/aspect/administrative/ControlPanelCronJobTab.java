/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Scanner;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.core.ConfigurationManager;


/**
 * 
 */
public class ControlPanelCronJobTab extends AbstractControlPanelTab {

	private Request request = null;
	private static Logger log = Logger.getLogger(ControlPanelCronJobTab.class);

		
	@Override
	public void addBody(Map objectModel, Division div) throws WingException {

		request = ObjectModelHelper.getRequest(objectModel);
        final String cron_user = ConfigurationManager.getProperty("lr", "lr.cron.user");
        List cron = div.addList("cronjobs", List.TYPE_FORM);
                
        cron.setHead(String.format("Current cron jobs for [%s]", cron_user));
               
        String filename = String.format("/etc/cron.d/%s", cron_user);
        String contents = "";
                
        try {
			contents = new Scanner(new File(filename)).useDelimiter("\\Z").next();
			cron.addItem(null, "alert alert-info linebreak").addContent(contents);
		} catch (FileNotFoundException e) {
			cron.addItem(null, "alert alert-danger").addContent("Unable to read the file. " + e.getMessage());
			log.error(e);
		}
                
    }

}




