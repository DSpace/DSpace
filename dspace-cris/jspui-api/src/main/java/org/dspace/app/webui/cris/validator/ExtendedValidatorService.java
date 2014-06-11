/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.validator;

import org.dspace.app.cris.service.ExtendedTabService;

import it.cilea.osd.jdyna.service.IPersistenceDynaService;
import it.cilea.osd.jdyna.service.ValidatorService;
import it.cilea.osd.jdyna.web.Box;
import it.cilea.osd.jdyna.web.Tab;

public class ExtendedValidatorService extends ValidatorService {

	

	public ExtendedValidatorService(IPersistenceDynaService applicationService) {
		super(applicationService);
	}

	
	/** 
	 * Shortname tab validation
	 *  
	 * @param clazz
	 * @param object
	 * @return
	 */
	public ValidationResult controllaShortName(Class clazz,Tab object) {
		if(object.getId()==null || !(((ExtendedTabService)getApplicationService()).exist(object.getClass(), object.getId()))) {
			if(((ExtendedTabService)getApplicationService()).getTabByShortName(clazz, object.getShortName())!=null)
				return new ValidationResult("error.message.validation.shortname",false,"Error");
		}
		return new ValidationResult();
	}
	
	/** 
	 * Shortname box validation
	 *  
	 * @param clazz
	 * @param object
	 * @return
	 */
	public ValidationResult checkShortName(Class clazz,Box object) {
		if(object.getId()==null || !(((ExtendedTabService)getApplicationService()).exist(object.getClass(), object.getId()))) {
			if(((ExtendedTabService)getApplicationService()).getBoxByShortName(clazz, object.getShortName())!=null)
				return new ValidationResult("error.message.validation.shortname",false,"Error");
		}
		return new ValidationResult();
	}
}
