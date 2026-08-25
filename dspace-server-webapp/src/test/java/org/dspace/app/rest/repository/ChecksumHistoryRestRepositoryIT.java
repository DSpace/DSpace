/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.checker.BitstreamDispatcher;
import org.dspace.checker.CheckerCommand;
import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.factory.CheckerServiceFactory;
import org.dspace.checker.service.ChecksumHistoryService;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.CoreHelpers;
import org.dspace.core.HibernateDBConnection;
import org.hibernate.query.MutationQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChecksumHistoryRestRepositoryIT extends AbstractControllerIntegrationTest {

    private Bitstream bitstream;
    private Long historyId;
    private String expectedResultCode;

    @Before
    public void initChecksumTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder
                .createCommunity(context)
                .withName("Test Community")
                .build();
        Collection collection = CollectionBuilder
                .createCollection(context, community)
                .withName("Test Collection")
                .build();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Test Item")
                .build();

        String bitstreamContent = "EXAMPLE CONTENT.";
        bitstream = BitstreamBuilder
                .createBitstream(context, item, new ByteArrayInputStream(bitstreamContent.getBytes()))
                .withName("test-file.txt")
                .build();

        CheckerCommand checker = new CheckerCommand(context);
        checker.setProcessStartDate(Instant.now());

        checker.setDispatcher(new BitstreamDispatcher() {
            private boolean delivered = false;

            @Override
            public Bitstream next() {
                if (!delivered) {
                    delivered = true;
                    return bitstream;
                }
                return null;
            }
        });

        checker.process();

        context.commit();

        ChecksumHistoryService historyService = CheckerServiceFactory.getInstance().getChecksumHistoryService();
        List<ChecksumHistory> histories = historyService.findByBitstream(context, bitstream, 1, 0);

        ChecksumHistory generatedHistory = histories.getFirst();
        historyId = generatedHistory.getID();
        expectedResultCode = generatedHistory.getResult().getResultCode().name();

        context.restoreAuthSystemState();
    }

    @After
    public void cleanupChecksums() throws Exception {
        if (bitstream != null) {
            context.turnOffAuthorisationSystem();
            HibernateDBConnection dbc = (HibernateDBConnection) CoreHelpers.getDBConnection(context);

            MutationQuery qryHistory = dbc.getSession().createNativeMutationQuery(
                    "DELETE FROM checksum_history WHERE bitstream_id = :id");
            qryHistory.setParameter("id", bitstream.getID());
            qryHistory.executeUpdate();

            MutationQuery qryRecent = dbc.getSession().createNativeMutationQuery(
                    "DELETE FROM most_recent_checksum WHERE bitstream_id = :id");
            qryRecent.setParameter("id", bitstream.getID());
            qryRecent.executeUpdate();

            context.commit();
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void findOneTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/checksumhistories/" + historyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(historyId.intValue())))
                .andExpect(jsonPath("$.resultCodeValue", is(expectedResultCode)))
                .andExpect(jsonPath("$.type", is("checksumhistory")));
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/core/checksumhistories/" + historyId))
                .andExpect(status().isForbidden());

        getClient().perform(get("/api/core/checksumhistories/" + historyId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/checksumhistories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements")
                        .value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$._embedded.checksumhistories").exists());
    }

    @Test
    public void findByBitstreamTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/checksumhistories/search/byBitstream")
                        .param("bitstream", bitstream.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andExpect(jsonPath("$._embedded.checksumhistories[0].id", is(historyId.intValue())));
    }

    @Test
    public void findByResultCodeTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/checksumhistories/search/byResultCode")
                        .param("resultCode", expectedResultCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.checksumhistories[?(@.id == " + historyId + ")]").exists());
    }

    @Test
    public void findByResultCodeBadRequestTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/checksumhistories/search/byResultCode")
                        .param("resultCode", "NOT_EXISTING"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void findBitstreamLinkTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/checksumhistories/" + historyId + "/bitstream"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bitstream.getID().toString())))
                .andExpect(jsonPath("$.name", is("test-file.txt")))
                .andExpect(jsonPath("$.type", is("bitstream")));
    }

    @Test
    public void findBitstreamLinkForbiddenTest() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/core/checksumhistories/" + historyId + "/bitstream"))
                .andExpect(status().isForbidden());
        getClient().perform(get("/api/core/checksumhistories/" + historyId + "/bitstream"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findBitstreamLinkNotFoundTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/core/checksumhistories/999999999/bitstream"))
                .andExpect(status().isNotFound());
    }
}
