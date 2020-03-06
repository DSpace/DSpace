/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.net.MalformedURLException;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.authority.AuthoritySearchService;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.xoai.util.ItemUtils;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;

public class OrcidVirtualElementAdditional implements XOAIItemCompilePlugin {

	private static final Logger log = Logger.getLogger(OrcidVirtualElementAdditional.class);
	
	private String metadata = "dc.contributor.author";

	private AuthoritySearchService authorityService;

	@Override
	public Metadata additionalMetadata(Context context, Metadata metadata, Item item) {
		Element personElement = ItemUtils.create("person");
		Element identifierElement = ItemUtils.create("identifier");
		Element orcidElement = ItemUtils.create("orcid");
		Element noneElement = ItemUtils.create("none");

		StringTokenizer dcf = new StringTokenizer(getMetadata(), ".");

		String[] tokens = { "", "", "" };
		int i = 0;
		while (dcf.hasMoreTokens()) {
			tokens[i] = dcf.nextToken().trim();
			i++;
		}
		String schema = tokens[0];
		String element = tokens[1];
		String qualifier = tokens[2];

		Metadatum[] values;
		if ("*".equals(qualifier)) {
			values = item.getMetadata(schema, element, Item.ANY, Item.ANY);
		} else if ("".equals(qualifier)) {
			values = item.getMetadata(schema, element, null, Item.ANY);
		} else {
			values = item.getMetadata(schema, element, qualifier, Item.ANY);
		}
		for (Metadatum val : values) {
			if (StringUtils.isNotBlank(val.authority)) {
				SolrQuery queryArgs = new SolrQuery();
				queryArgs.setQuery("id:" + val.authority);
				queryArgs.addFilterQuery("authority_type:orcid");
				queryArgs.addField("orcid_id");
				queryArgs.setRows(1);
				QueryResponse searchResponse;
				try {
					searchResponse = getAuthorityService().search(queryArgs);
					SolrDocumentList docs = searchResponse.getResults();

					if (docs.getNumFound() == 1) {
						String orcid = null;
						try {
							orcid = (String) docs.get(0).getFieldValue("orcid_id");
						} catch (Exception e) {
							log.error("couldn't get field value for key orcid_id", e);
						}
						if(StringUtils.isNotBlank(orcid)) {
							noneElement.getField().add(ItemUtils.createValue("value", orcid));
							noneElement.getField().add(ItemUtils.createValue("authority", val.authority));
							noneElement.getField().add(ItemUtils.createValue("confidence", "" + val.confidence));
						}
					}
				} catch (MalformedURLException | SolrServerException e1) {
					log.error(e1.getMessage(), e1);
				}

			}
		}
		orcidElement.getElement().add(noneElement);
		identifierElement.getElement().add(orcidElement);
		personElement.getElement().add(identifierElement);
		metadata.getElement().add(personElement);
		return metadata;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	public AuthoritySearchService getAuthorityService() {
		return authorityService;
	}

	public void setAuthorityService(AuthoritySearchService authorityService) {
		this.authorityService = authorityService;
	}
}
