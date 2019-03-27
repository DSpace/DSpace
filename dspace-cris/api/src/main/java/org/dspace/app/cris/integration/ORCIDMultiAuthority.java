/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.orcid.OrcidAuthorityValue;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.Choices;
import org.dspace.utils.DSpace;

/**
 * 
 * Authority to aggregate "extra" value to single choice 
 * 
 * @author Pascarelli Luigi Andrea
 *
 */
public class ORCIDMultiAuthority extends RPMultiAuthority {

	private static final int DEFAULT_MAX_ROWS = 10;

	private static Logger log = Logger.getLogger(ORCIDMultiAuthority.class);

	private OrcidService source = new DSpace().getServiceManager().getServiceByName("OrcidSource", OrcidService.class);

	public List<OrcidAuthorityExtraMetadataGenerator> generators = new DSpace().getServiceManager().getServicesByType(OrcidAuthorityExtraMetadataGenerator.class);
	
	@Override
	public Choices getMatches(String field, String query, int collection, int start, int limit, String locale) {
		Choices choices = super.getMatches(field, query, collection, start, limit, locale);		
		return new Choices(addExternalResults(field, query, choices, start, limit==0?DEFAULT_MAX_ROWS:limit), choices.start, choices.total, choices.confidence, choices.more);
	}

	protected Choice[] addExternalResults(String field, String text, Choices choices, int start, int max) {
		if (source != null) {
			try {
				List<Choice> results = new ArrayList<Choice>();
				List<AuthorityValue> values = source.queryOrcidBioByFamilyNameAndGivenName(text, start, max);
				// adding choices loop
				int added = 0;
				for (AuthorityValue val : values) {
					if (added < max) {						
						Map<String, String> extras = ((OrcidAuthorityValue)val).choiceSelectMap();
						extras.put("insolr", "false");
						extras.put("link", getLink((OrcidAuthorityValue)val));
						results.addAll(buildAggregateByExtra(val));
						added++;
					}
				}
				return (Choice[])ArrayUtils.addAll(choices.values, results.toArray(new Choice[results.size()]));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else {
			log.warn("external source for authority not configured");
		}	
		return choices.values;
	}

    public List<Choice> buildAggregateByExtra(AuthorityValue value)
    {
        List<Choice> choiceList = new LinkedList<Choice>();
        if(generators!=null) {
            for(OrcidAuthorityExtraMetadataGenerator gg : generators) {
                choiceList.addAll(gg.buildAggregate(source, value));
            }
        }
        return choiceList;
    }
	
    private String getLink(OrcidAuthorityValue val) {
		return source.getBaseURL() + val.getOrcid_id();
	}

}
