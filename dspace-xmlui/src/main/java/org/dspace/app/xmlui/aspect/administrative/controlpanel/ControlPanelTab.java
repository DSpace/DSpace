/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.controlpanel;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * Public interface for admin ControlPanel tabs. This interface should be implemented by any class
 * which defines a "tab" in the ControlPanel
 * @author LINDAT/CLARIN dev team (http://lindat.cz)
 */
public interface ControlPanelTab {
	
	public void addBody(Map objectModel, Division div) throws SAXException, WingException,
    						UIException, SQLException, IOException, AuthorizeException;

}

