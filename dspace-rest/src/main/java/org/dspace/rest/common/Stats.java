/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;
import javax.ws.rs.WebApplicationException;

import org.apache.log4j.Logger;
import org.dspace.core.Constants;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.content.DatasetTimeGenerator;

@XmlRootElement(name = "histogram")
public class Stats {
	private org.dspace.rest.common.StatsData data;
	private Long sum=0L;
	/** log4j category */
    private static final Logger log = Logger.getLogger(Stats.class);
    
    private enum TYPE {VIEW,DOWNLOAD};
	
	public Stats(){
		
	}
	
	public Stats(String type, org.dspace.content.DSpaceObject dso, String startDate, String endDate, String interval) throws Exception{
		String solrQuery="";
		TYPE typeInt;
		String idQuery="";
		if(interval==null) {
			interval = "month";
		}
		if(!(interval.trim().toLowerCase().compareTo("day")==0 || interval.trim().toLowerCase().compareTo("month")==0 || interval.trim().toLowerCase().compareTo("year")==0)){
			throw new WebApplicationException(new Exception("interval " +interval +" not supported"), 501);
		}
				
		if(type.toLowerCase().compareTo("view")==0){
			if(dso!=null){
				idQuery=" AND id:" + dso.getID();
			}
			typeInt=TYPE.VIEW;
			solrQuery="type:2 AND isBot:false" +idQuery;
		}
		else if(type.toLowerCase().compareTo("download")==0){
			if(dso!=null){
				if(dso.getType()==Constants.BITSTREAM){
					idQuery=" AND id:"+dso.getID();
				} else if(dso.getType()==Constants.ITEM){
					idQuery=" AND owningItem:" + dso.getID();
				}
			}
			typeInt=TYPE.DOWNLOAD;
			solrQuery="type:0 AND isBot:false"+idQuery;
		} else {
			data=new StatsData();
			Stats view = new Stats ("view", dso, startDate, endDate, interval);
			Stats download = new Stats("download", dso, startDate, endDate, interval);
			data.setViews(view.getData().getViews());
			data.setDownloads(download.getData().getDownloads());
			sum=view.getQueryCount()+download.getQueryCount();
			return;
		}
		int max =0;
		ObjectCount count = SolrLogger.queryTotal(solrQuery, null);
		sum =count.getCount();
		
		Date start;
		Date end;
		
		if(startDate!=null && endDate!=null){
			start=parseDate(startDate);
			end=parseDate(endDate);
		} else {
			end = new Date();
			Calendar c = Calendar.getInstance(); 
			c.setTime(end); 
			c.add(Calendar.MONTH, -3);
			start= c.getTime();
		}
		
		log.debug("dates " + start + " " + end);
		
		DatasetTimeGenerator dtg = new DatasetTimeGenerator();
		dtg.setDateInterval(interval, start, end);
		
		ObjectCount[] returnvalues= SolrLogger.queryFacetDateISO(solrQuery, null, max, dtg.getDateType(), dtg.getStartDate(), dtg.getEndDate(), true);
		log.debug("retunrvalues " + returnvalues.length);
		data = new StatsData();
		if(typeInt==TYPE.VIEW){
			data.setViews(returnvalues);
			sum=data.getTotalViews();
		} else if(typeInt==TYPE.DOWNLOAD){
			data.setDownloads(returnvalues);
			sum=data.getTotalDownloads();
		}
	}

	public Stats(String type, ArrayList<Integer> items, String startDate, String endDate, String interval) throws Exception{
		String solrQuery="";
		TYPE typeInt;
		ArrayList<String> idQueryList=new ArrayList<String>();
		String idQuery ="";
		int counter =0;
		if(interval==null) {
			interval = "month";
		}

		
		if(!(interval.trim().toLowerCase().compareTo("day")==0 || interval.trim().toLowerCase().compareTo("month")==0 || interval.trim().toLowerCase().compareTo("year")==0)){
			throw new WebApplicationException(new Exception("interval " +interval +" not supported"), 501);
		}
				
		if(type.toLowerCase().compareTo("view")==0){
			if(items!=null && items.size()>0){
				idQuery=idQuery.concat(" AND (");
				for(Integer itemId : items){
					idQuery=idQuery.concat(" OR id:" + itemId);
					counter++;
					if(counter>=100){
						idQuery=idQuery.replaceFirst(" OR ", "");
						idQuery=idQuery.concat(")");
						idQueryList.add(idQuery);
						idQuery =" AND (";
						counter=0;
					}
					
				}
				idQuery=idQuery.replaceFirst(" OR ", "");
				idQuery=idQuery.concat(")");
				idQueryList.add(idQuery);
			}
			typeInt=TYPE.VIEW;
			solrQuery="type:2 AND isBot:false" ;
		}
		else if(type.toLowerCase().compareTo("download")==0){
			if(items!=null && items.size()>0){
				idQuery=idQuery.concat(" AND (");
				for(Integer itemId : items){
					idQuery=idQuery.concat(" OR owningItem:" + itemId);
					counter++;
					if(counter>=100){
						idQuery=idQuery.replaceFirst(" OR ", "");
						idQuery=idQuery.concat(")");
						idQueryList.add(idQuery);
						idQuery =" AND (";
						counter=0;
					}
					
				}
				idQuery=idQuery.replaceFirst(" OR ", "");
				idQuery=idQuery.concat(")");
				idQueryList.add(idQuery);
			}
			typeInt=TYPE.DOWNLOAD;
			solrQuery="type:0 AND isBot:false";
		} else {
			data=new StatsData();
			Stats view = new Stats ("view", items, startDate, endDate, interval);
			Stats download = new Stats("download", items, startDate, endDate, interval);
			data.setViews(view.getData().getViews());
			data.setDownloads(download.getData().getDownloads());
//			sum=view.getQueryCount()+download.getQueryCount();
			sum=data.getTotalViews()+data.getTotalDownloads();
			return;
		}
		
		int max =0;
//		for(String idQ : idQueryList){
//			log.debug("query : " + solrQuery +idQ);
//			ObjectCount count = SolrLogger.queryTotal(solrQuery + idQ, null);
//			sum +=count.getCount();
//		}
		
		Date start;
		Date end;
		
		if(startDate!=null && endDate!=null){
			start=parseDate(startDate);
			end=parseDate(endDate);
		} else {
			end = new Date();
			Calendar c = Calendar.getInstance(); 
			c.setTime(end); 
			c.add(Calendar.MONTH, -3);
			start= c.getTime();
		}
		
		
		DatasetTimeGenerator dtg = new DatasetTimeGenerator();
		dtg.setDateInterval(interval, start, end);
		if(data==null){
			data = new StatsData();
		}
		for(String idQ : idQueryList){
			ObjectCount[] returnvalues= SolrLogger.queryFacetDateISO(solrQuery+ idQ, null, max, dtg.getDateType(), dtg.getStartDate(), dtg.getEndDate(), true);
			if(typeInt==TYPE.VIEW){
				data.addViews(returnvalues);
			} else if(typeInt==TYPE.DOWNLOAD){
				data.addDownloads(returnvalues);
			}
			
		}
		if(typeInt==TYPE.VIEW){
			sum=data.getTotalViews();
		} else if(typeInt==TYPE.DOWNLOAD){
			sum=data.getTotalDownloads();
		}
	}

	
	private Date parseDate(String datestring) throws Exception {
		SimpleDateFormat sdf;
		Date date;
		log.debug("length " + datestring.trim().length());
		switch(datestring.trim().length()){
			case 19:
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				break;
			case 10:
				sdf = new SimpleDateFormat("yyyy-MM-dd");
				break;
			case 28:
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
				break;
			case 20:
				sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); 
				break;
			default:
				throw new Exception("unsupported Format");
		}
		
		date = sdf.parse(datestring);
		return date;
	}

	public org.dspace.rest.common.StatsData getData() {
		return data;
	}


	public void setData(org.dspace.rest.common.StatsData data) {
		this.data = data;
	}


	public Long getQueryCount() {
		return sum;
	}


	public void setQueryCount(Long queryCount) {
		this.sum = queryCount;
	}

}
