/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;


import java.beans.PropertyEditor;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.service.ApplicationService;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * This is the base abstract SpringMVC controller for the RPs authority list
 * project. All the other form controller extends this. This abstract controller
 * is responsible to initialize the SpringMVC binder system with the
 * configurated property editors.
 * 
 * @author cilea
 * 
 */
public abstract class BaseFormController extends SimpleFormController {

    /**
     * The log4j category
     */
	protected final Log log = LogFactory.getLog(getClass());

	/**
     * the applicationService for query the RP db, injected by Spring IoC
     */
	protected ApplicationService applicationService;

	/**
	 * the configurated property editors to use, injected by Spring IoC
	 */
	private Map<Class, PropertyEditor> customPropertyEditors;

	
	public void setApplicationService(ApplicationService applicationService) {
		this.applicationService = applicationService;
	}

	@Override
	/**
	 * Register custom property editors injected by Spring IoC
	 */
	protected void initBinder(HttpServletRequest request,
			ServletRequestDataBinder binder) {
		if (customPropertyEditors != null) {
			for (Class propertyClass : customPropertyEditors.keySet()) {
				log.debug("Registrato customEditor "
						+ customPropertyEditors.get(propertyClass).getClass()
						+ " per la tipologia " + propertyClass);
				binder.registerCustomEditor(propertyClass,
						customPropertyEditors.get(propertyClass));
			}
		}

	}

	public void setCustomPropertyEditors(
			Map<Class, PropertyEditor> customPropertyEditors) {
		this.customPropertyEditors = customPropertyEditors;
	}
}
