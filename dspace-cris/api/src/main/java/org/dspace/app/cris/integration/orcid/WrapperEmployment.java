package org.dspace.app.cris.integration.orcid;

import org.dspace.authority.orcid.jaxb.employment.Employment;

public class WrapperEmployment
{
    Integer id;

    String uuid;

    Integer type;

    Employment employment;

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

    public Employment getEmployment()
    {
        return employment;
    }

    public void setEmployment(Employment employment)
    {
        this.employment = employment;
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
