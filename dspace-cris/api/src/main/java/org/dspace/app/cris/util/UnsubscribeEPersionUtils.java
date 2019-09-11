/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.util;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.CrisSubscribeService;
import org.dspace.app.cris.statistics.StatSubscriptionViewBean;
import org.dspace.app.cris.statistics.service.StatSubscribeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscribe;
import org.dspace.utils.DSpace;

public class UnsubscribeEPersionUtils {
	/** log4j logger */
	private static Logger log = Logger.getLogger(UnsubscribeEPersionUtils.class);
	
	/**
	 * Unsubscribe an eperson from all the notification.
	 * 
	 * @param context The context
	 * @param email The mail related to an ePerson
	 * @param skipEmail if true skip sending the summary email.
	 * @throws Exception
	 */
	public static void process(
			Context context, String email, boolean skipEmail) throws Exception {
				
		EPerson eperson = EPerson.findByEmail(context, email);
		if (eperson == null) {
			throw new Exception("The email " + email + " is not related to any ePerson.");
		}
		
		log.debug("Using email template: unsubscriptions");
		Email recoverEmail = Email.getEmail(I18nUtil.getEmailFilename(Locale.getDefault(), "unsubscriptions"));
		String recipient = ConfigurationManager.getProperty("alert.recipient");
		String notify = ConfigurationManager.getProperty("registration.notify");
		String mailTo = "";
		if (StringUtils.isNotBlank(recipient)) {
			log.debug("Using alert.recipient: " + recipient);

			recoverEmail.addRecipient(recipient);
			mailTo = recipient;
		}
		else if (StringUtils.isNotBlank(notify)) {
			log.debug("Using registration.notify: " + recipient);
			
			recoverEmail.addRecipient(notify);
			mailTo = notify;
		}
		else if (!skipEmail) {
			throw new Exception("No administration email configurated. Fix configuration value alert.recipient or registration.notify.");
		}
		log.debug("{0}: " + email);
		recoverEmail.addArgument(email);			// {0}

		StringBuilder list = new StringBuilder();
		// get subscription to community
		Community[] communities = Subscribe.getCommunitySubscriptions(context, eperson);
		if (communities != null && communities.length > 0) {
			for (Community c : communities) {
				list.append("\t" + c.getName() + ", with handle " + c.getHandle() + "\n");
				
				Subscribe.unsubscribe(context, eperson, c);
			}
		}
		log.debug("unsubscribe communities: {1}: " + list.toString());
		recoverEmail.addArgument(list.toString());	// {1}
		
		list.setLength(0);
		// get subscription to collection
		Collection[] collections = Subscribe.getSubscriptions(context, eperson);
		if (collections != null && collections.length > 0) {
			for (Collection c : collections) {
				list.append("\t" + c.getName() + ", handle " + c.getHandle() + "\n");
				
				Subscribe.unsubscribe(context, eperson, c);
			}
		}
		log.debug("unsubscribe collections: {2}: " + list.toString());
		recoverEmail.addArgument(list.toString());	// {2}
		
		list.setLength(0);
		// get cris object subscriptions
		DSpace dspace = new DSpace();
		ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
				ApplicationService.class);
		CrisSubscribeService crisUnsubscribeService = new CrisSubscribeService(applicationService);
		List<String> crisObjUuids = crisUnsubscribeService.getSubscriptions(eperson);
		if (crisObjUuids != null && crisObjUuids.size() > 0) {
			for (String uuid : crisObjUuids) {
				ACrisObject o = applicationService.getEntityByUUID(uuid);
				list.append("\t" + o.getName() + ", handle " + o.getHandle() + ", id " + o.getCrisID() + "\n");
				
				crisUnsubscribeService.unsubscribe(eperson, uuid);
			}
		}
		log.debug("unsubscribe cris Objects: {3}: " + list.toString());
		recoverEmail.addArgument(list.toString());	// {3}
		
		list.setLength(0);
		// get statistical subscriptioons
		StatSubscribeService statUnsibscribeService = new StatSubscribeService(applicationService);
		List<StatSubscriptionViewBean> statSubViewBeans = statUnsibscribeService.getSubscriptions(context, eperson);
		if (statSubViewBeans != null && statSubViewBeans.size() > 0) {
			for (StatSubscriptionViewBean s : statSubViewBeans) {
				StringBuilder freq = new StringBuilder();
				int len = 0;
				for (Integer f : s.getFreqs()) {
					String fasS = Integer.toString(f);
					
					freq.ensureCapacity(len + fasS.length() + 2);
					if (len > 0)
						freq.append(" ,");
					freq.append(fasS);
					len += fasS.length() + 2;
				}
				
				String label = "";
				String type = "";
				switch (s.getType()) {
					case Constants.ITEM:
						label = "handle";
						type = "type item";
						break;
						
					case Constants.COLLECTION:
						label = "handle";
						type = "type colllection";
						break;
						
					case Constants.COMMUNITY:
						label = "handle";
						type = "type community";
						break;
						
					default:
						label = "uuid";
						
						ACrisObject criso = applicationService.getEntityByUUID(s.getId());
						type = "handle " + criso.getHandle() + ", id " + criso.getCrisID();
						break;
				}
				list.append("\t" + s.getObjectName() + ", frequences (" + freq 
						+ "), " + label + " " + s.getId() 
						+ ", " + type + "(" + s.getType() + ")\n");

				statUnsibscribeService.unsubscribeUUID(eperson, s.getId());
			}
		}
		log.debug("unsubscribe statistics: {4}: " + list.toString());
		recoverEmail.addArgument(list.toString());	// {4}
		
		if (!skipEmail) {
			recoverEmail.send();
			
			log.info("summary email sent to " + mailTo);
		}
		else {
			log.info("skip sending the summary email");
		}
	}
}
