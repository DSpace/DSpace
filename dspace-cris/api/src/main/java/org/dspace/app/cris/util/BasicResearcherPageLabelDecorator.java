package org.dspace.app.cris.util;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedField;
import org.dspace.app.cris.model.VisibilityConstants;

public class BasicResearcherPageLabelDecorator implements IResearcherPageLabelDecorator {

	@Override
	public String generateDisplayValue(String alternativeName, ResearcherPage rp) {
		 if (alternativeName.equals(rp.getFullName()))
	        {
	            RestrictedField translatedName = rp.getTranslatedName();
	            return rp.getFullName()
	                    + (translatedName != null
	                            && translatedName.getValue() != null
	                            && !translatedName.getValue().isEmpty()
	                            && translatedName.getVisibility() == VisibilityConstants.PUBLIC ? " "
	                            + translatedName.getValue()
	                            : "");
	        }
	        else
	        {
	            return alternativeName + " See \"" + rp.getFullName() + "\"";
	        }
	}

}
