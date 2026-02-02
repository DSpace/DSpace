/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import org.apache.commons.cli.ParseException;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ScriptService;
import org.junit.Test;

public class CurationIT extends AbstractIntegrationTestWithDatabase {

    @Test(expected = ParseException.class)
    public void curationWithoutEPersonParameterTest() throws Exception {

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                                              .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .build();
        context.restoreAuthSystemState();
        String[] args = new String[] {"curate", "-t", CurationClientOptions.getTaskOptions().get(0),
            "-i", collection.getHandle()};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

        DSpaceRunnable script = null;
        if (scriptConfiguration != null) {
            script = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
        }
        if (script != null) {
            if (DSpaceRunnable.StepResult.Continue.equals(script.initialize(args, testDSpaceRunnableHandler, null))) {
                script.run();
            }
        }
    }

    @Test
    public void curationWithEPersonParameterTest() throws Exception {

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                                              .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .build();
        context.restoreAuthSystemState();
        String[] args = new String[] {"curate", "-e", "admin@email.com", "-t",
            CurationClientOptions.getTaskOptions().get(0), "-i", collection.getHandle()};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

        DSpaceRunnable script = null;
        if (scriptConfiguration != null) {
            script = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
        }
        if (script != null) {
            if (DSpaceRunnable.StepResult.Continue.equals(script.initialize(args, testDSpaceRunnableHandler, null))) {
                script.run();
            }
        }
    }
}
