/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ScriptService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MetadataEditControlledVocabularyIT extends AbstractIntegrationTestWithDatabase {
    ConfigurationService configurationService = new DSpace().getConfigurationService();
    PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
    MetadataAuthorityService mas = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
    ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private Collection collection;

    private TestDSpaceRunnableHandler handler;

    private File file;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        configurationService.setProperty("vocabulary.plugin.srsc.hierarchy.store", true);
        configurationService.setProperty("vocabulary.plugin.srsc.hierarchy.suggest", true);
        configurationService.setProperty("vocabulary.plugin.srsc.delimiter", "::");
        configurationService.setProperty("authority.controlled.dc.subject", "true");
        configurationService.setProperty("choices.plugin.dc.subject", "DSpaceControlledVocabulary");
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.SolrAuthority = SolrAuthorAuthority",
                                             "org.dspace.content.authority.SolrAuthority = SolrEditorAuthority",
                                             "org.dspace.content.authority.SolrAuthority = SolrSubjectAuthority",
                                             "org.dspace.content.authority.DSpaceControlledVocabulary = DCV"
                                         });

        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        mas.clearCache();

        handler = new TestDSpaceRunnableHandler();
        file = File.createTempFile("edit", ".csv");

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).build();
        collection = CollectionBuilder.createCollection(context, community).build();

        context.restoreAuthSystemState();
    }

    @After
    @Override
    public void destroy() throws Exception {
        super.destroy();

        configurationService.reloadConfig();

        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        mas.clearCache();
    }

    @Test
    public void testEditVocabularySubject() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder
            .createItem(context, collection)
            .withTitle("Test Export")
            .withSubject("Church studies", "VR110103", 600)
            .withSubject("History of religion", "VR110102", 600)
            .build();
        context.restoreAuthSystemState();

        runExport(item);
        assertNull(
            "Export should succeed",
            handler.getException()
        );

        String exportedContent = readFile();
        assertThat(exportedContent, containsString("Church studies::VR110103::600"));
        assertThat(exportedContent, containsString("History of religion::VR110102::600"));

        writeFile(exportedContent.replace(
            "History of religion::VR110102::600",
            "Missionary studies::VR110104::600"
        ));

        runImport();
        assertNull(
            "Re-import should succeed",
            handler.getException()
        );

        // Note: we need to re-retrieve the Item to see the newly updated metadata
        item = itemService.find(context, item.getID());

        List<MetadataValue> subjects = itemService.getMetadata(item, "dc", "subject", null, Item.ANY);
        assertEquals(2, subjects.size());

        List<String> values = subjects.stream().map(MetadataValue::getValue).collect(Collectors.toList());
        List<String> authorities = subjects.stream().map(MetadataValue::getAuthority).collect(Collectors.toList());

        assertThat(values, containsInAnyOrder("Church studies", "Missionary studies"));
        assertThat(authorities, containsInAnyOrder("VR110103", "VR110104"));
    }

    @Test
    public void testEditVocabularySubjectHierarchy() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder
            .createItem(context, collection)
            .withTitle("Test Export")
            .withSubject("HUMANITIES and RELIGION::Religion/Theology::Psychology of religion", "VR110109", 600)
            .withSubject("HUMANITIES and RELIGION::Religion/Theology::Philosophy of religion", "VR110110", 600)
            .build();
        context.restoreAuthSystemState();

        runExport(item);
        assertNull(
            "Export should succeed",
            handler.getException()
        );

        String exportedContent = readFile();
        assertThat(exportedContent, containsString(
            "HUMANITIES and RELIGION::Religion/Theology::Psychology of religion::VR110109::600"));
        assertThat(exportedContent, containsString(
            "HUMANITIES and RELIGION::Religion/Theology::Philosophy of religion::VR110110::600"));

        writeFile(exportedContent.replace(
            "HUMANITIES and RELIGION::Religion/Theology::Psychology of religion::VR110109::600",
            "HUMANITIES and RELIGION::Religion/Theology::New Testament exegesis::VR110111::600"
        ));

        runImport();
        assertNull(
            "Re-import should succeed",
            handler.getException()
        );

        // Note: we need to re-retrieve the Item to see the newly updated metadata
        item = itemService.find(context, item.getID());

        List<MetadataValue> subjects = itemService.getMetadata(item, "dc", "subject", null, Item.ANY);
        assertEquals(2, subjects.size());

        List<String> values = subjects.stream().map(MetadataValue::getValue).collect(Collectors.toList());
        List<String> authorities = subjects.stream().map(MetadataValue::getAuthority).collect(Collectors.toList());

        assertThat(values, containsInAnyOrder(
            "HUMANITIES and RELIGION::Religion/Theology::Philosophy of religion",
            "HUMANITIES and RELIGION::Religion/Theology::New Testament exegesis"
        ));
        assertThat(authorities, containsInAnyOrder("VR110110", "VR110111"));
    }

    private void runExport(Item item) throws Exception {
        runScript(new String[] {
            "metadata-export",
            "-i", String.valueOf(item.getHandle()),
            "-f", file.getAbsolutePath()
        });
    }

    private void runImport() throws Exception {
        runScript(new String[] {
            "metadata-import", "-s",
            "-e", eperson.getEmail(),
            "-f", file.getAbsolutePath(),
        });
    }

    private void runScript(String[] args) throws Exception {
        ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

        DSpaceRunnable script = null;
        if (scriptConfiguration != null) {
            script = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
        }
        if (script != null) {
            script.initialize(args, handler, null);
            script.run();
        }
    }

    private String readFile() throws Exception {
        return IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
    }

    private void writeFile(String content) throws Exception {
        try (
            BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8
                )
            )
        ) {
            out.write(content);
            out.flush();
        }
    }
}
