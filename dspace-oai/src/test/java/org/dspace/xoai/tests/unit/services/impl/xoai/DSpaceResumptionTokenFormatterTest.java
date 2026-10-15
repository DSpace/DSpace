/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.unit.services.impl.xoai;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import com.lyncode.xoai.dataprovider.core.ResumptionToken;
import com.lyncode.xoai.dataprovider.exceptions.BadResumptionToken;
import org.dspace.xoai.data.ResumptionCursor;
import org.dspace.xoai.services.impl.xoai.DSpaceResumptionTokenFormatter;
import org.junit.Test;

public class DSpaceResumptionTokenFormatterTest {

    private static final String SET = "col_123456789_2";

    private final String itemId = UUID.randomUUID().toString();
    private final ResumptionCursor cursor = new ResumptionCursor();
    private final DSpaceResumptionTokenFormatter underTest = new DSpaceResumptionTokenFormatter(cursor);

    @Test
    public void theCursorReachedWhileServingAPageIsThePositionFieldOfTheToken() {
        cursor.moveTo(itemId);

        // The offset the xoai library counts up is not carried: the cursor alone names the position.
        assertThat(underTest.format(new ResumptionToken(200, "oai_dc", SET, null, null)),
                   is("oai_dc///" + SET + "/" + itemId));
    }

    @Test
    public void aVerbThatNeverReachedTheItemRepositoryPagesThroughItsOffset() {
        // ListSets paginates through its own repository, which leaves the cursor untouched: its tokens
        // carry the offset in the position field, because sets have no item.id to stand on.
        assertThat(underTest.format(new ResumptionToken(200, null, null, null, null)),
                   is("////200"));
    }

    @Test
    public void aTokenCarryingACursorIsParsedIntoThePositionItNames() throws Exception {
        ResumptionToken token = underTest.parse("oai_dc///" + SET + "/" + itemId);

        assertThat(token.getMetadataPrefix(), is("oai_dc"));
        assertThat(token.getSet(), is(SET));
        assertThat(cursor.valueOf(), is(itemId));
        assertThat("the cursor is the position, nothing is left to skip", token.getOffset(), is(0));
    }

    @Test
    public void aTokenCarryingAnOffsetIsParsedIntoIt() throws Exception {
        // The ListSets shape -- also what a token from before cursors existed looks like: the repository
        // then pages through the offset, slow on deep pages but correct.
        ResumptionToken token = underTest.parse("oai_dc///" + SET + "/200");

        assertThat(token.getOffset(), is(200));
        assertThat(token.getMetadataPrefix(), is("oai_dc"));
        assertThat(token.getSet(), is(SET));
        assertTrue(cursor.isEmpty());
    }

    @Test
    public void aTokenWithFewerFieldsThanExpectedIsRefused() {
        assertRefused("oai_dc///" + SET);
    }

    @Test
    public void theRetiredSixFieldShapeCarryingBothOffsetAndCursorIsRefused() {
        assertRefused("oai_dc///" + SET + "/200/" + itemId);
    }

    @Test
    public void aPositionThatIsNeitherAnItemIdNorAnOffsetIsRefused() {
        assertRefused("////");
    }

    @Test
    public void aCursorThatIsNotAnItemIdIsRejectedInsteadOfReachingSolr() {
        // The token is client supplied and the cursor is interpolated into a Solr query.
        assertRefused("oai_dc///" + SET + "/* TO *] OR item.public:false");
    }

    @Test
    public void whatIsFormattedCanBeParsedBack() throws Exception {
        cursor.moveTo(itemId);
        String formatted = underTest.format(new ResumptionToken(300, "mets", SET, null, null));

        ResumptionCursor parsedCursor = new ResumptionCursor();
        ResumptionToken parsed = new DSpaceResumptionTokenFormatter(parsedCursor).parse(formatted);

        assertThat(parsed.getMetadataPrefix(), is("mets"));
        assertThat(parsed.getSet(), is(SET));
        assertThat(parsedCursor.valueOf(), is(itemId));
    }

    private void assertRefused(String token) {
        try {
            underTest.parse(token);
            fail("expected a bad resumption token for " + token);
        } catch (BadResumptionToken expected) {
            assertTrue("nothing must be handed to the repository", cursor.isEmpty());
        }
    }
}
