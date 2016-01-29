/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.statistics;

import java.util.HashMap;
import java.util.Map;

public abstract class AStatComponentService<T extends IStatsGenericComponent> implements IStatComponentService<T>
{
    public static final String TOP_CONTINENT_LENGTH = "topContinentLength";

    public static final String TOP_COUNTRY_LENGTH = "topCountryLength";

    public static final String TOP_CITY_LENGTH = "topCityLength";

    protected static final Object SHOW_MORE_LENGTH = "showMoreLength";

    public static final String YEARS_QUERY = "yearsQuery";

    public static final String _SELECTED_OBJECT = "selectedObject";

    protected boolean showSelectedObject = true;
    
    protected boolean showExtraTab = true;

    protected Integer topRelation;

    protected static boolean excludeBot = true;

    protected static Integer topCountryLength;

    protected static Integer topContinentLength;

    protected static Integer topCityLength;

    protected static Integer showMoreLength;

    protected static Integer yearsQuery;
    
    public Map getCommonsParams()
    {
        Map params = new HashMap();
        params.put(TOP_CONTINENT_LENGTH, topContinentLength);
        params.put(TOP_COUNTRY_LENGTH, topCountryLength);
        params.put(TOP_CITY_LENGTH, topCityLength);
        params.put(SHOW_MORE_LENGTH, showMoreLength);
        return params;
    }
    
    public boolean isShowSelectedObject()
    {
        return showSelectedObject;
    }

    public void setShowSelectedObject(boolean showSelectedObject)
    {
        this.showSelectedObject = showSelectedObject;
    }

    public Integer getTopRelation()
    {
        return topRelation;
    }

    public void setTopRelation(Integer topRelation)
    {
        this.topRelation = topRelation;
    }

    public static boolean isExcludeBot()
    {
        return excludeBot;
    }

    public void setExcludeBot(boolean excludeBot)
    {
        this.excludeBot = excludeBot;
    }

    public static Integer getTopCountryLength()
    {
        return topCountryLength;
    }

    public void setTopCountryLength(Integer topCountryLength)
    {
        this.topCountryLength = topCountryLength;
    }

    public static Integer getTopContinentLength()
    {
        return topContinentLength;
    }

    public void setTopContinentLength(Integer topContinentLength)
    {
        this.topContinentLength = topContinentLength;
    }

    public static Integer getTopCityLength()
    {
        return topCityLength;
    }

    public void setTopCityLength(Integer topCityLength)
    {
        this.topCityLength = topCityLength;
    }

    public static Integer getShowMoreLength()
    {
        return showMoreLength;
    }

    public void setShowMoreLength(Integer showMoreLength)
    {
        this.showMoreLength = showMoreLength;
    }

    public static Integer getYearsQuery()
    {
        return yearsQuery;
    }

    public void setYearsQuery(Integer yearsQuery)
    {
        this.yearsQuery = yearsQuery;
    }

    public boolean isShowExtraTab()
    {
        return showExtraTab;
    }

    public void setShowExtraTab(boolean showExtraTab)
    {
        this.showExtraTab = showExtraTab;
    }

   


}
