/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import static java.util.Arrays.asList;
import static java.util.Calendar.DAY_OF_YEAR;
import static org.apache.commons.lang.time.DateFormatUtils.format;
import static org.dspace.core.Constants.BITSTREAM;
import static org.dspace.core.Constants.COLLECTION;
import static org.dspace.core.Constants.COMMUNITY;
import static org.dspace.core.Constants.ITEM;
import static org.dspace.statistics.SolrLoggerServiceImpl.DATE_FORMAT_8601;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.utils.DSpace;
import org.junit.Test;

/**
 * Integration test to test the {@link AnonymizeStatistics} script.
 */
public class AnonymizeStatisticsIT
        extends AbstractIntegrationTestWithDatabase {

    SolrStatisticsCore solrStatisticsCore = new DSpace().getSingletonService(SolrStatisticsCore.class);

    @Test
    public void testAnonymizeStatistics() throws Exception {

        // These IP addresses should not be anonymized in this test
        String ip1 = "75.133.248.54";
        String ip2 = "195.11.13.244";
        String ip3 = "16f4:0586:3148:3a8a:f307:e13e:2614:21a2";
        String ip4 = "5b02:f3ed:635f:98b1:d2c5:f292:90d9:3982";

        // IPv4 addresses are anonymized by default by masking the last part with '255'

        String ip5
                = "75.133.248.54";
        String ip5Anonymized
                = "75.133.248.255";

        String ip6
                = "195.11.13.244";
        String ip6Anonymized
                = "195.11.13.255";

        // IPv6 addresses are anonymized by default by masking the last two parts with 'FFFF'

        String ip7
                = "16f4:0586:3148:3a8a:f307:e13e:2614:21a2";
        String ip7Anonymized
                = "16f4:0586:3148:3a8a:f307:e13e:FFFF:FFFF";

        String ip8
                = "5b02:f3ed:635f:98b1:d2c5:f292:90d9:3982";
        String ip8Anonymized
                = "5b02:f3ed:635f:98b1:d2c5:f292:FFFF:FFFF";

        // bitstream document which should not be anonymized
        addSolrDocument(asList(
                Pair.of("id", "bitstream_view_recent"),
                Pair.of("type", BITSTREAM),
                Pair.of("ip", ip1),
                Pair.of("dns", "dns_1"),
                Pair.of("time", getTimeNDaysAgo(5))
        ));

        // item document which should not be anonymized
        addSolrDocument(asList(
                Pair.of("id", "item_view_recent"),
                Pair.of("type", ITEM),
                Pair.of("ip", ip2),
                Pair.of("dns", "dns_2"),
                Pair.of("time", getTimeNDaysAgo(20))
        ));

        // collection document which should not be anonymized
        addSolrDocument(asList(
                Pair.of("id", "collection_view_recent"),
                Pair.of("type", COLLECTION),
                Pair.of("ip", ip3),
                Pair.of("dns", "dns_3"),
                Pair.of("time", getTimeNDaysAgo(50))
        ));

        // community document which should not be anonymized
        addSolrDocument(asList(
                Pair.of("id", "community_view_recent"),
                Pair.of("type", COMMUNITY),
                Pair.of("ip", ip4),
                Pair.of("dns", "dns_4"),
                Pair.of("time", getTimeNDaysAgo(89))
        ));

        // bitstream document which should be anonymized
        addSolrDocument(asList(
                Pair.of("id", "bitstream_view_old"),
                Pair.of("type", BITSTREAM),
                Pair.of("ip", ip5),
                Pair.of("dns", "dns_1"),
                Pair.of("time", getTimeNDaysAgo(90))
        ));

        // item document which should be anonymized
        addSolrDocument(asList(
                Pair.of("id", "item_view_old"),
                Pair.of("type", ITEM),
                Pair.of("ip", ip6),
                Pair.of("dns", "dns_2"),
                Pair.of("time", getTimeNDaysAgo(130))
        ));

        // collection document which should be anonymized
        addSolrDocument(asList(
                Pair.of("id", "collection_view_old"),
                Pair.of("type", COLLECTION),
                Pair.of("ip", ip7),
                Pair.of("dns", "dns_3"),
                Pair.of("time", getTimeNDaysAgo(200))
        ));

        // community document which should be anonymized
        addSolrDocument(asList(
                Pair.of("id", "community_view_old"),
                Pair.of("type", COMMUNITY),
                Pair.of("ip", ip8),
                Pair.of("dns", "dns_4"),
                Pair.of("time", getTimeNDaysAgo(500))
        ));

        solrStatisticsCore.getSolr().commit();

        runDSpaceScript("anonymize-statistics");

        // The recent documents should not be anonymized

        assertEquals(
                ip1,
                getSolrDocumentById("bitstream_view_recent").getFieldValue("ip")
        );
        assertEquals(
                "dns_1",
                getSolrDocumentById("bitstream_view_recent").getFieldValue("dns")
        );

        assertEquals(
                ip2,
                getSolrDocumentById("item_view_recent").getFieldValue("ip")
        );
        assertEquals(
                "dns_2",
                getSolrDocumentById("item_view_recent").getFieldValue("dns")
        );

        assertEquals(
                ip3,
                getSolrDocumentById("collection_view_recent").getFieldValue("ip")
        );
        assertEquals(
                "dns_3",
                getSolrDocumentById("collection_view_recent").getFieldValue("dns")
        );

        assertEquals(
                ip4,
                getSolrDocumentById("community_view_recent").getFieldValue("ip")
        );
        assertEquals(
                "dns_4",
                getSolrDocumentById("community_view_recent").getFieldValue("dns")
        );

        // The older documents should be anonymized

        assertEquals(
                ip5Anonymized,
                getSolrDocumentById("bitstream_view_old").getFieldValue("ip")
        );
        assertEquals(
                "anonymized",
                getSolrDocumentById("bitstream_view_old").getFieldValue("dns")
        );

        assertEquals(
                ip6Anonymized,
                getSolrDocumentById("item_view_old").getFieldValue("ip")
        );
        assertEquals(
                "anonymized",
                getSolrDocumentById("item_view_old").getFieldValue("dns")
        );

        assertEquals(
                ip7Anonymized,
                getSolrDocumentById("collection_view_old").getFieldValue("ip")
        );
        assertEquals(
                "anonymized",
                getSolrDocumentById("collection_view_old").getFieldValue("dns")
        );

        assertEquals(
                ip8Anonymized,
                getSolrDocumentById("community_view_old").getFieldValue("ip")
        );
        assertEquals(
                "anonymized",
                getSolrDocumentById("community_view_old").getFieldValue("dns")
        );
    }

    private void addSolrDocument(List<Pair<String, Object>> fields) throws IOException, SolrServerException {

        SolrInputDocument document = new SolrInputDocument();
        for (Pair<String, Object> field : fields) {
            document.addField(field.getKey(), field.getValue());
        }
        solrStatisticsCore.getSolr().add(document);
    }

    private SolrDocument getSolrDocumentById(String id) throws IOException, SolrServerException {
        SolrDocumentList results = solrStatisticsCore.getSolr().query(new SolrQuery("id:" + id)).getResults();
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    private String getTimeNDaysAgo(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(DAY_OF_YEAR, -daysAgo);
        return format(calendar, DATE_FORMAT_8601);
    }
}
