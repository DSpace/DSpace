/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.content.authority.zdb.ZDBAuthorityValue;
import org.dspace.content.authority.zdb.ZDBService;
import org.dspace.utils.DSpace;
/**
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class ZDBAuthority extends ItemAuthority {

    private static final int DEFAULT_MAX_ROWS = 10;

    private static Logger log = Logger.getLogger(ZDBAuthority.class);

    private ZDBService source = new DSpace().getServiceManager().getServiceByName("ZDBSource", ZDBService.class);

    private static DSpace dspace = new DSpace();

    @Override
    public Choices getMatches(String query, int start, int limit, String locale) {
        Choices choices = super.getMatches(query, start, limit, locale);
        return new Choices(addExternalResults(query, choices, start, limit <= 0 ? DEFAULT_MAX_ROWS : limit),
                choices.start, choices.total, choices.confidence, choices.more);
    }

    protected Choice[] addExternalResults(String text, Choices choices, int start, int max) {
        if (source != null) {
            try {
                List<Choice> results = new ArrayList<Choice>();
                List<ZDBAuthorityValue> values = source.list(text, start, max);
                // adding choices loop
                int added = 0;
                for (AuthorityValue val : values) {
                    if (added < max) {
                        Map<String, String> extras = getZDBExtra(val);
                        results.add(new Choice(val.generateString(), val.getValue(), val.getValue(), extras));
                        added++;
                    }
                }
                return (Choice[]) ArrayUtils.addAll(choices.values, results.toArray(new Choice[results.size()]));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.warn("external source for authority not configured");
        }
        return choices.values;
    }

    private Map<String, String> getZDBExtra(AuthorityValue val) {
        Map<String, String> extras = new HashMap<String, String>();
        List<ZDBExtraMetadataGenerator> generators = dspace.getServiceManager()
                .getServicesByType(ZDBExtraMetadataGenerator.class);
        if (generators != null) {
            for (ZDBExtraMetadataGenerator gg : generators) {
                Map<String, String> extrasTmp = gg.build(val);
                extras.putAll(extrasTmp);
            }
        }
        return extras;
    }
}