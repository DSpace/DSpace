/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.wos.services;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.CharsetUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.metrics.wos.dto.WosResponse;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.StreamGenericDisseminationCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.PluginManager;

public class WosService {

	/** log4j category */
	private static final Logger log = Logger.getLogger(WosService.class);

	private CloseableHttpClient client = null;

	private int maxNumberOfTries;
	private long sleepBetweenTimeouts;
	private long sleepBetweenEachCall;
	private int timeout;

	public WosService() {
		HttpClientBuilder custom = HttpClients.custom();
		// httpclient 4.3+ doesn't appear to have any sensible defaults any
		// more. Setting conservative defaults as not to hammer the Scopus
		// service too much.
		client = custom.disableAutomaticRetries().setMaxConnTotal(5)
				.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(timeout).build()).build();

	}

	/**
	 * Contact the Thomson Reuters Article Match Retrieval Service
	 * 
	 * @param activateSleep
	 * @param pmids
	 * @param dois
	 * @param isis
	 * @return
	 */
	public WosResponse getCitations(Context context, boolean activateSleep, List<DSpaceObject> items) {
		log.info(LogManager.getHeader(context, "getCitations",
                "Retrieving citations from WOS for " + items.size() + " items"));
		if (activateSleep) {
			try {
				Thread.sleep(sleepBetweenEachCall);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		String endpoint = ConfigurationManager.getProperty("cris", "ametrics.thomsonreuters.wos.endpoint");

		HttpPost method = null;
		WosResponse wosResponse = null;
		int numberOfTries = 0;

		while (numberOfTries < maxNumberOfTries && wosResponse == null) {
			numberOfTries++;
			try {
				Thread.sleep(sleepBetweenTimeouts * (numberOfTries - 1));

				URIBuilder uriBuilder = new URIBuilder(endpoint);

				final StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) PluginManager
						.getNamedPlugin(StreamDisseminationCrosswalk.class, "wos");

				File file = File.createTempFile("tmp-wos-", ".xml");
				FileOutputStream outputStream = new FileOutputStream(file);
				try {
					((StreamGenericDisseminationCrosswalk) streamCrosswalkDefault).disseminate(context, items,
							outputStream);
				} catch (CrosswalkException e) {
					log.error(e.getMessage(), e);
				}

				method = new HttpPost(uriBuilder.build());

				MultipartEntityBuilder builder = MultipartEntityBuilder.create();

				ContentType contentType = ContentType.create("text/xml", CharsetUtils.get("UTF-8"));
				FileBody fileBody = new FileBody(file, contentType);

				builder.addPart("wos-application.xml", fileBody);

				method.setEntity(new FileEntity(file, contentType));


				// Execute the method.
				HttpResponse response = client.execute(method);
		        
				int statusCode = response.getStatusLine().getStatusCode();
				HttpEntity responseBody = response.getEntity();

				if (statusCode != HttpStatus.SC_OK) {
					wosResponse = new WosResponse("WOS return not OK status: " + statusCode, ConstantMetrics.STATS_INDICATOR_TYPE_ERROR);
				} else if (null != responseBody) {
                    if (log.isDebugEnabled())
                    {                      
                        log.debug(responseBody.getContent());
                    }
					wosResponse = new WosResponse(responseBody.getContent());
				} else {
					wosResponse = new WosResponse("WOS returned no response", ConstantMetrics.STATS_INDICATOR_TYPE_ERROR);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return wosResponse;
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
