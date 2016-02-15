/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.scopus.services;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.metrics.scopus.dto.ScopusResponse;
import org.dspace.core.ConfigurationManager;

public class ScopusService {

	/** log4j category */
	private static final Logger log = Logger.getLogger(ScopusService.class);

	private CloseableHttpClient client = null;

	private int maxNumberOfTries;
	private long sleepBetweenTimeouts;
	private long sleepBetweenEachCall;
	private int timeout;

	public ScopusService() {
		HttpClientBuilder custom = HttpClients.custom();
		// httpclient 4.3+ doesn't appear to have any sensible defaults any
		// more. Setting conservative defaults as not to hammer the Scopus
		// service too much.
		client = custom.disableAutomaticRetries().setMaxConnTotal(5)
				.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(timeout).build()).build();

	}

	/**
	 * Abstract Citation Count API: This represents a retrieval of citation
	 * counts for specific documents (SCOPUS content) as a branded image.
	 * Performs a search based upon the identifiers provided and returns the
	 * cited-by count of the document(s) returned as a SCOPUS-branded image or
	 * as textual metadata (JSON, XML). Note that each category is considered a
	 * distinct resource and access restrictions may be applicable.
	 * 
	 * @param activateSleep
	 * @param pmids
	 * @param dois
	 * @param eids
	 * @return
	 */
	public ScopusResponse getCitations(boolean activateSleep, List<String> pmids, List<String> dois,
			List<String> eids) {

		if (activateSleep) {
			try {
				Thread.sleep(sleepBetweenEachCall);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		String endpoint = ConfigurationManager.getProperty("cris", "ametrics.elsevier.scopus.endpoint");
		String apiKey = ConfigurationManager.getProperty("cris", "ametrics.elsevier.scopus.apikey");

		HttpGet method = null;
		ScopusResponse scopusResponse = null;
		int numberOfTries = 0;

		while (numberOfTries < maxNumberOfTries && scopusResponse == null) {
			numberOfTries++;
			try {
				Thread.sleep(sleepBetweenTimeouts * (numberOfTries - 1));

				URIBuilder uriBuilder = new URIBuilder(endpoint);
				String query = "";
				if (pmids != null && pmids.size() > 0) {
					String pmid = pmids.get(0);
					if (StringUtils.isNotBlank(pmid)) {
						query += "PMID(" + pmid + ")";
					}
				}
				if (dois != null && dois.size() > 0) {
					String doi = dois.get(0);
					if (StringUtils.isNotBlank(doi)) {
						if (StringUtils.isNotBlank(query)) {
							query += " OR ";
						}
						query += "DOI(" + doi + ")";
					}
				}
				if (eids != null && eids.size() > 0) {
					String eid = eids.get(0);
					if (StringUtils.isNotBlank(eid)) {
						if (StringUtils.isNotBlank(query)) {
							query += " OR ";
						}
						query += "EID(" + eid + ")";
					}					
				}
				uriBuilder.addParameter("query", query);

				method = new HttpGet(uriBuilder.build());

				method.addHeader("Accept", "application/xml");
				method.addHeader("X-ELS-APIKey", apiKey);

				// Execute the method.
				HttpResponse response = client.execute(method);
				int statusCode = response.getStatusLine().getStatusCode();
				HttpEntity responseBody = response.getEntity();

				if (statusCode != HttpStatus.SC_OK) {
					scopusResponse = new ScopusResponse("Scopus return not OK status: " + statusCode, ConstantMetrics.STATS_INDICATOR_TYPE_ERROR);
				} else if (null != responseBody) {
                    if (log.isDebugEnabled())
                    {                      
                        log.debug(responseBody.getContent());
                    }
					scopusResponse = new ScopusResponse(responseBody.getContent());
				} else {
					scopusResponse = new ScopusResponse("Scopus returned no response", ConstantMetrics.STATS_INDICATOR_TYPE_ERROR);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return scopusResponse;
	}

	public void setMaxNumberOfTries(int maxNumberOfTries) {
		this.maxNumberOfTries = maxNumberOfTries;
	}

	public void setSleepBetweenTimeouts(long sleepBetweenTimeouts) {
		this.sleepBetweenTimeouts = sleepBetweenTimeouts;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public long getSleepBetweenEachCall() {
		return sleepBetweenEachCall;
	}

	public void setSleepBetweenEachCall(long sleepBetweenEachCall) {
		this.sleepBetweenEachCall = sleepBetweenEachCall;
	}
}
