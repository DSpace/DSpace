/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.data;

/**
 * Ordered key of the last record served by an OAI-PMH page, carried across a single request.
 *
 * <p>The xoai library models a resumptionToken as a plain record offset
 * ({@link com.lyncode.xoai.dataprovider.core.ResumptionToken#getOffset()}), which forces Solr to skip that
 * many documents before it can serve a page: the deeper a harvest goes, the slower the response gets.
 * The {@code item.id} of the last record already served lets Solr seek straight to the next page instead, at a
 * cost that no longer grows with the offset -- and whether more pages remain is read off the bounded query's
 * numFound, so the key is the only position the token has to carry. It's possible because the Solr query is
 * already sorted on {@code item.id}.</p>
 *
 * <p>It has nowhere to live in the xoai library's token, so it travels through this object between the two places
 * DSpace owns:
 *   * {@code DSpaceResumptionTokenFormatter}, which reads it from and writes it back to the token string
 *   * {@code DSpaceItemSolrRepository}, which turns it into a Solr range query.
 * </p>
 *
 * <p>One instance is created per request by {@code DSpaceOAIDataProvider} and handed to both collaborators,
 * so concurrent harvests never share a position. A request serves one page, which is why a single key is
 * enough: the value read from the incoming token is replaced by the one the served page ends on.</p>
 */
public class ResumptionCursor {

    private String itemUUID;

    public String valueOf() {
        return itemUUID;
    }

    public boolean isEmpty() {
        return itemUUID == null;
    }

    public void moveTo(String itemUUID) {
        this.itemUUID = itemUUID;
    }
}
