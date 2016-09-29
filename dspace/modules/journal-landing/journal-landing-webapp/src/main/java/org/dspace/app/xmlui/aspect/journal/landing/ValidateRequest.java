/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.avalon.framework.parameters.ParameterException;
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
public class ValidateRequest extends AbstractAction
{
    private static final Logger log = Logger.getLogger(ValidateRequest.class);

    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
                    String source, Parameters parameters) throws ParameterException
    {
        String journalISSN = parameters.getParameter(Const.PARAM_JOURNAL_ISSN);
        if (journalISSN == null || journalISSN.length() == 0 || !Const.issnPattern.matcher(journalISSN).matches()) {
            return null;
        }
        DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByISSN(journalISSN);
        if (journalConcept != null && (journalConcept.getIntegrated() || journalConcept.getHasJournalPage())) {
            Map map = new HashMap();
            map.put(Const.PARAM_JOURNAL_NAME, journalConcept.getFullName());
            map.put(Const.PARAM_JOURNAL_ISSN, journalISSN);
            map.put(Const.PARAM_JOURNAL_ABBR, journalConcept.getJournalID());
            try {
                map.put(Const.PARAM_DOWNLOAD_DURATION, parameters.getParameter(Const.PARAM_DOWNLOAD_DURATION));
            } catch (Exception e) {}
            return map;
        }
        return null;
    }
}

