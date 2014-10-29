package org.dspace.app.cris.util;

import org.dspace.app.cris.model.ResearcherPage;

public interface IResearcherPageLabelDecorator {

	String generateDisplayValue(String name, ResearcherPage researcherPage);
}
