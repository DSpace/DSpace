/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.util;

import javax.persistence.Transient;

import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.SolrLogger;

public class StatsConfig {
    
    @Transient
    public static final int DETAILS_SECTION = -1;

    @Transient
    public static final int DOWNLOAD_CV_SECTION = -2;

    @Transient
    public static final int COLLABORATION_NETWORK_SECTION = -3;
    
	private String statisticsCore;
	
	public String getStatisticsCore() {
	    if(statisticsCore==null) {
	        statisticsCore = ConfigurationManager.getProperty(SolrLogger.CFG_STAT_MODULE, "server");
	    }
		return statisticsCore;
	}

	public void setStatisticsCore(String statisticsCore) {
		this.statisticsCore = statisticsCore;
	}
	
}
