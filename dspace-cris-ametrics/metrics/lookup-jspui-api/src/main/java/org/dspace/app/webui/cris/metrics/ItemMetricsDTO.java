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
}