/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import java.util.Map;
import java.util.HashMap;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;

import org.apache.log4j.Logger;
import org.dspace.JournalUtils;

import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.authority.Concept;
import org.dspace.core.Context;

/**
 * Cocoon Action to confirm that the requested journal landing page is for 
 * a journal that is under authority control in Dryad.
 * 
 * @author Nathan Day
 */
public class ValidateRequest extends AbstractAction {

    private static final Logger log = Logger.getLogger(ValidateRequest.class);

    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
                    String source, Parameters parameters) throws Exception
    {
        String journalName = parameters.getParameter(PARAM_JOURNAL_NAME);
        if (journalName == null || journalName.length() == 0) return null;

        // verify we have an accurate journal
        Context context = ContextUtil.obtainContext(objectModel);
        Concept journalConcept = JournalUtils.getJournalConceptByName(context,journalName);
        if (journalConcept == null) {
            return null;
        }
        String journalAbbr = JournalUtils.getJournalShortID(journalConcept);        
        if (journalAbbr != null && journalAbbr.length() != 0) {
            Map map = new HashMap();
            map.put(PARAM_JOURNAL_NAME, journalName);
            map.put(PARAM_JOURNAL_ABBR, journalAbbr);
            return map;
        }
        return null;
    }
}