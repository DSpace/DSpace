/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.ProcessStatus;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;
import org.dspace.scripts.service.ProcessService;

public class ProcessBuilder extends AbstractBuilder<Process, ProcessService> {

    private Process process;

    protected ProcessBuilder(Context context) {
        super(context);
    }

    public static ProcessBuilder createProcess(Context context, EPerson ePerson, String scriptName,
                                               List<DSpaceCommandLineParameter> parameters)
        throws SQLException {
        ProcessBuilder processBuilder = new ProcessBuilder(context);
        return processBuilder.create(context, ePerson, scriptName, parameters);
    }

    private ProcessBuilder create(Context context, EPerson ePerson, String scriptName,
                                  List<DSpaceCommandLineParameter> parameters)
        throws SQLException {
        this.context = context;
        this.process = processService.create(context, ePerson, scriptName, parameters);
        this.process.setProcessStatus(ProcessStatus.SCHEDULED);
        return this;
    }

    public void cleanup() throws Exception {
        delete(process);
    }

    public Process build() {
        try {
            processService.update(context, process);
            context.dispatchEvents();
            indexingService.commit();
        } catch (Exception e) {
            return null;
        }
        return process;
    }

    public void delete(Process dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Process attachedDso = c.reloadEntity(dso);
            if (attachedDso != null) {
                getService().delete(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
    }


    protected ProcessService getService() {
        return processService;
    }
}
