/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.ExpiresValidity;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadJournalConcept;
import org.dspace.JournalUtils;
import org.dspace.core.ConfigurationManager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Cocoon Action to confirm that the requested journal landing page is for 
 * a journal that is under authority control in Dryad.
 * 
 * @author Nathan Day
 */
public class ValidateRequest extends AbstractAction implements CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(ValidateRequest.class);
    private static final Pattern issnPattern = Pattern.compile("\\d{4}-\\d{3}[\\dX]");

    protected static final long pageValidityMs = ConfigurationManager.getLongProperty("landing-page.cache.validity");
    protected  SourceValidity validity = new ExpiresValidity(System.currentTimeMillis() + pageValidityMs);

    private String journalISSN;

    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
                    String source, Parameters parameters) throws Exception
    {
        journalISSN = parameters.getParameter(Const.PARAM_JOURNAL_ISSN);
        if (journalISSN == null || journalISSN.length() == 0 || !issnPattern.matcher(journalISSN).matches()) {
            journalISSN = null;
            return null;
        }
        DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByISSN(journalISSN);
        if (journalConcept != null && journalConcept.getIntegrated()) {
            Map map = new HashMap();
            map.put(Const.PARAM_JOURNAL_NAME, journalConcept.getFullName());
            map.put(Const.PARAM_JOURNAL_ISSN, journalISSN);
            map.put(Const.PARAM_JOURNAL_ABBR, journalConcept.getJournalID());
            return map;
        }
        return null;
    }


    @Override
    public Serializable getKey()
    {
        return journalISSN;
    }

    @Override
    public SourceValidity getValidity()
    {
        return validity;
    }
}

