/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.cli.Options;
import org.dspace.app.util.DSpaceObjectUtilsImpl;
import org.dspace.app.util.service.DSpaceObjectUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.utils.DSpace;

/**
 * Script configuration for {@link BulkAccessControl}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 * @param  <T> the {@link BulkAccessControl} type
 */
public class BulkAccessControlScriptConfiguration<T extends BulkAccessControl> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {

        try {
            if (Objects.isNull(commandLineParameters)) {
                return authorizeService.isAdmin(context) || authorizeService.isComColAdmin(context)
                    || authorizeService.isItemAdmin(context);
            } else {
                List<String> dspaceObjectIDs =
                    commandLineParameters.stream()
                                         .filter(parameter -> "-u".equals(parameter.getName()))
                                         .map(DSpaceCommandLineParameter::getValue)
                                         .collect(Collectors.toList());

                DSpaceObjectUtils dSpaceObjectUtils = new DSpace().getServiceManager().getServiceByName(
                    DSpaceObjectUtilsImpl.class.getName(), DSpaceObjectUtilsImpl.class);

                for (String dspaceObjectID : dspaceObjectIDs) {

                    DSpaceObject dso = dSpaceObjectUtils.findDSpaceObject(context, UUID.fromString(dspaceObjectID));

                    if (Objects.isNull(dso)) {
                        throw new IllegalArgumentException();
                    }

                    if (!authorizeService.isAdmin(context, dso)) {
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("u", "uuid", true, "target uuids of communities/collections/items");
            options.getOption("u").setType(String.class);
            options.getOption("u").setRequired(true);

            options.addOption("f", "file", true, "source json file");
            options.getOption("f").setType(InputStream.class);
            options.getOption("f").setRequired(true);

            options.addOption("h", "help", false, "help");

            super.options = options;
        }
        return options;
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    /**
     * Generic setter for the dspaceRunnableClass
     *
     * @param dspaceRunnableClass The dspaceRunnableClass to be set on this
     *                            BulkImportScriptConfiguration
     */
    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

}
