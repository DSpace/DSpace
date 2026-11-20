/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.xoai;

import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

import com.lyncode.xoai.dataprovider.core.ResumptionToken;
import com.lyncode.xoai.dataprovider.exceptions.BadResumptionToken;
import com.lyncode.xoai.dataprovider.services.api.ResumptionTokenFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.xoai.data.ResumptionCursor;
import org.dspace.xoai.util.DateUtils;


/**
 * Decode and encode the resumptionToken values DSpace hands out.
 *
 * <p>One shape is accepted, {@code metadataPrefix/from/until/set/cursor}: the four fields the xoai library
 * drives, plus the position the harvest resumes from. What that position is depends on the verb:</p>
 * <ul>
 *   <li>the {@code item.id} of the last record served, for the verbs that page through the item repository
 *       (ListRecords, ListIdentifiers). It lets the next page be reached with a Solr range query instead of
 *       a deep offset skip; see {@link ResumptionCursor}.</li>
 *   <li>a plain record offset, for ListSets: sets have no item.id and the xoai library pages them by
 *       counting records up. An item-verb token carrying an offset instead of an item.id -- the historical
 *       token shape -- is served the same way: slow on deep pages, but correct.</li>
 * </ul>
 *
 * <p>Any other shape is refused as {@link BadResumptionToken}.</p>
 */
public class DSpaceResumptionTokenFormatter implements ResumptionTokenFormatter {
    private final static Logger log = LogManager.getLogger(DSpaceResumptionTokenFormatter.class);
    private final static String TOKEN_SEPARATOR = "/";

    private final ResumptionCursor cursor;

    public DSpaceResumptionTokenFormatter(ResumptionCursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public ResumptionToken parse(String resumptionToken) throws BadResumptionToken {
        if (resumptionToken == null) {
            return new ResumptionToken();
        }
        String[] res = resumptionToken.split(TOKEN_SEPARATOR, -1);
        if (res.length != 5) {
            throw new BadResumptionToken();
        }
        try {
            String prefix = res[0].isEmpty() ? null : res[0];
            Date from = res[1].isEmpty() ? null : Date.from(DateUtils.parse(res[1]));
            Date until = res[2].isEmpty() ? null : Date.from(DateUtils.parse(res[2]));
            String set = res[3].isEmpty() ? null : res[3];
            // The last field is the position the harvest resumes from:
            //   * the item.id of the last record served,
            //   * a plain offset for the verbs that have none (ListSets).
            // Forcing the value through UUID.fromString is also what keeps a client supplied string out of the Solr
            // query the cursor is interpolated into.
            int offset = 0;
            try {
                cursor.moveTo(UUID.fromString(res[4]).toString());
            } catch (IllegalArgumentException notAnItemId) {
                offset = Integer.parseInt(res[4]);
            }
            return new ResumptionToken(offset, prefix, set, from, until);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BadResumptionToken();
        }
    }


    @Override
    public String format(ResumptionToken resumptionToken) {
        // The repository moves the cursor to the end of the page it just served, which is exactly where this
        // token resumes from. It stays empty for the verbs that never reach the item repository -- ListSets --
        // whose pages are reached through the offset the xoai library counts up.
        return new StringJoiner(TOKEN_SEPARATOR)
            .add(resumptionToken.hasMetadataPrefix() ? resumptionToken.getMetadataPrefix() : "")
            .add(resumptionToken.hasFrom() ? DateUtils.format(resumptionToken.getFrom().toInstant()) : "")
            .add(resumptionToken.hasUntil() ? DateUtils.format(resumptionToken.getUntil().toInstant()) : "")
            .add(resumptionToken.hasSet() ? resumptionToken.getSet() : "")
            .add(cursor.isEmpty() ? String.valueOf(resumptionToken.getOffset()) : cursor.valueOf())
            .toString();
    }

}
