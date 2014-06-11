/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Type;

/**
 * This class models a single field of the ResearcherPage.
 * Any field contains a value and a visibility flags. 
 * 
 * @author cilea
 *
 */
@Embeddable
@MappedSuperclass
public class RestrictedField implements IRestrictedField, Serializable {
	
	@Type(type = "text")
	/**
	 * the actual content of the field
	 */
	private String value;
		
	/**
	 * the visibility flags of the field
	 */
	private Integer visibility;

	private String authority;
	
    /**
     * Constructor. All the field are by default publicly visible
     */
	public RestrictedField()
    {
	    this.visibility = 1;
    }
	
	/**
     * Getter method.
     * 
     * @return the actual content of the field
     */
	public String getValue() {
		return value;
	}

	/**
     * Setter method.
     * 
     * @param value
     *            the actual content of the field
     */
	public void setValue(String value) {
		this.value = value;
	}

	/**
     * Getter method.
     * 
     * @see VisibilityConstants
     * 
     * @return the visibility flags of the field
     */
    public Integer getVisibility()
    {
        if(visibility==null) {
            return 0;
        }
        return visibility;
    }

    /**
     * Setter method. * @see VisibilityConstants
     * 
     * @param visibility
     *            the visibility flags of the field
     */
    public void setVisibility(Integer visibility)
    {	    
		this.visibility = visibility ==null?new Integer(0):visibility;
	}

    public void setAuthority(String authority)
    {
        this.authority = authority;
    }

    public String getAuthority()
    {
        return authority;
    }
}
