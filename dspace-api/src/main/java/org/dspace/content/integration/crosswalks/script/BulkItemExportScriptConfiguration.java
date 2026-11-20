/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.script;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.cli.Options;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 * {@link ScriptConfiguration} for the {@link BulkItemExport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 * @param <T> the BulkItemExport type
 */
public class BulkItemExportScriptConfiguration<T extends BulkItemExport> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {
        StringBuilder property = new StringBuilder("bulk-export.limit.");
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        ConfigurationService configurationService = new DSpace().getConfigurationService();
        try {
            if (
                    authorizeService.isAdmin(context) ||
                    authorizeService.isComColAdmin(context) ||
                    authorizeService.isItemAdmin(context)
            ) {
                property.append("admin");
            } else {
                property.append(Optional.ofNullable(context.getCurrentUser()).map(ignored -> "loggedIn")
                                        .orElse("notLoggedIn"));
            }
        } catch (SQLException e) {
            return false;
        }
        int maxByUserCategory = configurationService.getIntProperty(property.toString(), -1);
        return maxByUserCategory != 0;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("q", "query", true, "the query to perform to find the items to be exported");
            options.getOption("q").setType(String.class);
            options.getOption("q").setRequired(false);

            options.addOption("s", "scope", true, "the items to search scope");
            options.getOption("s").setType(String.class);
            options.getOption("s").setRequired(false);

            options.addOption("c", "configuration", true, "the discovery configuration");
            options.getOption("c").setType(String.class);
            options.getOption("c").setRequired(false);

            options.addOption("sf", "filters", true, "the search filters");
            options.getOption("sf").setType(String.class);
            options.getOption("sf").setRequired(false);

            options.addOption("t", "type", true, "the entity type");
            options.getOption("t").setType(String.class);
            options.getOption("t").setRequired(false);

            options.addOption("so", "sort", true, "the sort field and order");
            options.getOption("so").setType(String.class);
            options.getOption("so").setRequired(false);

            options.addOption("f", "format", true, "the format in which the itemd must be exported");
            options.getOption("f").setType(String.class);
            options.getOption("f").setRequired(true);

            options.addOption("si", "selected-items", true, "the list of selected items to export");
            options.getOption("si").setType(String.class);
            options.getOption("si").setRequired(false);

            options.addOption("o", "offset", true, "the offset for export start");
            options.getOption("o").setType(String.class);
            options.getOption("o").setRequired(false);

            options.addOption("l", "limit", true, "limit of items to export");
            options.getOption("l").setType(String.class);
            options.getOption("l").setRequired(false);

            super.options = options;
        }
        return options;
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

}
