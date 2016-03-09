package org.dspace.app.xmlui.aspect.journal.landing;

import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.content.authority.Concept;
import org.jdom2.Document;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Nathan Day
 */
public class JournalStatsTest extends JournalLandingBaseTest {

    private JournalStats journalStats;
    String journalIssn = "1111-1111";

    @Before
    public void setUp() {
        super.setUp();
        journalStats = new JournalStats();
        try {
            Concept c = Concept.findByConceptMetadata(context, journalIssn, "journal", "issn").get(0);
            parameters.setParameter(Const.PARAM_CONCEPT_ID, String.valueOf(c.getID()));
            parameters.setParameter(Const.PARAM_JOURNAL_ISSN, journalIssn);
            journalStats.setup(resolver, objectModel, src, parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSetup() throws Exception {
        // journalStats.setup() tested in @Before setUp() above
    }

    private static String dataPackageCountXpath = "count(//dri:div[@n='journal-landing-stats-deps']//dri:reference) = 2";
    @Test
    public void testAddBody() throws Exception {
        String path = this.getClass().getResource("/solr.1.xml").getFile();
        setQueryResponse(path, "utf-8");
        Body body = doc.setBody();
        journalStats.addBody(body);
        DRIContentHandler dch = new DRIContentHandler(body);
        Document jdomDoc = dch.getDocument();
        XPathExpression<Boolean> pkgExp =
            XPathFactory.instance().compile(dataPackageCountXpath, Filters.fboolean(), vars, driNs);
        assertTrue(pkgExp.evaluateFirst(jdomDoc));
    }
}