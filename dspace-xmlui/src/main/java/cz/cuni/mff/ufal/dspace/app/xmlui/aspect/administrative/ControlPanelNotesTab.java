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
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.core.ConfigurationManager;

/**
 * 
 */
public class ControlPanelNotesTab extends AbstractControlPanelTab {

	private Request request = null;
	private static Logger log = Logger.getLogger(ControlPanelNotesTab.class);

	@Override
	public void addBody(Map objectModel, Division div) throws WingException {

		request = ObjectModelHelper.getRequest(objectModel);
		List rn = div.addList("releasenotes", List.TYPE_FORM);
		final String rnfile = ConfigurationManager.getProperty("lr", "lr.releasenotes");

		if (null == rnfile) {
			// conf is not present
			rn.setHead("Release notes present in configuration.");
		} else {
			rn.setHead("Release notes from " + rnfile);
			if (!new java.io.File(rnfile).isFile() || 0 == new java.io.File(rnfile).length()) {
				// does not exist
				rn.addItem(null, "alert alert-danger").addContent("File " + rnfile + " does not exist!");
			} else {
				try {
					String contents = new Scanner(new File(rnfile)).useDelimiter("\\Z").next();
					rn.addItem(null, "alert alert-info linebreak").addContent(contents);
				} catch (FileNotFoundException e) {
					rn.addItem(null, "alert alert-danger").addContent("Unable to read file.");
					log.equals(e);
				}
			}
		}

	}

}

