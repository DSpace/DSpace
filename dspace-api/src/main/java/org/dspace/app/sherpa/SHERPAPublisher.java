/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa;

import java.util.List;

/**
 * POJO representation for a SHERPA Publisher record
 * 
 * @author Andrea Bollini
 * 
 */
public class SHERPAPublisher
{
    private String name;

    private String alias;

    private String homeurl;

    private String prearchiving;

    private List<String> prerestriction;

    private String postarchiving;

    private List<String> postrestriction;

    private String pubarchiving;

    private List<String> pubrestriction;
    
    private List<String> condition;

    private String paidaccessurl;

    private String paidaccessname;

    private String paidaccessnotes;

    private List<String[]> copyright;

    private String romeocolour;

    private String dateadded;

    private String dateupdated;

    public SHERPAPublisher(String name, String alias, String homeurl,
            String prearchiving, List<String> prerestriction,
            String postarchiving, List<String> postrestriction,
            String pubarchiving, List<String> pubrestriction,
            List<String> condition, String paidaccessurl,
            String paidaccessname, String paidaccessnotes,
            List<String[]> copyright, String romeocolour, String datedded,
            String dateupdated)
    {
        this.name = name;

        this.alias = alias;

        this.homeurl = homeurl;

        this.prearchiving = prearchiving;

        this.prerestriction = prerestriction;

        this.postarchiving = postarchiving;

        this.postrestriction = postrestriction;

        this.pubarchiving = pubarchiving;

        this.pubrestriction = pubrestriction;

        this.condition = condition;

        this.paidaccessurl = paidaccessurl;

        this.paidaccessname = paidaccessname;

        this.paidaccessnotes = paidaccessnotes;

        this.copyright = copyright;

        this.romeocolour = romeocolour;

        this.dateadded = datedded;

        this.dateupdated = dateupdated;
    }

    public String getName()
    {
        return name;
    }

    public String getAlias()
    {
        return alias;
    }

    public String getHomeurl()
    {
        return homeurl;
    }

    public String getPrearchiving()
    {
        return prearchiving;
    }

    public List<String> getPrerestriction()
    {
        return prerestriction;
    }

    public String getPostarchiving()
    {
        return postarchiving;
    }

    public List<String> getPostrestriction()
    {
        return postrestriction;
    }

    public String getPubarchiving()
    {
        return pubarchiving;
    }
    
    public List<String> getPubrestriction()
    {
        return pubrestriction;
    }
    
    public List<String> getCondition()
    {
        return condition;
    }

    public String getPaidaccessurl()
    {
        return paidaccessurl;
    }

    public String getPaidaccessname()
    {
        return paidaccessname;
    }

    public String getPaidaccessnotes()
    {
        return paidaccessnotes;
    }

    public List<String[]> getCopyright()
    {
        return copyright;
    }

    public String getRomeocolour()
    {
        return romeocolour;
    }

    public String getDatedded()
    {
        return dateadded;
    }

    public String getDateupdated()
    {
        return dateupdated;
    }

}
