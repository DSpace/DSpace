/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link MetadataExportFilteredItemsReport} script
 *
 * @author Jean-François Morin (Université Laval)
 */
public class MetadataExportFilteredItemsReportScriptConfiguration<T extends MetadataExportFilteredItemsReport>
        extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableclass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableclass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        dspaceRunnableclass = dspaceRunnableClass;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();
            options.addOption("c", "collections", true,
                "UUIDs of collections to search for eligible records");
            options.getOption("c").setType(String.class);
            options.addOption("qp", "queryPredicates", true,
                "Predicates or field queries used as criteria to filter records");
            options.getOption("qp").setType(String.class);
            options.addOption("f", "filters", true, """
                Filters from the org.dspace.contentreport.Filter enumeration
                used to filter records. Any filtered included here is considered as being selected,
                and is considered unselected otherwise.""");
            options.getOption("f").setType(String.class);
            options.addOption("h", "help", false, "help");

            super.options =  options;
        }
        return options;
    }

}
