/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.authority.orcid.jaxb.common.ExternalId;
import org.dspace.authority.orcid.jaxb.common.ExternalIds;
import org.dspace.authority.orcid.jaxb.work.Citation;
import org.dspace.authority.orcid.jaxb.work.CitationType;
import org.dspace.authority.orcid.jaxb.work.Work;
import org.dspace.authority.orcid.jaxb.work.WorkTitle;
import org.dspace.authority.orcid.jaxb.work.WorkType;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.restlet.resource.ResourceException;

public class OrcidTest {
	
	private static Logger log = Logger.getLogger(OrcidTest.class);
	
	public static void main(String[] args)
			throws CrosswalkException, IOException, SQLException, AuthorizeException, ResourceException, JAXBException {

		Context context = new Context();

		OrcidService orcid = OrcidService.getOrcid();
		Work work = new Work();

		Item item = Item.find(context, 211);

		final StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) PluginManager
				.getNamedPlugin(StreamDisseminationCrosswalk.class, "bibtex");

		OutputStream outputStream = new ByteArrayOutputStream();
		streamCrosswalkDefault.disseminate(context, item, outputStream);
		String citationFromBTE = outputStream.toString();

		Citation citation = new Citation();
		citation.setCitationType(CitationType.BIBTEX);
		citation.setCitationValue(citationFromBTE);

		WorkTitle title = new WorkTitle();
		title.setTitle(item.getName());
		work.setTitle(title);
		work.setCitation(citation);
		work.setType(WorkType.BOOK);

		ExternalIds externalIdentifiers = new ExternalIds();
		// WorkExternalIdentifier externalIdentifier = new
		// WorkExternalIdentifier();
		// externalIdentifier.setWorkExternalIdentifierId(StringEscapeUtils.escapeXml("<![CDATA["+item.getHandle()+"]]>"));
		// externalIdentifier.setWorkExternalIdentifierType("﻿handle");
		// externalIdentifiers.getWorkExternalIdentifier().add(externalIdentifier);

		ExternalId externalIdentifier1 = new ExternalId();
		externalIdentifier1.setExternalIdValue("" + item.getID());
		externalIdentifier1.setExternalIdType("source-work-id");
		externalIdentifiers.getExternalId().add(externalIdentifier1);

		work.setExternalIds(externalIdentifiers);

		try {
			orcid.appendWork("0000-0001-9753-8285", "d0c29317-cb20-4a39-a475-2bcbf5cf650b", work);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}

		try {
			orcid.getRecord("0000-0001-9753-8285", "d0c29317-cb20-4a39-a475-2bcbf5cf650b");
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
	}

}
