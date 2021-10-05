/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.dspace.content.ProcessStatus.COMPLETED;
import static org.dspace.content.ProcessStatus.FAILED;
import static org.dspace.content.ProcessStatus.RUNNING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.ProcessStatus;
import org.dspace.scripts.Process;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;

/**
 * Integration tests for {@link ProcessCleaner}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ProcessCleanerIT extends AbstractIntegrationTestWithDatabase {

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private ProcessService processService = ScriptServiceFactory.getInstance().getProcessService();

    @Test
    public void testWithoutProcessToDelete() throws Exception {

        Process process_1 = buildProcess(COMPLETED, addDays(new Date(), -2));
        Process process_2 = buildProcess(RUNNING, addDays(new Date(), -1));
        Process process_3 = buildProcess(FAILED, addDays(new Date(), -3));

        configurationService.setProperty("process-cleaner.days", 5);

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "process-cleaner" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        List<String> messages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(messages, hasSize(3));
        assertThat(messages, hasItem("Searching for processes with status: [COMPLETED]"));
        assertThat(messages, hasItem("Found 0 processes to be deleted"));
        assertThat(messages, hasItem("Process cleanup completed"));

        assertThat(processService.find(context, process_1.getID()), notNullValue());
        assertThat(processService.find(context, process_2.getID()), notNullValue());
        assertThat(processService.find(context, process_3.getID()), notNullValue());

    }

    @Test
    public void testWithoutSpecifiedStatus() throws Exception {

        Process process_1 = buildProcess(COMPLETED, addDays(new Date(), -2));
        Process process_2 = buildProcess(RUNNING, addDays(new Date(), -1));
        Process process_3 = buildProcess(FAILED, addDays(new Date(), -3));
        Process process_4 = buildProcess(COMPLETED, addDays(new Date(), -6));
        Process process_5 = buildProcess(COMPLETED, addDays(new Date(), -8));
        Process process_6 = buildProcess(RUNNING, addDays(new Date(), -7));
        Process process_7 = buildProcess(FAILED, addDays(new Date(), -8));

        configurationService.setProperty("process-cleaner.days", 5);

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "process-cleaner" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        List<String> messages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(messages, hasSize(3));
        assertThat(messages, hasItem("Searching for processes with status: [COMPLETED]"));
        assertThat(messages, hasItem("Found 2 processes to be deleted"));
        assertThat(messages, hasItem("Process cleanup completed"));

        assertThat(processService.find(context, process_1.getID()), notNullValue());
        assertThat(processService.find(context, process_2.getID()), notNullValue());
        assertThat(processService.find(context, process_3.getID()), notNullValue());
        assertThat(processService.find(context, process_4.getID()), nullValue());
        assertThat(processService.find(context, process_5.getID()), nullValue());
        assertThat(processService.find(context, process_6.getID()), notNullValue());
        assertThat(processService.find(context, process_7.getID()), notNullValue());

    }

    @Test
    public void testWithCompletedStatus() throws Exception {

        Process process_1 = buildProcess(COMPLETED, addDays(new Date(), -2));
        Process process_2 = buildProcess(RUNNING, addDays(new Date(), -1));
        Process process_3 = buildProcess(FAILED, addDays(new Date(), -3));
        Process process_4 = buildProcess(COMPLETED, addDays(new Date(), -6));
        Process process_5 = buildProcess(COMPLETED, addDays(new Date(), -8));
        Process process_6 = buildProcess(RUNNING, addDays(new Date(), -7));
        Process process_7 = buildProcess(FAILED, addDays(new Date(), -8));

        configurationService.setProperty("process-cleaner.days", 5);

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "process-cleaner", "-c" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        List<String> messages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(messages, hasSize(3));
        assertThat(messages, hasItem("Searching for processes with status: [COMPLETED]"));
        assertThat(messages, hasItem("Found 2 processes to be deleted"));
        assertThat(messages, hasItem("Process cleanup completed"));

        assertThat(processService.find(context, process_1.getID()), notNullValue());
        assertThat(processService.find(context, process_2.getID()), notNullValue());
        assertThat(processService.find(context, process_3.getID()), notNullValue());
        assertThat(processService.find(context, process_4.getID()), nullValue());
        assertThat(processService.find(context, process_5.getID()), nullValue());
        assertThat(processService.find(context, process_6.getID()), notNullValue());
        assertThat(processService.find(context, process_7.getID()), notNullValue());

    }

    @Test
    public void testWithRunningStatus() throws Exception {

        Process process_1 = buildProcess(COMPLETED, addDays(new Date(), -2));
        Process process_2 = buildProcess(RUNNING, addDays(new Date(), -1));
        Process process_3 = buildProcess(FAILED, addDays(new Date(), -3));
        Process process_4 = buildProcess(COMPLETED, addDays(new Date(), -6));
        Process process_5 = buildProcess(COMPLETED, addDays(new Date(), -8));
        Process process_6 = buildProcess(RUNNING, addDays(new Date(), -7));
        Process process_7 = buildProcess(FAILED, addDays(new Date(), -8));
        Process process_8 = buildProcess(RUNNING, addDays(new Date(), -9));

        configurationService.setProperty("process-cleaner.days", 5);

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "process-cleaner", "-r" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        List<String> messages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(messages, hasSize(3));
        assertThat(messages, hasItem("Searching for processes with status: [RUNNING]"));
        assertThat(messages, hasItem("Found 2 processes to be deleted"));
        assertThat(messages, hasItem("Process cleanup completed"));

        assertThat(processService.find(context, process_1.getID()), notNullValue());
        assertThat(processService.find(context, process_2.getID()), notNullValue());
        assertThat(processService.find(context, process_3.getID()), notNullValue());
        assertThat(processService.find(context, process_4.getID()), notNullValue());
        assertThat(processService.find(context, process_5.getID()), notNullValue());
        assertThat(processService.find(context, process_6.getID()), nullValue());
        assertThat(processService.find(context, process_7.getID()), notNullValue());
        assertThat(processService.find(context, process_8.getID()), nullValue());

    }

    @Test
    public void testWithFailedStatus() throws Exception {

        Process process_1 = buildProcess(COMPLETED, addDays(new Date(), -2));
        Process process_2 = buildProcess(RUNNING, addDays(new Date(), -1));
        Process process_3 = buildProcess(FAILED, addDays(new Date(), -3));
        Process process_4 = buildProcess(COMPLETED, addDays(new Date(), -6));
        Process process_5 = buildProcess(COMPLETED, addDays(new Date(), -8));
        Process process_6 = buildProcess(RUNNING, addDays(new Date(), -7));
        Process process_7 = buildProcess(FAILED, addDays(new Date(), -8));
        Process process_8 = buildProcess(FAILED, addDays(new Date(), -9));

        configurationService.setProperty("process-cleaner.days", 5);

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "process-cleaner", "-f" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        List<String> messages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(messages, hasSize(3));
        assertThat(messages, hasItem("Searching for processes with status: [FAILED]"));
        assertThat(messages, hasItem("Found 2 processes to be deleted"));
        assertThat(messages, hasItem("Process cleanup completed"));

        assertThat(processService.find(context, process_1.getID()), notNullValue());
        assertThat(processService.find(context, process_2.getID()), notNullValue());
        assertThat(processService.find(context, process_3.getID()), notNullValue());
        assertThat(processService.find(context, process_4.getID()), notNullValue());
        assertThat(processService.find(context, process_5.getID()), notNullValue());
        assertThat(processService.find(context, process_6.getID()), notNullValue());
        assertThat(processService.find(context, process_7.getID()), nullValue());
        assertThat(processService.find(context, process_8.getID()), nullValue());

    }

    @Test
    public void testWithCompletedAndFailedStatus() throws Exception {

        Process process_1 = buildProcess(COMPLETED, addDays(new Date(), -2));
        Process process_2 = buildProcess(RUNNING, addDays(new Date(), -1));
        Process process_3 = buildProcess(FAILED, addDays(new Date(), -3));
        Process process_4 = buildProcess(COMPLETED, addDays(new Date(), -6));
        Process process_5 = buildProcess(COMPLETED, addDays(new Date(), -8));
        Process process_6 = buildProcess(RUNNING, addDays(new Date(), -7));
        Process process_7 = buildProcess(FAILED, addDays(new Date(), -8));
        Process process_8 = buildProcess(FAILED, addDays(new Date(), -9));

        configurationService.setProperty("process-cleaner.days", 5);

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "process-cleaner", "-c", "-f" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        List<String> messages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(messages, hasSize(3));
        assertThat(messages, hasItem("Searching for processes with status: [COMPLETED, FAILED]"));
        assertThat(messages, hasItem("Found 4 processes to be deleted"));
        assertThat(messages, hasItem("Process cleanup completed"));

        assertThat(processService.find(context, process_1.getID()), notNullValue());
        assertThat(processService.find(context, process_2.getID()), notNullValue());
        assertThat(processService.find(context, process_3.getID()), notNullValue());
        assertThat(processService.find(context, process_4.getID()), nullValue());
        assertThat(processService.find(context, process_5.getID()), nullValue());
        assertThat(processService.find(context, process_6.getID()), notNullValue());
        assertThat(processService.find(context, process_7.getID()), nullValue());
        assertThat(processService.find(context, process_8.getID()), nullValue());

    }

    @Test
    public void testWithCompletedAndRunningStatus() throws Exception {

        Process process_1 = buildProcess(COMPLETED, addDays(new Date(), -2));
        Process process_2 = buildProcess(RUNNING, addDays(new Date(), -1));
        Process process_3 = buildProcess(FAILED, addDays(new Date(), -3));
        Process process_4 = buildProcess(COMPLETED, addDays(new Date(), -6));
        Process process_5 = buildProcess(COMPLETED, addDays(new Date(), -8));
        Process process_6 = buildProcess(RUNNING, addDays(new Date(), -7));
        Process process_7 = buildProcess(FAILED, addDays(new Date(), -8));
        Process process_8 = buildProcess(RUNNING, addDays(new Date(), -9));

        configurationService.setProperty("process-cleaner.days", 5);

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "process-cleaner", "-c", "-r" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        List<String> messages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(messages, hasSize(3));
        assertThat(messages, hasItem("Searching for processes with status: [COMPLETED, RUNNING]"));
        assertThat(messages, hasItem("Found 4 processes to be deleted"));
        assertThat(messages, hasItem("Process cleanup completed"));

        assertThat(processService.find(context, process_1.getID()), notNullValue());
        assertThat(processService.find(context, process_2.getID()), notNullValue());
        assertThat(processService.find(context, process_3.getID()), notNullValue());
        assertThat(processService.find(context, process_4.getID()), nullValue());
        assertThat(processService.find(context, process_5.getID()), nullValue());
        assertThat(processService.find(context, process_6.getID()), nullValue());
        assertThat(processService.find(context, process_7.getID()), notNullValue());
        assertThat(processService.find(context, process_8.getID()), nullValue());

    }

    @Test
    public void testWithFailedAndRunningStatus() throws Exception {

        Process process_1 = buildProcess(COMPLETED, addDays(new Date(), -2));
        Process process_2 = buildProcess(RUNNING, addDays(new Date(), -1));
        Process process_3 = buildProcess(FAILED, addDays(new Date(), -3));
        Process process_4 = buildProcess(COMPLETED, addDays(new Date(), -6));
        Process process_5 = buildProcess(COMPLETED, addDays(new Date(), -8));
        Process process_6 = buildProcess(RUNNING, addDays(new Date(), -7));
        Process process_7 = buildProcess(FAILED, addDays(new Date(), -8));
        Process process_8 = buildProcess(RUNNING, addDays(new Date(), -9));

        configurationService.setProperty("process-cleaner.days", 5);

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "process-cleaner", "-f", "-r" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        List<String> messages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(messages, hasSize(3));
        assertThat(messages, hasItem("Searching for processes with status: [FAILED, RUNNING]"));
        assertThat(messages, hasItem("Found 3 processes to be deleted"));
        assertThat(messages, hasItem("Process cleanup completed"));

        assertThat(processService.find(context, process_1.getID()), notNullValue());
        assertThat(processService.find(context, process_2.getID()), notNullValue());
        assertThat(processService.find(context, process_3.getID()), notNullValue());
        assertThat(processService.find(context, process_4.getID()), notNullValue());
        assertThat(processService.find(context, process_5.getID()), notNullValue());
        assertThat(processService.find(context, process_6.getID()), nullValue());
        assertThat(processService.find(context, process_7.getID()), nullValue());
        assertThat(processService.find(context, process_8.getID()), nullValue());

    }

    @Test
    public void testWithCompletedFailedAndRunningStatus() throws Exception {

        Process process_1 = buildProcess(COMPLETED, addDays(new Date(), -2));
        Process process_2 = buildProcess(RUNNING, addDays(new Date(), -1));
        Process process_3 = buildProcess(FAILED, addDays(new Date(), -3));
        Process process_4 = buildProcess(COMPLETED, addDays(new Date(), -6));
        Process process_5 = buildProcess(COMPLETED, addDays(new Date(), -8));
        Process process_6 = buildProcess(RUNNING, addDays(new Date(), -7));
        Process process_7 = buildProcess(FAILED, addDays(new Date(), -8));
        Process process_8 = buildProcess(RUNNING, addDays(new Date(), -9));

        configurationService.setProperty("process-cleaner.days", 5);

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "process-cleaner", "-f", "-r", "-c" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        List<String> messages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(messages, hasSize(3));
        assertThat(messages, hasItem("Searching for processes with status: [COMPLETED, FAILED, RUNNING]"));
        assertThat(messages, hasItem("Found 5 processes to be deleted"));
        assertThat(messages, hasItem("Process cleanup completed"));

        assertThat(processService.find(context, process_1.getID()), notNullValue());
        assertThat(processService.find(context, process_2.getID()), notNullValue());
        assertThat(processService.find(context, process_3.getID()), notNullValue());
        assertThat(processService.find(context, process_4.getID()), nullValue());
        assertThat(processService.find(context, process_5.getID()), nullValue());
        assertThat(processService.find(context, process_6.getID()), nullValue());
        assertThat(processService.find(context, process_7.getID()), nullValue());
        assertThat(processService.find(context, process_8.getID()), nullValue());

    }

    private Process buildProcess(ProcessStatus processStatus, Date creationTime) throws SQLException {
        return ProcessBuilder.createProcess(context, admin, "test", List.of())
            .withProcessStatus(processStatus)
            .withCreationTime(creationTime)
            .build();
    }
}
