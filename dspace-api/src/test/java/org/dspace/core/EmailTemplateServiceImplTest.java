/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.dspace.AbstractDSpaceTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.service.EmailTemplateService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link EmailTemplateServiceImpl}.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public class EmailTemplateServiceImplTest extends AbstractDSpaceTest {

    private EmailTemplateService emailTemplateService;
    private File dspaceDir;
    private File emailsDir;
    private ConfigurationService configurationService;
    private AuthorizeService authorizeService;
    private Context context;

    @Before
    public void setUp() {

        try {
            dspaceDir = Files.createTempDirectory("dspace-email-test-").toFile();
            emailsDir = new File(dspaceDir, "config" + File.separator + "emails");
            emailsDir.mkdirs();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary test directory", e);
        }

        configurationService = mock(ConfigurationService.class);
        authorizeService = mock(AuthorizeService.class);

        // Configure mock configuration service
        when(configurationService.getProperty("dspace.dir")).thenReturn(dspaceDir.getAbsolutePath());

        emailTemplateService = new EmailTemplateServiceImpl();
        ReflectionTestUtils.setField(emailTemplateService, "configurationService", configurationService);
        ReflectionTestUtils.setField(emailTemplateService, "authorizeService", authorizeService);

        context = mock(Context.class);
        try {
            when(authorizeService.isAdmin(context)).thenReturn(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void cleanup() {
        if (dspaceDir != null && dspaceDir.exists()) {
            deleteRecursive(dspaceDir);
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    private void createTemplateFile(String name, String content) throws IOException {
        File file = new File(emailsDir, name);
        Files.writeString(file.toPath(), content);
    }

    @Test
    public void testFindAllTemplatesSuccess() throws Exception {
        createTemplateFile("register",
            "#set($subject = \"Registration\")\n"
            + "## {0} user name\n"
            + "Hello ${params[0]}, welcome to ${config.get('dspace.name')}.\n");
        createTemplateFile("feedback",
            "#set($subject = \"Feedback\")\n"
            + "## {0} sender\n"
            + "Feedback from ${params[0]}.\n");

        List<EmailTemplate> templates = emailTemplateService.findAll(context);
        assertNotNull(templates);
        assertEquals(2, templates.size());
        assertEquals("feedback", templates.get(0).getName());
        assertEquals("register", templates.get(1).getName());
    }

    @Test
    public void testFindByNameSuccess() throws Exception {
        String content = "#set($subject = \"Welcome to DSpace\")\n"
            + "#set($mimetype = \"text/plain\")\n"
            + "## {0} recipient name\n"
            + "## {1} verification url\n"
            + "Dear ${params[0]},\n"
            + "Click here: ${params[1]}\n"
            + "-- ${config.get('dspace.name')}\n";
        createTemplateFile("register", content);

        EmailTemplate template = emailTemplateService.findByName(context, "register");
        assertNotNull(template);
        assertEquals("register", template.getName());
        assertEquals("Welcome to DSpace", template.getSubject());
        assertEquals(EmailTemplateMimeType.TEXT_PLAIN, template.getMimetype());
        assertNotNull(template.getEtag());
        assertFalse(template.getEtag().isEmpty());

        // Verify parsed positional variables
        List<EmailTemplateVariable> variables = template.getVariables();
        assertNotNull(variables);
        assertEquals(2, variables.size());
        assertEquals(0, variables.get(0).getIndex());
        assertEquals("recipient name", variables.get(0).getDescription());
        assertEquals("${params[0]}", variables.get(0).getPlaceholder());
        assertEquals(1, variables.get(1).getIndex());
        assertEquals("verification url", variables.get(1).getDescription());
        assertEquals("${params[1]}", variables.get(1).getPlaceholder());
    }

    @Test
    public void testFindByNameHtmlMimeType() throws Exception {
        String content = "#set($subject = \"HTML Notification\")\n"
            + "#set($mimetype = \"text/html\")\n"
            + "<p>Hello ${params[0]}, welcome to <b>DSpace</b>!</p>\n";
        createTemplateFile("test_html", content);

        EmailTemplate template = emailTemplateService.findByName(context, "test_html");
        assertNotNull(template);
        assertEquals(EmailTemplateMimeType.TEXT_HTML, template.getMimetype());
    }

    @Test
    public void testFindByNameNotFound() throws Exception {
        EmailTemplate template = emailTemplateService.findByName(context, "non_existent");
        assertNull(template);
    }

    @Test
    public void testUpdateTemplateSuccess() throws Exception {
        createTemplateFile("register", "#set($subject = \"Old\")\nOld content ${params[0]}");

        String updatedContent = "#set($subject = \"Updated Subject\")\n"
            + "## {0} recipient name\n"
            + "Updated body for ${params[0]}.\n";

        EmailTemplate result = emailTemplateService.update(context, "register", updatedContent);
        assertNotNull(result);
        assertEquals("Updated Subject", result.getSubject());
        assertTrue(result.getContent().contains("Updated body for ${params[0]}"));

        // Verify persisted content on disk
        File file = new File(emailsDir, "register");
        String diskContent = Files.readString(file.toPath());
        assertTrue(diskContent.contains("Updated body for ${params[0]}"));
    }

    @Test
    public void testUpdatePreservesLeadingWhitespace() throws Exception {
        createTemplateFile("register", "#set($subject = \"Old\")\nOld content");

        String contentWithLeadingWhitespace = "\n  #set($subject = \"Indented\")\n  Indented body text here.\n";
        EmailTemplate result = emailTemplateService.update(context, "register", contentWithLeadingWhitespace);
        assertNotNull(result);

        File file = new File(emailsDir, "register");
        String diskContent = Files.readString(file.toPath());
        assertTrue(diskContent.startsWith("\n  #set($subject = \"Indented\")"));
    }

    @Test
    public void testUpdateTemplateNotFound() {
        assertThrows(IllegalArgumentException.class, () ->
            emailTemplateService.update(context, "non_existent", "Some valid content for template.")
        );
    }

    @Test
    public void testUpdateTemplateRejectsShortContent() throws Exception {
        createTemplateFile("register", "#set($subject = \"Valid\")\nValid content.");

        assertThrows(IllegalArgumentException.class, () ->
            emailTemplateService.update(context, "register", "Short")
        );
    }

    @Test
    public void testUpdateTemplateRejectsInvalidVelocitySyntax() throws Exception {
        createTemplateFile("register", "#set($subject = \"Valid\")\nValid content.");

        // Unclosed VTL directive
        String badContent = "#set($subject = \"Invalid\")\n#if($foo) missing endif";

        assertThrows(IllegalArgumentException.class, () ->
            emailTemplateService.update(context, "register", badContent)
        );
    }

    @Test
    public void testUpdateTemplateRejectsScriptTag() throws Exception {
        createTemplateFile("test_xss", "#set($subject = \"Valid\")\nValid content.");

        String maliciousContent = "#set($subject = \"Malicious\")\n"
            + "<p>Hello</p><script>alert('XSS')</script>";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            emailTemplateService.update(context, "test_xss", maliciousContent)
        );
        assertTrue(ex.getMessage().contains("prohibited script or unsafe HTML elements"));
    }

    @Test
    public void testUpdateTemplateRejectsIframeTag() throws Exception {
        createTemplateFile("test_iframe", "#set($subject = \"Valid\")\nValid content.");

        String maliciousContent = "#set($subject = \"Malicious\")\n"
            + "<iframe src=\"https://evil.com\"></iframe>";

        assertThrows(IllegalArgumentException.class, () ->
            emailTemplateService.update(context, "test_iframe", maliciousContent)
        );
    }

    @Test
    public void testUpdateTemplateRejectsInlineJavascript() throws Exception {
        createTemplateFile("test_inline_js", "#set($subject = \"Valid\")\nValid content.");

        String maliciousContent = "#set($subject = \"Malicious\")\n"
            + "<a href=\"javascript:evil()\">Click</a>";

        assertThrows(IllegalArgumentException.class, () ->
            emailTemplateService.update(context, "test_inline_js", maliciousContent)
        );
    }

    @Test
    public void testUpdateTemplateRejectsOnerrorEventHandler() throws Exception {
        createTemplateFile("test_onerror", "#set($subject = \"Valid\")\nValid content.");

        String maliciousContent = "#set($subject = \"Malicious\")\n"
            + "<img src=\"invalid.jpg\" onerror=\"alert(1)\"/>";

        assertThrows(IllegalArgumentException.class, () ->
            emailTemplateService.update(context, "test_onerror", maliciousContent)
        );
    }

    @Test
    public void testUpdateTemplateAllowsSafeHtmlTags() throws Exception {
        createTemplateFile("safe_html", "#set($subject = \"Valid\")\nValid initial content.");

        String safeContent = "#set($subject = \"Formatted Email\")\n"
            + "#set($mimetype = \"text/html\")\n"
            + "<p>Hello <b>${params[0]}</b>, please visit <a href=\"https://dspace.org\">DSpace</a>.</p>\n"
            + "<ul><li>Item 1</li><li>Item 2</li></ul>\n"
            + "<table><tr><td>Cell 1</td></tr></table>\n";

        EmailTemplate result = emailTemplateService.update(context, "safe_html", safeContent);
        assertNotNull(result);
        assertEquals("Formatted Email", result.getSubject());
    }

    @Test
    public void testPathTraversalRejectionInFindByName() {
        assertThrows(IllegalArgumentException.class, () ->
            emailTemplateService.findByName(context, "../../../etc/passwd")
        );
    }

    @Test
    public void testPathTraversalRejectionInUpdate() {
        assertThrows(IllegalArgumentException.class, () ->
            emailTemplateService.update(context, "sub/../../secret", "Valid content for test template.")
        );
    }

    @Test
    public void testSymlinkEscapeRejection() throws Exception {
        // Create an outside file
        File outsideDir = Files.createTempDirectory("outside-dir-").toFile();
        File outsideFile = new File(outsideDir, "outside_template");
        Files.writeString(outsideFile.toPath(), "Outside content that shouldn't be accessible.");

        // Create a symlink inside emails directory pointing outside
        Path symlink = emailsDir.toPath().resolve("symlinked_file");
        try {
            Files.createSymbolicLink(symlink, outsideFile.toPath());
        } catch (UnsupportedOperationException | IOException e) {
            // If filesystem doesn't support symlinks in this environment, skip
            return;
        }

        assertThrows(IllegalArgumentException.class, () ->
            emailTemplateService.findByName(context, "symlinked_file")
        );

        deleteRecursive(outsideDir);
    }

    @Test
    public void testETagDeterminism() {
        String content1 = "#set($subject = \"Test\")\nBody content 1.";
        String content2 = "#set($subject = \"Test\")\nBody content 2.";

        String etag1 = emailTemplateService.computeETag(content1);
        String etag1Repeat = emailTemplateService.computeETag(content1);
        String etag2 = emailTemplateService.computeETag(content2);

        assertNotNull(etag1);
        assertEquals(DigestUtils.sha256Hex(content1), etag1);
        assertEquals(etag1, etag1Repeat);
        assertNotEquals(etag1, etag2);
    }

    @Test
    public void testNonAdminAccessDeniedOnFindAll() throws Exception {
        when(authorizeService.isAdmin(context)).thenReturn(false);

        EPerson user = mock(EPerson.class);
        when(user.getName()).thenReturn("regularuser");
        when(user.getID()).thenReturn(UUID.randomUUID());
        when(user.getEmail()).thenReturn("user@dspace.test");
        when(context.getCurrentUser()).thenReturn(user);

        assertThrows(AuthorizeException.class, () -> emailTemplateService.findAll(context));
    }

    @Test
    public void testNonAdminAccessDeniedOnFindByName() throws Exception {
        when(authorizeService.isAdmin(context)).thenReturn(false);
        assertThrows(AuthorizeException.class, () -> emailTemplateService.findByName(context, "register"));
    }

    @Test
    public void testNonAdminAccessDeniedOnUpdate() throws Exception {
        when(authorizeService.isAdmin(context)).thenReturn(false);
        assertThrows(AuthorizeException.class, () ->
            emailTemplateService.update(context, "register", "New content.")
        );
    }

    @Test
    public void testNullContextAccessDenied() {
        assertThrows(AuthorizeException.class, () -> emailTemplateService.findAll(null));
        assertThrows(AuthorizeException.class, () -> emailTemplateService.findByName(null, "register"));
        assertThrows(AuthorizeException.class, () ->
            emailTemplateService.update(null, "register", "New content with sufficient length.")
        );
    }
}
