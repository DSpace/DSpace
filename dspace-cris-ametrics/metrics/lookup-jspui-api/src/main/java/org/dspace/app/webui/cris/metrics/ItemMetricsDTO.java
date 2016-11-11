/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.metrics;

import java.util.Date;

public class ItemMetricsDTO {
	public String type;
	public Double counter;
	public Double ranking;
	public Integer rankingLev;
	public Double last1;
	public Double last2;
	public String moreLink;
	public Date time;
	private NumberFormatter formatter;
	
	public void setType(String type) {
		this.type = type;
	}
	public void setCounter(Double counter) {
		this.counter = counter;
	}
	public void setRanking(Double ranking) {
		this.ranking = ranking;
	}
	public void setRankingLev(Integer rankingLev) {
		this.rankingLev = rankingLev;
	}
	public void setLast1(Double last1) {
		this.last1 = last1;
	}
	public void setLast2(Double last2) {
		this.last2 = last2;
	}
	public void setMoreLink(String moreLink) {
		this.moreLink = moreLink;
	}
	public String getType() {
		return type;
	}
	public Double getCounter() {
		return counter;
	}
	public Double getRanking() {
		return ranking;
	}
	public Integer getRankingLev() {
		return rankingLev;
	}
	public Double getLast1() {
		return last1;
	}
	public Double getLast2() {
		return last2;
	}
	public String getMoreLink() {
		return moreLink;
	}
	public Date getTime()
    {
        return time;
    }
	public void setTime(Date time)
    {
        this.time = time;
    }
    public NumberFormatter getFormatter()
    {
        return formatter;
    }
    public void setFormatter(NumberFormatter formatter)
    {
        this.formatter = formatter;
    }
}