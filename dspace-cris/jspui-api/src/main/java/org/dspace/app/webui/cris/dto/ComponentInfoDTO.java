/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.dto;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.sort.SortOption;

public class ComponentInfoDTO<T extends DSpaceObject>
{
    private String type;

    private String order;

    private int etAl;

    private int rpp;

    private SortOption so;

    private T[] items;

    private int pagecurrent;

    private int pagefirst;

    private int pagelast;
    
    private int pagetotal;

    private long total;

    private int start;

	private int searchTime;

	private String relationName;
	
	private String browseType;

    private String buildCommonURL()
    {

        return "?open=" + type + "&amp;sort_by" + type + "="
                + (so != null ? so.getNumber() : 0) + "&amp;order" + type + "="
                + order + "&amp;rpp" + type + "=" + rpp + "&amp;etal" + type
                + "=" + etAl + "&amp;start" + type + "=";

    }

    private String buildCommonURL(String query,String page){
    	StringBuffer sb = new StringBuffer();
    	sb.append("?");
    	HashMap<String,String> pairs = new HashMap<String,String>();

    	if(StringUtils.isNotBlank(query)){
	    	String[] param = StringUtils.split(query, "&");
	    	for(String p: param){
	    		String[] pair = StringUtils.split(p, "=");
	    		pairs.put(pair[0],pair[1]);
	    	}
    	}
    	pairs.put("start"+type, page);
    	for(String key: pairs.keySet()){
    		sb.append(key+"="+pairs.get(key)+"&");
    	}
    	
    	String url = sb.toString();
    	url = StringUtils.left(url, StringUtils.length(url)-1);
    	return url;
    }
    
    public String buildPrevURL()
    {
        return buildCommonURL() + ((pagecurrent - 2) * rpp);
    }

    public String buildPrevURL(String query)
    {
    	Integer prevPage = (pagecurrent - 2) * rpp;
    	
        return buildCommonURL(query, prevPage.toString());
    }    
    
    public String buildNextURL()
    {
        return buildCommonURL() + (pagecurrent * rpp);
    }

    public String buildNextURL(String query){
    	Integer nextPage = (pagecurrent ) * rpp;
    	return buildCommonURL(query, nextPage.toString());
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getOrder()
    {
        return order;
    }

    public void setOrder(String order)
    {
        this.order = order;
    }

    public int getEtAl()
    {
        return etAl;
    }

    public void setEtAl(int etAl)
    {
        this.etAl = etAl;
    }

    public int getRpp()
    {
        return rpp;
    }

    public void setRpp(int rpp)
    {
        this.rpp = rpp;
    }

    public SortOption getSo()
    {
        return so;
    }

    public void setSo(SortOption so)
    {
        this.so = so;
    }

    public T[] getItems()
    {
        return items;
    }

    public void setItems(T[] items)
    {
        this.items = items;
    }

    public int getPagecurrent()
    {
        return pagecurrent;
    }

    public void setPagecurrent(int pagecurrent)
    {
        this.pagecurrent = pagecurrent;
    }

    public int getPagefirst()
    {
        return pagefirst;
    }

    public void setPagefirst(int pagefirst)
    {
        this.pagefirst = pagefirst;
    }

    public int getPagelast()
    {
        return pagelast;
    }

    public void setPagelast(int pagelast)
    {
        this.pagelast = pagelast;
    }

    public long getTotal()
    {
        return total;
    }

    public void setTotal(long total)
    {
        this.total = total;
    }

    public int getStart()
    {
        return start;
    }

    public void setStart(int start)
    {
        this.start = start;
    }

    public String buildMyLink(int q)
    {

        String myLink = "<a href=\""
                + "?open="
                + type
                + "&amp;sort_by"
                + type
                + "="
                + (so != null ? so.getNumber()
                        : 0) + "&amp;order" + type + "="
                + order + "&amp;rpp" + type + "="
                + rpp + "&amp;etal" + type + "="
                + etAl + "&amp;start" + type
                + "=";

        if (q == pagecurrent)
        {
            myLink =  "<a href=\"#\">" + q + "</a>";
        }
        else
        {
            myLink = myLink + (q - 1) * rpp + "\">" + q + "</a>";
        }
        return myLink;
    }

    public String buildMyLink(String query,int q)
    {

        String myLink = "<a href=\"";
              
        if (q == pagecurrent)
        {
            myLink =  "<a href=\"#\">" + q + "</a>";
        }
        else
        {
        	Integer page = (q-1)*rpp;
            myLink = myLink + buildCommonURL(query, page.toString()) + "\">" + q + "</a>";
        }
        return myLink;
    }
    
    
    public int getPagetotal()
    {
        return pagetotal;
    }

    public void setPagetotal(int pagetotal)
    {
        this.pagetotal = pagetotal;
    }

	public void setSearchTime(int searchTime) {
		this.searchTime = searchTime;
	}

	public int getSearchTime() {
		return searchTime;
	}

	public void setRelationName(String relationName) {
		this.relationName = relationName;
	}

	public String getRelationName() {
		return relationName;
	}

    public String getBrowseType()
    {
        return browseType;
    }

    public void setBrowseType(String browseType)
    {
        this.browseType = browseType;
    }
}
