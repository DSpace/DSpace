/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network.dto;


public class DTOMetric
{
    private String authority;
    private String fullName;
    private String type;    
    private String numbersConnections;
    private String maxStrength;
    private String averageStrength;
    private String quadraticVariance;
    public String getAuthority()
    {
        return authority;
    }
    public void setAuthority(String authority)
    {
        this.authority = authority;
    }
    public String getFullName()
    {
        return fullName;
    }
    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
    }
    public String getNumbersConnections()
    {
        return numbersConnections;
    }
    public void setNumbersConnections(String numbersConnections)
    {
        this.numbersConnections = numbersConnections;
    }
    public String getMaxStrength()
    {
        return maxStrength;
    }
    public void setMaxStrength(String maxStrength)
    {
        this.maxStrength = maxStrength;
    }
    public String getAverageStrength()
    {
        return averageStrength;
    }
    public void setAverageStrength(String averageStrength)
    {
        this.averageStrength = averageStrength;
    }
    public String getQuadraticVariance()
    {
        return quadraticVariance;
    }
    public void setQuadraticVariance(String quadraticVariance)
    {
        this.quadraticVariance = quadraticVariance;
    }
    

}
