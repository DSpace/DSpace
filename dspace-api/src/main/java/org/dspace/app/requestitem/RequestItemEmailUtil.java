/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;

/**
 * Utility class to centralize logic around who should receive
 * the request for the item copy
 * 
 * @author Sam Ottenhoff
 * 
 */

public class RequestItemEmailUtil {
	
	private static Logger log = Logger.getLogger(RequestItemEmailUtil.class);

	public static String getSubmitterOrHelpdeskEmail(Item item) {
		boolean helpdeskOverridesSubmitter = ConfigurationManager.getBooleanProperty("request.item.helpdesk.override", false);
		String helpDeskEmail = ConfigurationManager.getProperty("mail.helpdesk");
		
		if (helpdeskOverridesSubmitter && StringUtils.isNotBlank(helpDeskEmail)) {
			return helpDeskEmail;
		}
		else if (helpdeskOverridesSubmitter) {
			return ConfigurationManager.getProperty("mail.admin");
		}
		else {
			EPerson submitter;
			try {
				submitter = item.getSubmitter();
				if (StringUtils.isNotBlank(submitter.getEmail())) {
					return submitter.getEmail();
				}
			} catch (SQLException e) {
				log.error(e.getMessage());
			}
			
			if (StringUtils.isNotBlank(helpDeskEmail)) {
				return helpDeskEmail;
			}
			
			return ConfigurationManager.getProperty("mail.admin");
			
		}
	}
		
}