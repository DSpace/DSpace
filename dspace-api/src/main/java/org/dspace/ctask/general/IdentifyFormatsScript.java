/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import org.apache.commons.cli.ParseException;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.dspace.core.Context;
import org.dspace.curate.Curator;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Process/script wrapper around the {@link IdentifyFormats} curation task. It re-runs
 * bitstream format identification and corrects bitstreams stored as "Unknown". Unlike
 * running the generic {@code curate} script, this script defaults to the whole repository
 * when no target is given, so an administrator can fix mislabelled bitstreams across the
 * site from the Admin UI (Administrative &gt; Processes) or the CLI in one step.
 *
 * <p>The correction logic is not duplicated here: this script simply invokes the
 * {@code identifyformats} curation task through a {@link Curator}.
 *
 * @author DSpace
 */
public class IdentifyFormatsScript extends DSpaceRunnable<IdentifyFormatsScriptConfiguration<IdentifyFormatsScript>> {

    /** Registered id of the curation task this script delegates to (see curate.cfg). */
    protected static final String TASK_NAME = "identifyformats";

    protected SiteService siteService;
    protected ConfigurationService configurationService;

    protected String identifier;
    protected boolean processAll;
    protected boolean help;

    @Override
    public IdentifyFormatsScriptConfiguration<IdentifyFormatsScript> getScriptConfiguration() {
        return new DSpace().getServiceManager()
            .getServiceByName("identify-formats", IdentifyFormatsScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        this.siteService = ContentServiceFactory.getInstance().getSiteService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        this.help = commandLine.hasOption('h');
        this.identifier = commandLine.getOptionValue('i');
        this.processAll = commandLine.hasOption('a');
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            return;
        }

        Context context = new Context();
        context.turnOffAuthorisationSystem();

        // Optionally widen the task to all bitstreams (not just Unknown) for the duration
        // of this run, restoring the previous configuration afterwards.
        String previousProcessAll = configurationService.getProperty(IdentifyFormats.CFG_PROCESS_ALL);
        if (processAll) {
            configurationService.setProperty(IdentifyFormats.CFG_PROCESS_ALL, "true");
        }

        try {
            Curator curator = new Curator(handler);
            // A reporter must be set: without one, Curator.report() (called by the task)
            // routes through the handler with an unparseable format string. The task's full
            // result is surfaced to the process log via getResult() below, so the reporter
            // content itself is not needed here.
            curator.setReporter(new StringBuilder());
            curator.addTask(context, TASK_NAME);

            if (identifier != null) {
                handler.logInfo("Identifying bitstream formats under: " + identifier);
                curator.curate(context, identifier);
            } else {
                handler.logInfo("Identifying bitstream formats across the whole repository");
                Site site = siteService.findSite(context);
                curator.curate(context, site);
            }

            handler.logInfo(curator.getResult(TASK_NAME));
            context.restoreAuthSystemState();
            context.complete();
        } catch (Exception e) {
            context.abort();
            throw e;
        } finally {
            configurationService.setProperty(IdentifyFormats.CFG_PROCESS_ALL, previousProcessAll);
        }
    }
}
