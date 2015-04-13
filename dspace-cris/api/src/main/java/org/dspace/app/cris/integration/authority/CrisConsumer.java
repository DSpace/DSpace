/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.cris.integration.authority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.integration.RPAuthority;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.AuthorityValueGenerator;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

/**
 */
public class CrisConsumer implements Consumer {

	private static final Logger log = Logger.getLogger(CrisConsumer.class);

	private SearchService searcher = new DSpace().getServiceManager().getServiceByName(SearchService.class.getName(),
			SearchService.class);

	private ApplicationService applicationService = new DSpace().getServiceManager().getServiceByName(
			"applicationService", ApplicationService.class);

	public void initialize() throws Exception {

	}

	public void consume(Context ctx, Event event) throws Exception {
		DSpaceObject dso = event.getSubject(ctx);
		if (dso instanceof Item) {
			Item item = (Item) dso;

			List<String> listMetadata = ChoiceAuthorityManager.getManager().getAuthorityMetadataForAuthority(
					RPAuthority.RP_AUTHORITY_NAME);

			Map<String, List<DCValue>> toBuild = new HashMap<String, List<DCValue>>();
			Map<String, String> toBuildType = new HashMap<String, String>();

			for (String metadata : listMetadata) {
				DCValue[] dcvalues = item.getMetadata(metadata);
				for (DCValue dcval : dcvalues) {
					String authority = dcval.authority;
					if (StringUtils.isNotBlank(authority)) {
						if (authority.startsWith(AuthorityValueGenerator.GENERATE)) {

							String[] split = StringUtils.split(authority, AuthorityValueGenerator.SPLIT);
							String type = null, info = null;
							if (split.length > 0) {
								type = split[1];
								if (split.length > 1) {
									info = split[2];
								}
							}

							toBuildType.put(info, type);

							List<DCValue> list = new ArrayList<DCValue>();
							if (toBuild.containsKey(info)) {
								list = toBuild.get(info);
								list.add(dcval);
							} else {
								list.add(dcval);
							}
							toBuild.put(info, list);
						}
					}
				}
			}

			Map<String, String> toBuildAuthority = new HashMap<String, String>();

			for (String orcid : toBuild.keySet()) {

				String rpKey = null;

				ResearcherPage rp = applicationService.getEntityBySourceId(toBuildType.get(orcid).toUpperCase(), orcid,
						ResearcherPage.class);

				if (rp != null) {
					rpKey = rp.getCrisID();
				} else {
					SolrQuery query = new SolrQuery();
					query.setQuery("search.resourcetype:9");
					String filterQuery = "";

					if (StringUtils.isNotBlank(orcid)) {
						filterQuery += "crisrp." + toBuildType.get(orcid).toLowerCase() + ":\"" + orcid + "\"";
					}
					query.addFilterQuery(filterQuery);
					QueryResponse qResp = searcher.search(query);
					SolrDocumentList docList = qResp.getResults();
					if (docList.size() > 1) {
						SolrDocument doc = docList.get(0);
						rpKey = (String) doc.getFirstValue("objectpeople_authority");
					}

					if (rpKey == null) {
						// build a simple RP
						rp = new ResearcherPage();

						rp.setSourceID(orcid);
						rp.setSourceRef(toBuildType.get(orcid).toUpperCase());

						ResearcherPageUtils.buildTextValue(rp, toBuild.get(orcid).get(0).value, "fullName");
						// ResearcherPageUtils.buildTextValue(rp, email,
						// "email");
						ResearcherPageUtils.buildTextValue(rp, orcid, "orcid");

						applicationService.saveOrUpdate(ResearcherPage.class, rp);
						rpKey = rp.getCrisID();
					}

				}
				
				if (StringUtils.isNotBlank(rpKey)) {
					toBuildAuthority.put(orcid, rpKey);
				}
			}		
			
			
			for (String orcid : toBuildAuthority.keySet()) {
				for (DCValue dcvalue : toBuild.get(orcid)) {
					DCValue newValue = dcvalue.copy();
					newValue.authority = toBuildAuthority.get(orcid);
					item.replaceMetadataValue(dcvalue, newValue);
					item.update();
				}
			}
		}
	}

	public void end(Context ctx) throws Exception {
		// nothing to do
	}

	public void finish(Context ctx) throws Exception {
		// nothing to do
	}
}