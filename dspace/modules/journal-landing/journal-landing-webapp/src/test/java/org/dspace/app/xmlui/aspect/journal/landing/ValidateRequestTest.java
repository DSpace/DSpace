package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.content.authority.Concept;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test ValicateRequest.act(). Note: Concept data provided by test db script.
 * @author Nathan Day
 */
public class ValidateRequestTest extends JournalLandingBaseTest {

    ValidateRequest validateRequest;

    @Before
    public void setUp() {
        super.setUp();
        validateRequest = new ValidateRequest();
    }

    @Test
    public void testAct() throws Exception {
        String journalIssn = "1111-1111";
        Redirector redirector = mock(Redirector.class);
        SourceResolver sourceResolver = mock(SourceResolver.class);
        parameters.setParameter(Const.PARAM_JOURNAL_ISSN, journalIssn);
        Map m = validateRequest.act(redirector, sourceResolver, objectModel, src, parameters);
        Concept c = Concept.find(context, (Integer) m.get(Const.PARAM_CONCEPT_ID));
        assertTrue(c.getMetadata("journal", "issn", null, null)[0].value.equals(journalIssn));
    }

    @Test(expected=ParameterException.class)
    public void testActNoIssn() throws Exception {
        Redirector redirector = mock(Redirector.class);
        SourceResolver sourceResolver = mock(SourceResolver.class);
        Map m = validateRequest.act(redirector, sourceResolver, objectModel, src, parameters);
        assertTrue(m == null);
    }

    @Test
    public void testActBadIssn() throws Exception {
        Redirector redirector = mock(Redirector.class);
        SourceResolver sourceResolver = mock(SourceResolver.class);
        parameters.setParameter(Const.PARAM_JOURNAL_ISSN, "1111-11");
        Map m = validateRequest.act(redirector, sourceResolver, objectModel, src, parameters);
        assertTrue(m == null);
    }

    @Test
    public void testActNoConcept() throws Exception {
        Redirector redirector = mock(Redirector.class);
        SourceResolver sourceResolver = mock(SourceResolver.class);
        parameters.setParameter(Const.PARAM_JOURNAL_ISSN, "2222-2222");
        Map m = validateRequest.act(redirector, sourceResolver, objectModel, src, parameters);
        assertTrue(m == null);
    }
}