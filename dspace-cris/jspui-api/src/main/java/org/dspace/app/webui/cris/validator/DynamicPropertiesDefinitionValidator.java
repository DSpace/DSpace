/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.validator;

import it.cilea.osd.common.validation.BaseValidator;
import it.cilea.osd.jdyna.controller.DecoratorPropertiesDefinitionController;
import it.cilea.osd.jdyna.service.ValidatorService.ValidationResult;

import java.util.List;

import org.dspace.app.cris.model.jdyna.DecoratorDynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorRPPropertiesDefinition;
import org.springframework.validation.Errors;

public class DynamicPropertiesDefinitionValidator extends BaseValidator {
	
   	private List<String> messages;
   	
   	private ExtendedValidatorService validatorService;

   	
   	public boolean supports(Class arg0) {
		return getClazz().isAssignableFrom(arg0);
	}
	
	@Override
	public void validate(Object object, Errors errors) {

		DecoratorDynamicPropertiesDefinition metadato = (DecoratorDynamicPropertiesDefinition) object;

		// lo shortname non puo' essere vuoto

		String shortName = metadato.getShortName();

		// validazione shortname...deve essere unico e non nullo e formato solo da caratteri
		// alfabetici da 'a-zA-Z','_' e '-'
		boolean result = (shortName != null) && shortName.matches("^[a-z_\\-A-Z]*$");
				
		if (result && shortName.length()!=0) {

			ValidationResult result2 = null;

			// verifica se e' unica
			// controllo sul db che non ci siano shortname uguali
			result2 = validatorService.checkShortName(
					metadato.getObject().getClass(), metadato.getObject());
			if (!result2.isSuccess())
				errors.rejectValue("shortName", result2.getMessage());

		} else {
			errors.rejectValue("shortName",
					"error.message.validation.shortname.pattern");
		}
	}

	public void setValidatorService(ExtendedValidatorService validatorService) {
		this.validatorService = validatorService;
	}
	

}
