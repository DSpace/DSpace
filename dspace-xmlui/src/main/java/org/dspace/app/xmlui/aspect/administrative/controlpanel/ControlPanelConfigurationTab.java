/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.controlpanel;

import java.util.Map;

import org.dspace.app.util.Util;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.DatabaseManager;

/**
 * Control panel tab that displays important configuration.
 * Based on the original ControlPanel class by Jay Paz and Scott Phillips
 * @author LINDAT/CLARIN dev team (http://lindat.cz)
 *
 */
public class ControlPanelConfigurationTab extends AbstractControlPanelTab {

	private static final Message T_DSPACE_HEAD = message("xmlui.administrative.ControlPanel.dspace_head");
	private static final Message T_DSPACE_DIR = message("xmlui.administrative.ControlPanel.dspace_dir");
	private static final Message T_DSPACE_URL = message("xmlui.administrative.ControlPanel.dspace_url");
	private static final Message T_DSPACE_HOST_NAME = message("xmlui.administrative.ControlPanel.dspace_hostname");
	private static final Message T_DSPACE_NAME = message("xmlui.administrative.ControlPanel.dspace_name");
	private static final Message T_DSPACE_VERSION = message("xmlui.administrative.ControlPanel.dspace_version");
	private static final Message T_DB_NAME = message("xmlui.administrative.ControlPanel.db_name");
	private static final Message T_DB_URL = message("xmlui.administrative.ControlPanel.db_url");
	private static final Message T_DB_DRIVER = message("xmlui.administrative.ControlPanel.db_driver");
	private static final Message T_DB_MAX_CONN = message("xmlui.administrative.ControlPanel.db_maxconnections");
	private static final Message T_DB_MAX_WAIT = message("xmlui.administrative.ControlPanel.db_maxwait");
	private static final Message T_DB_MAX_IDLE = message("xmlui.administrative.ControlPanel.db_maxidle");
	private static final Message T_MAIL_SERVER = message("xmlui.administrative.ControlPanel.mail_server");
	private static final Message T_MAIL_FROM_ADDRESS = message("xmlui.administrative.ControlPanel.mail_from_address");
	private static final Message T_FEEDBACK_RECIPIENT = message("xmlui.administrative.ControlPanel.mail_feedback_recipient");
	private static final Message T_MAIL_ADMIN = message("xmlui.administrative.ControlPanel.mail_admin");

	private static final String T_UNSET = "UNSET";

    /**
     * Guarantee a non-null String.
     *
     * @param value candidate string.
     * @return {@code value} or a constant indicating an unset value.
     */
    private static String notempty(String value) { return (null == value || "".equals(value)) ? T_UNSET : value; }

	@Override
	public void addBody(Map objectModel, Division div) throws WingException {
		// LIST: DSpace
		List dspace = div.addList("dspace");
		dspace.setHead(T_DSPACE_HEAD);

		dspace.addLabel(T_DSPACE_VERSION);
		dspace.addItem(Util.getSourceVersion());

		dspace.addLabel(T_DSPACE_DIR);
		dspace.addItem(notempty(ConfigurationManager.getProperty("dspace.dir")));

		dspace.addLabel(T_DSPACE_URL);
		String base_url = notempty(ConfigurationManager.getProperty("dspace.url"));
		dspace.addItemXref(base_url, base_url);

		dspace.addLabel(T_DSPACE_HOST_NAME);
		dspace.addItem(notempty(ConfigurationManager.getProperty("dspace.hostname")));

		dspace.addLabel(T_DSPACE_NAME);
		dspace.addItem(notempty(ConfigurationManager.getProperty("dspace.name")));

		dspace.addLabel(T_DB_NAME);
		dspace.addItem(notempty(DatabaseManager.getDbName()));

		dspace.addLabel(T_DB_URL);
		dspace.addItem(notempty(ConfigurationManager.getProperty("db.url")));

		dspace.addLabel(T_DB_DRIVER);
		dspace.addItem(notempty(ConfigurationManager.getProperty("db.driver")));

		dspace.addLabel(T_DB_MAX_CONN);
		dspace.addItem(notempty(ConfigurationManager.getProperty("db.maxconnections")));

		dspace.addLabel(T_DB_MAX_WAIT);
		dspace.addItem(notempty(ConfigurationManager.getProperty("db.maxwait")));

		dspace.addLabel(T_DB_MAX_IDLE);
		dspace.addItem(notempty(ConfigurationManager.getProperty("db.maxidle")));

		dspace.addLabel(T_MAIL_SERVER);
		dspace.addItem(notempty(ConfigurationManager.getProperty("mail.server")));

		dspace.addLabel(T_MAIL_FROM_ADDRESS);
		dspace.addItem(notempty(ConfigurationManager.getProperty("mail.from.address")));

		dspace.addLabel(T_FEEDBACK_RECIPIENT);
		dspace.addItem(notempty(ConfigurationManager.getProperty("feedback.recipient")));

		dspace.addLabel(T_MAIL_ADMIN);
		dspace.addItem(notempty(ConfigurationManager.getProperty("mail.admin")));
	}

}

