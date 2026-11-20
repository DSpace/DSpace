/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Script configuration for {@link BulkImport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 * @param  <T> the {@link BulkImport} type
 */
public class BulkImportScriptConfiguration<T extends BulkImport> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {
        try {
            // fall back to base tests if no other parameters passed
            if (Objects.isNull(commandLineParameters)) {
                return authorizeService.isCollectionAdmin(context) || authorizeService.isAdmin(context);
            }

            // admins can always execute
            if (authorizeService.isAdmin(context)) {
                return true;
            }

            // resolve target collection properly before checking isAdmin / can execute
            String collectionId = commandLineParameters.stream()
                .filter(parameter -> "-c".equals(parameter.getName()))
                .map(DSpaceCommandLineParameter::getValue)
                .findFirst()
                .orElse(null);

            if (StringUtils.isBlank(collectionId)) {
                return false;
            }

            UUID collectionUuid = UUID.fromString(collectionId);
            CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
            Collection collection = collectionService.find(context, collectionUuid);
            if (collection == null) {
                return false;
            }

            return authorizeService.isAdmin(context, collection);

        } catch (IllegalArgumentException e) {
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("SQLException occurred when checking if the current user is an admin", e);
        }
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("c", "collection", true,
                    "The UUID of the collection where the bulk import will be performed");
            options.getOption("c").setType(String.class);
            options.getOption("c").setRequired(true);

            options.addOption("f", "file", true, "source file");
            options.getOption("f").setType(InputStream.class);
            options.getOption("f").setRequired(true);

            options.addOption("er", "concludeOnError", false, "conclude the import at the first error");
            options.getOption("er").setType(boolean.class);
            options.getOption("er").setRequired(false);

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
