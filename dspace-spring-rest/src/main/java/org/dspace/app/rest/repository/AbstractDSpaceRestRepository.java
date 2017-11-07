/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.apache.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactoryImpl;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is the base class for any Rest Repository. It provides utility method to
 * access the DSpaceContext
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public abstract class AbstractDSpaceRestRepository {
	
	private static final Logger log = Logger.getLogger(AbstractDSpaceRestRepository.class);
	
	@Autowired
	protected Utils utils;

	protected RequestService requestService = new DSpace().getRequestService();

	protected Context obtainContext() {
		Request currentRequest = requestService.getCurrentRequest();
		Context context = (Context) currentRequest.getAttribute(ContextUtil.DSPACE_CONTEXT);
		if (context != null && context.isValid()) {
			return context;
		}
		context = new Context();
		currentRequest.setAttribute(ContextUtil.DSPACE_CONTEXT, context);
		setUpTestUser(context);
		return context;
	}

	private void setUpTestUser(Context context) {
		boolean runSingleUser = DSpaceServicesFactoryImpl.getInstance().getConfigurationService()
				.getBooleanProperty("run.single.test-user");
		if (runSingleUser) {
			EPerson currentUser = null;
			try {
				currentUser = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context,
						"test-user@mailinator.com");
				if (currentUser == null) {		
					EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
					EPerson eperson;
					try {
						eperson = ePersonService.findByEmail(context, "test-user@mailinator.com");
						if (eperson == null) {
							// This EPerson creation should only happen once
							log.info("Creating initial EPerson (email=test-user@mailinator.com) for Tests");
							eperson = ePersonService.create(context);
							eperson.setFirstName(context, "first");
							eperson.setLastName(context, "last");
							eperson.setEmail("test-user@mailinator.com");
							eperson.setCanLogIn(true);
							eperson.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
							// actually save the eperson
							ePersonService.update(context, eperson);
							context.commit();
						}
						currentUser = eperson;
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			context.setCurrentUser(currentUser);
		}
	}

	public RequestService getRequestService() {
		return requestService;
	}
}
