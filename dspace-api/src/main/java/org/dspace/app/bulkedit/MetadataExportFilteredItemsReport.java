/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.MetadataDSpaceCsvExportServiceImpl;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentReportServiceFactory;
import org.dspace.content.service.MetadataDSpaceCsvExportService;
import org.dspace.contentreport.Filter;
import org.dspace.contentreport.FilteredItems;
import org.dspace.contentreport.FilteredItemsQuery;
import org.dspace.contentreport.QueryOperator;
import org.dspace.contentreport.QueryPredicate;
import org.dspace.contentreport.service.ContentReportService;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 * Metadata exporter to allow the batch export of metadata from a Filtered Items content report execution into a file
 *
 * @author Jean-François Morin (Université Laval)
 */
public class MetadataExportFilteredItemsReport extends DSpaceRunnable
        <MetadataExportFilteredItemsReportScriptConfiguration<MetadataExportFilteredItemsReport>> {

    private static final String EXPORT_CSV = "exportCSV";
    public static final String DEFAULT_FILENAME = "metadataExportFilteredItems.csv";
    private boolean help = false;
    private String[] collectionUuids;
    private String[] queryPredicates;
    private String[] queryFilters;

    private ConfigurationService configurationService;
    private ContentReportService contentReportService;
    private EPersonService ePersonService;
    private MetadataDSpaceCsvExportService metadataDSpaceCsvExportService;

    @SuppressWarnings("unchecked")
    @Override
    public MetadataExportFilteredItemsReportScriptConfiguration<MetadataExportFilteredItemsReport>
            getScriptConfiguration() {
        return new DSpace().getServiceManager()
            .getServiceByName("metadata-export-filtered-items-report",
                    MetadataExportFilteredItemsReportScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        ServiceManager serviceManager = new DSpace().getServiceManager();
        configurationService = serviceManager.getServicesByType(ConfigurationService.class).get(0);
        contentReportService = ContentReportServiceFactory.getInstance().getContentReportService();
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        metadataDSpaceCsvExportService = serviceManager.getServiceByName(
                MetadataDSpaceCsvExportServiceImpl.class.getCanonicalName(),
                MetadataDSpaceCsvExportService.class);

        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }

        if (commandLine.hasOption('c')) {
            collectionUuids = commandLine.getOptionValues('c');
        }

        if (commandLine.hasOption("qp")) {
            queryPredicates = commandLine.getOptionValues("qp");
        }

        if (commandLine.hasOption('f')) {
            queryFilters = commandLine.getOptionValues('f');
        }
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            loghelpinfo();
            printHelp();
            return;
        }
        handler.logDebug("starting content report export");

        Context context = new Context();
        context.setCurrentUser(ePersonService.find(context, getEpersonIdentifier()));

        List<String> collUuids = List.of();
        if (collectionUuids != null) {
            // Using a temporary Set to eliminate duplicates, if any
            Set<String> setUuids = arrayToStream(collectionUuids)
                    .map(uuids -> uuids.split("[^0-9A-Fa-f\\-]+"))
                    .flatMap(Arrays::stream)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            collUuids = new ArrayList<>(setUuids);
        }

        List<QueryPredicate> predicates = List.of();
        if (queryPredicates != null) {
            predicates = arrayToStream(queryPredicates)
                    .filter(StringUtils::isNotBlank)
                    .map(pred -> buildPredicate(context, pred))
                    .collect(Collectors.toList());
        }

        Set<Filter> filters = EnumSet.noneOf(Filter.class);
        if (queryFilters != null) {
            Arrays.stream(queryFilters)
                    .map(Filter::getFilters)
                    .flatMap(Set::stream)
                    .filter(f -> f != null)
                    .forEach(filters::add);
        }

        handler.logDebug("building query");
        FilteredItemsQuery query = FilteredItemsQuery.of(
                collUuids, predicates, 0, Integer.MAX_VALUE, filters, List.of());
        handler.logDebug("creating iterator");

        FilteredItems items = contentReportService.findFilteredItems(context, query);
        handler.logDebug("creating dspacecsv");
        DSpaceCSV dSpaceCSV = metadataDSpaceCsvExportService.export(context, items.getItems().iterator(),
                true, handler);
        handler.logDebug("writing to file " + getFileNameOrExportFile());
        handler.writeFilestream(context, getFileNameOrExportFile(), dSpaceCSV.getInputStream(), EXPORT_CSV);
        context.restoreAuthSystemState();
        context.complete();
    }

    protected void loghelpinfo() {
        handler.logInfo("metadata-export-filtered-items-report");
    }

    protected String getFileNameOrExportFile() {
        return configurationService.getProperty("contentreport.metadataquery.csv.filename.default", DEFAULT_FILENAME);
    }

    private static Stream<String> arrayToStream(String... array) {
        return Optional.ofNullable(array)
                .stream()
                .flatMap(Arrays::stream)
                .filter(StringUtils::isNotBlank);
    }

    private QueryPredicate buildPredicate(Context context, String exp) {
        String[] tokens = exp.split("\\:");
        String field = tokens.length > 0 ? tokens[0].trim() : "";
        QueryOperator operator = tokens.length > 1 ? QueryOperator.get(tokens[1].trim()) : null;
        String value = tokens.length > 2 ? StringUtils.trimToEmpty(tokens[2]) : "";

        try {
            List<MetadataField> fields = contentReportService.getMetadataFields(context, field);
            return QueryPredicate.of(fields, operator, value);
        } catch (SQLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

}
