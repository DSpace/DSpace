/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.ORCIDAuthority;
import org.dspace.app.cris.model.orcid.OrcidPreferencesUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

/**
 * @author l.pascarelli
 *
 */
public class CrisOrcidQueueConsumer implements Consumer {

	private static final Logger log = Logger.getLogger(CrisOrcidQueueConsumer.class);

	private OrcidPreferencesUtils orcidPreferencesUtils = new DSpace().getServiceManager().getServiceByName("orcidPreferencesUtils", OrcidPreferencesUtils.class);

	public void consume(Context ctx, Event event) throws Exception {
		DSpaceObject dso = event.getSubject(ctx);
		if (dso instanceof Item) {
			Item item = (Item) dso;
			if (item.isArchived()) {
				// 1)check the internal contributors
				Set<String> listAuthoritiesManager = ChoiceAuthorityManager.getManager().getAuthorities();
				for (String crisAuthority : listAuthoritiesManager) {
					List<String> listMetadata = ChoiceAuthorityManager.getManager()
							.getAuthorityMetadataForAuthority(crisAuthority);

					for (String metadata : listMetadata) {
						ChoiceAuthority choiceAuthority = ChoiceAuthorityManager.getManager()
								.getChoiceAuthority(metadata);
						if (ORCIDAuthority.class.isAssignableFrom(choiceAuthority.getClass())) {
							// 2)check for each internal contributors if has
							// authority
							Metadatum[] Metadatums = item.getMetadataByMetadataString(metadata);
							for (Metadatum dcval : Metadatums) {
								String authority = dcval.authority;
								if (StringUtils.isNotBlank(authority)) {
									// 3)check the orcid preferences
									boolean isAPreferiteWork = orcidPreferencesUtils
											.isAPreferiteToSendToOrcid(authority, dso, "orcid-publications-prefs");
									// 4)if the publications match the
									// preference add publication to queue
									if (isAPreferiteWork) {
										orcidPreferencesUtils.prepareOrcidQueue(authority, dso);
									}
								}
							}
						}
					}
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

	@Override
	public void initialize() throws Exception {
		// TODO Auto-generated method stub
		
	}
}