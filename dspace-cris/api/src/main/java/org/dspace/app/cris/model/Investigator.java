/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import javax.persistence.Embeddable;
import javax.persistence.OneToOne;

@Embeddable
public class Investigator implements IRestrictedField {
	
	private String extInvestigator;
	private String role;
	
	@OneToOne
	private ResearcherPage intInvestigator;

	public ResearcherPage getIntInvestigator() {
		return intInvestigator;
	}

	public void setIntInvestigator(ResearcherPage intInvestigator) {
		this.intInvestigator = intInvestigator;
	}

	public void setExtInvestigator(String extInvestigator) {
		this.extInvestigator = extInvestigator;
	}

	public String getExtInvestigator() {
		return extInvestigator;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getRole() {
		return role;
	}

    @Override
    public Integer getVisibility()
    {        
        return 1;
    }

    @Override
    public String getValue()
    {
        if(getIntInvestigator()!=null) {
            return getIntInvestigator().getFullName();
        }
        return extInvestigator;
    }

    @Override
    public void setVisibility(Integer visibility)
    {
        // TODO Auto-generated method stub        
    }

    @Override
    public void setValue(String value)
    {
        // TODO Auto-generated method stub
        
    }
	
	
}
