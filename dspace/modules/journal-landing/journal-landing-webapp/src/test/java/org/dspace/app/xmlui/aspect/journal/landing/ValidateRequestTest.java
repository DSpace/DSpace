package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.datadryad.api.DryadJournalConcept;
import org.dspace.JournalUtils;
import org.dspace.content.authority.Concept;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.dspace.JournalUtils.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test ValicateRequest.act(). Note: Concept data provided by test db script.
 * @author Nathan Day
 */
public class ValidateRequestTest extends JournalLandingBaseTest
{

    ValidateRequest validateRequest;
    String journalIssn = "1111-1111";
    String journalName = "Evolution";
    String journalAbbr = "Evol";
    Concept c;
    DryadJournalConcept djc;
    Redirector redirector;
    SourceResolver sourceResolver;

    @Before
    public void setUp() {
        super.setUp();
        validateRequest = new ValidateRequest();
        try {
            c = Concept.findByConceptMetadata(context, journalIssn, "journal", "issn").get(0);
            djc = getJournalConceptByISSN(journalIssn);
            if (djc == null) {
                djc = new DryadJournalConcept(this.context, c);
                djc.setFullName(journalName);
                djc.setISSN(journalIssn);
                addDryadJournalConcept(context, djc);
            }
            parameters.setParameter(Const.PARAM_JOURNAL_ISSN, journalIssn);
            parameters.setParameter(Const.PARAM_JOURNAL_NAME, journalName);
            parameters.setParameter(Const.PARAM_JOURNAL_ABBR, journalAbbr);
            redirector = mock(Redirector.class);
            sourceResolver = mock(SourceResolver.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testActIntegrated() throws Exception {
        djc.setBooleanIntegrated(true);
        JournalUtils.updateDryadJournalConcept(djc);
        Map m = validateRequest.act(redirector, sourceResolver, objectModel, src, parameters);
        assertTrue(m != null);
    }

    @Test
    public void testActNonIntegrated() throws Exception {
        djc.setBooleanIntegrated(false);
        JournalUtils.updateDryadJournalConcept(djc);
        Map m = validateRequest.act(redirector, sourceResolver, objectModel, src, parameters);
        assertTrue(m == null);
    }

    @Test(expected=ParameterException.class)
    public void testActNoIssn() throws Exception {
        parameters.removeParameter(Const.PARAM_JOURNAL_ISSN);
        Map m = validateRequest.act(redirector, sourceResolver, objectModel, src, parameters);
        assertTrue(m == null);
    }

    @Test
    public void testActBadIssn() throws Exception {
        parameters.setParameter(Const.PARAM_JOURNAL_ISSN, "1111-11");
        Map m = validateRequest.act(redirector, sourceResolver, objectModel, src, parameters);
        assertTrue(m == null);
    }

    @Test
    public void testActNoConcept() throws Exception {
        parameters.setParameter(Const.PARAM_JOURNAL_ISSN, "2222-2222");
        Map m = validateRequest.act(redirector, sourceResolver, objectModel, src, parameters);
        assertTrue(m == null);
    }
}