/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.validator;

import it.cilea.osd.common.validation.BaseValidator;
import it.cilea.osd.jdyna.service.ValidatorService.ValidationResult;
import it.cilea.osd.jdyna.web.Box;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.cris.model.jdyna.VisibilityTabConstant;
import org.springframework.validation.Errors;

public class BoxValidator extends BaseValidator {
	
   	private List<String> messages;
   	
   	private ExtendedValidatorService validatorService;


   	public boolean supports(Class arg0) {
		return getClazz().isAssignableFrom(arg0);
	}
	
	
	@Override
	public void validate(Object object, Errors errors) {

		Box metadato = (Box) object;

		// lo shortname non puo' essere vuoto

		String shortName = metadato.getShortName();
		if(shortName!=null && shortName.startsWith("edit")) {
			errors.rejectValue("shortName",
			"error.message.validation.shortname.pattern.nostartedit");
		}
		// validazione shortname...deve essere unico e non nullo e formato solo da caratteri
		// alfabetici da 'a-zA-Z','_' e '-'
		boolean result = (shortName != null) && shortName.matches("^[a-z_\\-A-Z]*$");
				
		if (result && shortName.length()!=0) {

			ValidationResult result2 = null;

			// verifica se e' unica
			// controllo sul db che non ci siano shortname uguali
			result2 = validatorService.checkShortName(
					object.getClass(), metadato);
			if (!result2.isSuccess())
				errors.rejectValue("shortName", result2.getMessage());

		} else {
			errors.rejectValue("shortName",
					"error.message.validation.shortname.pattern");
		}
		
	      if(VisibilityTabConstant.POLICY == metadato.getVisibility()) {
	            boolean authS = false;
	            boolean authG = false;
	            metadato.getAuthorizedSingle().removeAll(Collections.singleton(null));
	            if(CollectionUtils.isEmpty(metadato.getAuthorizedSingle())) {
	                authS = true;
	            }
	            metadato.getAuthorizedGroup().removeAll(Collections.singleton(null));
	            if(CollectionUtils.isEmpty(metadato.getAuthorizedGroup())) {
	                authG = true;
	            }
	            if(authS && authG) {
	                errors.rejectValue("visibility",
	                        "error.message.validation.policy.mandatory");
	            }
	        }
	}

	public void setValidatorService(ExtendedValidatorService validatorService) {
		this.validatorService = validatorService;
	}
	

}
