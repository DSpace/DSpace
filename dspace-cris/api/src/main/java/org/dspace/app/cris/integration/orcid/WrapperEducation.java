package org.dspace.app.cris.integration.orcid;

import org.orcid.jaxb.model.record_v2.Education;

public class WrapperEducation {
    
    Integer id;
    String uuid;
    Integer type;
    Education education;
    
    public Integer getId()
    {
        return id;
    }
    public void setId(Integer id)
    {
        this.id = id;
    }
    public String getUuid()
    {
        return uuid;
    }
    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }
    public Education getEducation()
    {
        return education;
    }
    public void setEducation(Education education)
    {
        this.education = education;
    }
    public Integer getType()
    {
        return type;
    }
    public void setType(Integer type)
    {
        this.type = type;
    }
}