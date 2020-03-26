/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.v1;

import java.util.List;
import java.util.Map;

/**
 * POJO representation for a SHERPA Publisher record
 * 
 * @author Andrea Bollini
 * 
 */
@Deprecated
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

    private Map<String, List<String>> conditionMap;

    public SHERPAPublisher() {

    }

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

    public Map<String, List<String>> getConditionMap() {
        return this.conditionMap;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setHomeurl(String homeurl) {
        this.homeurl = homeurl;
    }

    public void setPrearchiving(String prearchiving) {
        this.prearchiving = prearchiving;
    }

    public void setPrerestriction(List<String> prerestriction) {
        this.prerestriction = prerestriction;
    }

    public void setPostarchiving(String postarchiving) {
        this.postarchiving = postarchiving;
    }

    public void setPostrestriction(List<String> postrestriction) {
        this.postrestriction = postrestriction;
    }

    public void setPubarchiving(String pubarchiving) {
        this.pubarchiving = pubarchiving;
    }

    public void setPubrestriction(List<String> pubrestriction) {
        this.pubrestriction = pubrestriction;
    }

    public void setCondition(List<String> condition) {
        this.condition = condition;
    }

    public void setPaidaccessurl(String paidaccessurl) {
        this.paidaccessurl = paidaccessurl;
    }

    public void setPaidaccessname(String paidaccessname) {
        this.paidaccessname = paidaccessname;
    }

    public void setPaidaccessnotes(String paidaccessnotes) {
        this.paidaccessnotes = paidaccessnotes;
    }

    public void setCopyright(List<String[]> copyright) {
        this.copyright = copyright;
    }

    public void setRomeocolour(String romeocolour) {
        this.romeocolour = romeocolour;
    }

    public void setDateadded(String dateadded) {
        this.dateadded = dateadded;
    }

    public void setDateupdated(String dateupdated) {
        this.dateupdated = dateupdated;
    }

    public void setConditionMap(Map<String, List<String>> conditionMap) {
        this.conditionMap = conditionMap;
    }
}
