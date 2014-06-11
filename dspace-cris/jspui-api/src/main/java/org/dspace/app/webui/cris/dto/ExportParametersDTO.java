/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.dto;

import java.util.Date;
import java.util.List;

public class ExportParametersDTO
{
    private String defaultOperator;        
    
    private Boolean status;
    
    private Date creationStart;
    private Date creationEnd;
    
    private String staffNoStart;
    private String staffNoEnd;
    
    private String rpIdStart;
    private String rpIdEnd;

    private String names;
    private String dept;
    private String interests;
    private String media;
    
    private Boolean advancedSyntax;
    
    private String mainMode;
 
    private List<Integer> tabToExport;
    
    public ExportParametersDTO()
    {
        defaultOperator = "and";
    }
    
    public String getDefaultOperator()
    {
        return defaultOperator;
    }
    public void setDefaultOperator(String defaultOperator)
    {
        this.defaultOperator = defaultOperator;
    }
    public Boolean getStatus()
    {
        return status;
    }
    public void setStatus(Boolean status)
    {
        this.status = status;
    }
    public Date getCreationStart()
    {
        return creationStart;
    }
    public void setCreationStart(Date creationStart)
    {
        this.creationStart = creationStart;
    }
    public Date getCreationEnd()
    {
        return creationEnd;
    }
    public void setCreationEnd(Date creationEnd)
    {
        this.creationEnd = creationEnd;
    }
    public String getStaffNoStart()
    {
        return staffNoStart;
    }
    public void setStaffNoStart(String staffNoStart)
    {
        this.staffNoStart = staffNoStart;
    }
    public String getStaffNoEnd()
    {
        return staffNoEnd;
    }
    public void setStaffNoEnd(String staffNoEnd)
    {
        this.staffNoEnd = staffNoEnd;
    }
    public String getRpIdStart()
    {
        return rpIdStart;
    }
    public void setRpIdStart(String rpIdStart)
    {
        this.rpIdStart = rpIdStart;
    }
    public String getRpIdEnd()
    {
        return rpIdEnd;
    }
    public void setRpIdEnd(String rpIdEnd)
    {
        this.rpIdEnd = rpIdEnd;
    }
    public String getNames()
    {
        return names;
    }
    public void setNames(String names)
    {
        this.names = names;
    }
    public String getDept()
    {
        return dept;
    }
    public void setDept(String dept)
    {
        this.dept = dept;
    }
    public String getInterests()
    {
        return interests;
    }
    public void setInterests(String interests)
    {
        this.interests = interests;
    }
    public String getMedia()
    {
        return media;
    }
    public void setMedia(String media)
    {
        this.media = media;
    }
    public Boolean getAdvancedSyntax()
    {
        return advancedSyntax != null?advancedSyntax:false;
    }
    public void setAdvancedSyntax(Boolean advancedSyntax)
    {
        this.advancedSyntax = advancedSyntax;
    }
    public String getMainMode()
    {
        return mainMode;
    }
    public void setMainMode(String mainMode)
    {
        this.mainMode = mainMode;
    }

	public List<Integer> getTabToExport() {		
		return tabToExport;
	}

	public void setTabToExport(List<Integer> tabToExport) {
		this.tabToExport = tabToExport;
	}
    
    
}
