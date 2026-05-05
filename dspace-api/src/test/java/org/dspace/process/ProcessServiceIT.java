/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.process;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.ProcessStatus;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessServiceImpl;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ProcessService;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link ProcessServiceImpl}.
 */
public class ProcessServiceIT extends AbstractIntegrationTestWithDatabase {

    protected ProcessService processService = ScriptServiceFactory.getInstance().getProcessService();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void failProcessesOfInstanceTest() throws Exception {
        UUID oldInstance = UUID.fromString("04e7918f-ec0a-4d22-a765-0e28df041a7e");
        Process instanceOldProcess1 =
            ProcessBuilder.createProcess(context, admin, "mock-script", new LinkedList<>())
                .withProcessStatus(ProcessStatus.RUNNING)
                .withInstance(oldInstance)
                .build();
        Process instanceOldProcess2 =
            ProcessBuilder.createProcess(context, admin, "mock-script", new LinkedList<>())
                .withInstance(oldInstance)
                .withProcessStatus(ProcessStatus.RUNNING)
                .build();
        Process instanceNewProcess1 =
            ProcessBuilder.createProcess(context, admin, "mock-script", new LinkedList<>())
                .withProcessStatus(ProcessStatus.RUNNING)
                .build();
        Process instanceNewProcess2 =
            ProcessBuilder.createProcess(context, admin, "mock-script", new LinkedList<>())
                .withProcessStatus(ProcessStatus.RUNNING)
                .build();
        processService.failProcessesOfInstance(context, oldInstance);
        assertEquals(ProcessStatus.FAILED, instanceOldProcess1.getProcessStatus());
        assertEquals(ProcessStatus.FAILED, instanceOldProcess2.getProcessStatus());
        assertEquals(ProcessStatus.RUNNING, instanceNewProcess1.getProcessStatus());
        assertEquals(ProcessStatus.RUNNING, instanceNewProcess2.getProcessStatus());
    }
}
