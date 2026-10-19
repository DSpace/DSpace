/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.Options;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link Curation} script
 *
 * @author Maria Verdonck (Atmire) on 23/06/2020
 */
public class CurationScriptConfiguration<T extends Curation> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return this.dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    /**
     * Only repository admins or admins of the target object can run Curation script via the scripts
     * and processes endpoints.
     *
     * @param context The relevant DSpace context
     * @param commandLineParameters the parameters that will be used to start the process if known,
     *        <code>null</code> otherwise
     * @return true if the currentUser is allowed to run the script with the specified parameters or
     *         at least in some case if the parameters are not yet known
     */
    @Override
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {
        try {
            if (commandLineParameters == null) {
                return authorizeService.isAdmin(context) || authorizeService.isComColAdmin(context)
                        || authorizeService.isItemAdmin(context);
            } else if (commandLineParameters.stream()
                    .map(DSpaceCommandLineParameter::getName)
                    .noneMatch("-i"::equals)) {
                return authorizeService.isAdmin(context);
            } else {
                String dspaceObjectID = commandLineParameters.stream()
                                                             .filter(parameter -> "-i".equals(parameter.getName()))
                                                             .map(DSpaceCommandLineParameter::getValue)
                                                             .findFirst()
                                                             .get();
                HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
                DSpaceObject dso = handleService.resolveToObject(context, dspaceObjectID);
                return authorizeService.isAdmin(context, dso);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            super.options = CurationClientOptions.constructOptions();
        }
        return options;
    }
}
