/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.model.EmailTemplateRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Integration tests for {@link org.dspace.app.rest.repository.EmailTemplateRestRepository}.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public class EmailTemplateRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ObjectMapper mapper;

    private File emailsDir;
    private String originalRegisterContent;

    @Before
    public void setupTestEmails() throws Exception {
        String dspaceDir = configurationService.getProperty("dspace.dir");
        emailsDir = new File(dspaceDir, "config" + File.separator + "emails");
        if (!emailsDir.exists()) {
            emailsDir.mkdirs();
        }

        File registerFile = new File(emailsDir, "register");
        if (registerFile.exists()) {
            originalRegisterContent = Files.readString(registerFile.toPath(), StandardCharsets.UTF_8);
        } else {
            originalRegisterContent = "## Parameters: {0} registration URL\n"
                + "#set($subject = 'Account Registration')\n"
                + "Please click the link below to complete registration:\n"
                + "${params[0]}\n";
            Files.writeString(registerFile.toPath(), originalRegisterContent, StandardCharsets.UTF_8);
        }

        File testFile = new File(emailsDir, "it_test_template");
        String testContent = "## Parameters: {0} username\n"
            + "#set($subject = 'Test Notification')\n"
            + "Hello ${params[0]},\n"
            + "This is an integration test email template body.\n";
        Files.writeString(testFile.toPath(), testContent, StandardCharsets.UTF_8);
    }

    @After
    public void cleanupTestEmails() throws Exception {
        if (originalRegisterContent != null && emailsDir != null) {
            File registerFile = new File(emailsDir, "register");
            Files.writeString(registerFile.toPath(), originalRegisterContent, StandardCharsets.UTF_8);
        }
        if (emailsDir != null) {
            File testFile = new File(emailsDir, "it_test_template");
            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }

    @Test
    public void findAllAsAdmin() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/emailtemplates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.emailtemplates.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(2)));
    }

    @Test
    public void findAllAsAnonymous() throws Exception {
        getClient().perform(get("/api/system/emailtemplates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllAsNonAdmin() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/emailtemplates"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findOneAsAdmin() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/emailtemplates/it_test_template"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("it_test_template")))
                .andExpect(jsonPath("$.subject", is("Test Notification")))
                .andExpect(jsonPath("$.mimetype", is("text/plain")))
                .andExpect(jsonPath("$.etag", not(emptyOrNullString())))
                .andExpect(jsonPath("$.variables[0].index", is(0)))
                .andExpect(jsonPath("$.variables[0].description", is("username")))
                .andExpect(jsonPath("$.variables[0].placeholder", is("${params[0]}")))
                .andExpect(jsonPath("$.configVariables").doesNotExist())
                .andExpect(jsonPath("$.allowedConfigs").doesNotExist())
                .andExpect(header().exists(HttpHeaders.ETAG));
    }

    @Test
    public void findOneHtmlTemplate() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        File htmlFile = new File(emailsDir, "it_html_template");
        String htmlContent = "## Parameters: {0} name\n"
            + "#set($mimetype = 'text/html')\n"
            + "#set($subject = 'HTML Notification')\n"
            + "<h1>Hello ${params[0]}</h1>\n";
        Files.writeString(htmlFile.toPath(), htmlContent, StandardCharsets.UTF_8);

        try {
            getClient(token).perform(get("/api/system/emailtemplates/it_html_template"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("it_html_template")))
                    .andExpect(jsonPath("$.subject", is("HTML Notification")))
                    .andExpect(jsonPath("$.mimetype", is("text/html")));
        } finally {
            if (htmlFile.exists()) {
                htmlFile.delete();
            }
        }
    }

    @Test
    public void findOneNotFound() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/emailtemplates/non_existent_template_xyz"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void findOneAsAnonymous() throws Exception {
        getClient().perform(get("/api/system/emailtemplates/it_test_template"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneAsNonAdmin() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/emailtemplates/it_test_template"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getReturnsETagAndSupportsIfNoneMatch() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        AtomicReference<String> etagRef = new AtomicReference<>();
        getClient(token).perform(get("/api/system/emailtemplates/it_test_template"))
                .andExpect(status().isOk())
                .andDo(result -> etagRef.set(result.getResponse().getHeader(HttpHeaders.ETAG)));

        assertNotNull(etagRef.get());

        // Conditional GET with matching If-None-Match should return 304 Not Modified
        getClient(token).perform(get("/api/system/emailtemplates/it_test_template")
                .header(HttpHeaders.IF_NONE_MATCH, etagRef.get()))
                .andExpect(status().isNotModified());
    }

    @Test
    public void putAsAdminSuccess() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        AtomicReference<String> etagRef = new AtomicReference<>();
        getClient(token).perform(get("/api/system/emailtemplates/it_test_template"))
                .andExpect(status().isOk())
                .andDo(result -> etagRef.set(result.getResponse().getHeader(HttpHeaders.ETAG)));

        EmailTemplateRest rest = new EmailTemplateRest();
        String updatedBody = "## Parameters: {0} username\n"
            + "#set($subject = 'Updated Subject')\n"
            + "Updated body text for ${params[0]}.\n";
        rest.setContent(updatedBody);

        getClient(token).perform(put("/api/system/emailtemplates/it_test_template")
                .header(HttpHeaders.IF_MATCH, etagRef.get())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(rest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject", is("Updated Subject")))
                .andExpect(header().exists(HttpHeaders.ETAG));
    }

    @Test
    public void putWithoutIfMatchHeaderReturns428() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        EmailTemplateRest rest = new EmailTemplateRest();
        rest.setContent("## Parameters: {0} user\n#set($subject = 'Test')\nValid body text here.");

        getClient(token).perform(put("/api/system/emailtemplates/it_test_template")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(rest)))
                .andExpect(status().is(428));
    }

    @Test
    public void putWithStaleETagReturns412() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        EmailTemplateRest rest = new EmailTemplateRest();
        rest.setContent("## Parameters: {0} user\n#set($subject = 'Test')\nValid body text here.");

        getClient(token).perform(put("/api/system/emailtemplates/it_test_template")
                .header(HttpHeaders.IF_MATCH, "\"wrong-or-stale-etag\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(rest)))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    public void putWithWildcardIfMatchSuccess() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        EmailTemplateRest rest = new EmailTemplateRest();
        rest.setContent("## Parameters: {0} user\n#set($subject = 'Wildcard Test')\nValid body text here.");

        getClient(token).perform(put("/api/system/emailtemplates/it_test_template")
                .header(HttpHeaders.IF_MATCH, "*")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(rest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject", is("Wildcard Test")));
    }

    @Test
    public void concurrentEditSimulation() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        // Step 1: Admin A and Admin B both load the template and receive the initial ETag
        AtomicReference<String> initialEtag = new AtomicReference<>();
        getClient(token).perform(get("/api/system/emailtemplates/it_test_template"))
                .andExpect(status().isOk())
                .andDo(result -> initialEtag.set(result.getResponse().getHeader(HttpHeaders.ETAG)));

        // Step 2: Admin B saves first with the initial ETag -> succeeds
        EmailTemplateRest adminBEdit = new EmailTemplateRest();
        adminBEdit.setContent("## Parameters: {0} user\n#set($subject = 'Admin B Edit')\nAdmin B was faster.");

        getClient(token).perform(put("/api/system/emailtemplates/it_test_template")
                .header(HttpHeaders.IF_MATCH, initialEtag.get())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(adminBEdit)))
                .andExpect(status().isOk());

        // Step 3: Admin A attempts to save using the now-stale initial ETag -> rejected with 412 Precondition Failed
        EmailTemplateRest adminAEdit = new EmailTemplateRest();
        adminAEdit.setContent("## Parameters: {0} user\n#set($subject = 'Admin A Edit')\nAdmin A was slower.");

        getClient(token).perform(put("/api/system/emailtemplates/it_test_template")
                .header(HttpHeaders.IF_MATCH, initialEtag.get())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(adminAEdit)))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    public void putContentTooShortReturns400() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        EmailTemplateRest rest = new EmailTemplateRest();
        rest.setContent("short");

        getClient(token).perform(put("/api/system/emailtemplates/it_test_template")
                .header(HttpHeaders.IF_MATCH, "*")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(rest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void putContentOnlyWhitespaceReturns400() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        EmailTemplateRest rest = new EmailTemplateRest();
        rest.setContent("   \n\n\t   ");

        getClient(token).perform(put("/api/system/emailtemplates/it_test_template")
                .header(HttpHeaders.IF_MATCH, "*")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(rest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void putInvalidVtlSyntaxReturns400() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        EmailTemplateRest rest = new EmailTemplateRest();
        rest.setContent("#set($subject = 'Broken')\n#if($broken)\nMissing end directive");

        getClient(token).perform(put("/api/system/emailtemplates/it_test_template")
                .header(HttpHeaders.IF_MATCH, "*")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(rest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void putXssContentReturns400() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        EmailTemplateRest rest = new EmailTemplateRest();
        rest.setContent("#set($subject = 'XSS')\n<script>alert('xss')</script>\nBody content text.");

        getClient(token).perform(put("/api/system/emailtemplates/it_test_template")
                .header(HttpHeaders.IF_MATCH, "*")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(rest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void putNonExistentTemplateReturns404() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        EmailTemplateRest rest = new EmailTemplateRest();
        rest.setContent("## Parameters: {0} user\n#set($subject = 'Test')\nValid body text here.");

        getClient(token).perform(put("/api/system/emailtemplates/does_not_exist_template")
                .header(HttpHeaders.IF_MATCH, "*")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(rest)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void putAsAnonymousReturns401() throws Exception {
        EmailTemplateRest rest = new EmailTemplateRest();
        rest.setContent("## Parameters: {0} user\n#set($subject = 'Test')\nValid body text here.");

        getClient().perform(put("/api/system/emailtemplates/it_test_template")
                .header(HttpHeaders.IF_MATCH, "*")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(rest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void putAsNonAdminReturns403() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        EmailTemplateRest rest = new EmailTemplateRest();
        rest.setContent("## Parameters: {0} user\n#set($subject = 'Test')\nValid body text here.");

        getClient(token).perform(put("/api/system/emailtemplates/it_test_template")
                .header(HttpHeaders.IF_MATCH, "*")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(rest)))
                .andExpect(status().isForbidden());
    }
}
