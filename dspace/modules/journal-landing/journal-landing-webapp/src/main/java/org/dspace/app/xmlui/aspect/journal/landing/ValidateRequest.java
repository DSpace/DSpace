/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import java.util.Map;
import java.util.HashMap;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;

import org.apache.log4j.Logger;

import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;
import org.dspace.submit.utils.DryadJournalSubmissionUtils;

/**
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
        String journalAbbr = DryadJournalSubmissionUtils.findKeyByFullname(journalName);
        if (journalAbbr != null && journalAbbr.length() != 0) {
            Map map = new HashMap();
            map.put(PARAM_JOURNAL_NAME, journalName);
            map.put(PARAM_JOURNAL_ABBR, journalAbbr);
            return map;
        }
        return null;
    }
}