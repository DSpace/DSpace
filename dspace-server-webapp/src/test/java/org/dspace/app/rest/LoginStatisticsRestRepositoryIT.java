/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.dspace.app.rest.matcher.LoginStatisticsMatcher.match;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import org.dspace.app.rest.repository.LoginStatisticsRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.EPersonBuilder;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LoginStatisticsRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Before
    public void setup() {
        configurationService.setProperty("solr-statistics.autoCommit", false);
    }

    @Test
    public void testSearchByDateRangeWithoutParameters() throws Exception {

        context.turnOffAuthorisationSystem();
        EPerson firstUser = createEPerson("user1@email.com", "First", "User");
        EPerson secondUser = createEPerson("user2@email.com", "Second", "User");
        EPerson thirdUser = createEPerson("user3@email.com", "Third", "User");
        EPerson fourthUser = createEPerson("user4@email.com", "Fourth", "User");
        context.restoreAuthSystemState();

        loginManyTimes(firstUser, 3);
        loginManyTimes(secondUser, 5);
        loginManyTimes(thirdUser, 2);
        loginManyTimes(fourthUser, 0);

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/logins/search/byDateRange"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.logins", hasSize(4)))
            .andExpect(jsonPath("$._embedded.logins", contains(
                match("Second User", "user2@email.com", 5),
                match("First User", "user1@email.com", 3),
                match("Third User", "user3@email.com", 2),
                match("first (admin) last (admin)", "admin@email.com", 1))));

    }

    @Test
    public void testSearchByDateRangeWithLimit() throws Exception {

        context.turnOffAuthorisationSystem();
        EPerson firstUser = createEPerson("user1@email.com", "First", "User");
        EPerson secondUser = createEPerson("user2@email.com", "Second", "User");
        EPerson thirdUser = createEPerson("user3@email.com", "Third", "User");
        EPerson fourthUser = createEPerson("user4@email.com", "Fourth", "User");
        context.restoreAuthSystemState();

        loginManyTimes(firstUser, 3);
        loginManyTimes(secondUser, 5);
        loginManyTimes(thirdUser, 2);
        loginManyTimes(fourthUser, 0);

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/logins/search/byDateRange")
            .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.logins", hasSize(2)))
            .andExpect(jsonPath("$._embedded.logins", contains(
                match("Second User", "user2@email.com", 5),
                match("First User", "user1@email.com", 3))));

    }

    @Test
    public void testSearchByDateRange() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/logins/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), -1)))
            .param("endDate", formatDate(addDays(new Date(), 1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.logins", contains(
                match("first (admin) last (admin)", "admin@email.com", 1))));

        getClient(adminToken).perform(get("/api/statistics/logins/search/byDateRange")
            .param("endDate", formatDate(addDays(new Date(), 1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.logins", contains(
                match("first (admin) last (admin)", "admin@email.com", 1))));

        getClient(adminToken).perform(get("/api/statistics/logins/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), -1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.logins", contains(
                match("first (admin) last (admin)", "admin@email.com", 1))));

        getClient(adminToken).perform(get("/api/statistics/logins/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), 1)))
            .param("endDate", formatDate(addDays(new Date(), 2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.logins").doesNotExist());

        getClient(adminToken).perform(get("/api/statistics/logins/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), -2)))
            .param("endDate", formatDate(addDays(new Date(), -1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.logins").doesNotExist());

        getClient(adminToken).perform(get("/api/statistics/logins/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), 1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.logins").doesNotExist());

        getClient(adminToken).perform(get("/api/statistics/logins/search/byDateRange")
            .param("endDate", formatDate(addDays(new Date(), -1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.logins").doesNotExist());

    }

    @Test
    public void testSearchByDateRangeWithInvalidDates() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/logins/search/byDateRange")
            .param("startDate", "invalid date")
            .param("endDate", formatDate(addDays(new Date(), 1))))
            .andExpect(status().isUnprocessableEntity());

        getClient(adminToken).perform(get("/api/statistics/logins/search/byDateRange")
            .param("startDate", formatDate(addDays(new Date(), -1)))
            .param("endDate", "18/12/2021"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testSearchByDateRangeWithNotAdminUser() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/statistics/logins/search/byDateRange"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testFindOne() throws Exception {

        context.turnOffAuthorisationSystem();
        EPerson user = createEPerson("user1@email.com", "First", "User");
        context.restoreAuthSystemState();

        loginManyTimes(user, 3);

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/logins/" + user.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", match("First User", "user1@email.com", 3)));
    }

    @Test
    public void testFindOneWithoutLogin() throws Exception {

        context.turnOffAuthorisationSystem();
        EPerson user = createEPerson("user1@email.com", "First", "User");
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/logins/" + user.getID().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testFindOneWithUnknownUser() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/statistics/logins/" + UUID.randomUUID().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testFindOneWithNotAdminUser() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/statistics/logins/" + UUID.randomUUID().toString()))
            .andExpect(status().isForbidden());
    }

    private String formatDate(Date date) {
        return LoginStatisticsRestRepository.DATE_FORMATTER.format(date);
    }

    private void loginManyTimes(EPerson user, int times) throws Exception {
        for (int i = 0; i < times; i++) {
            getAuthToken(user.getEmail(), password);
        }
    }

    private EPerson createEPerson(String email, String firstName, String lastName) throws SQLException {
        return EPersonBuilder.createEPerson(context)
            .withEmail(email)
            .withNameInMetadata(firstName, lastName)
            .withPassword(password)
            .build();
    }
}
