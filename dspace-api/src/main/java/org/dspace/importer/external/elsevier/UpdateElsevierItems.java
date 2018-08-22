/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.elsevier;

import java.sql.*;
import java.util.*;
import java.util.Collection;
import org.apache.commons.cli.*;
import org.apache.commons.collections.*;
import org.apache.commons.lang.*;
import org.apache.log4j.*;
import org.dspace.authorize.*;
import org.dspace.content.*;
import org.dspace.content.factory.*;
import org.dspace.content.service.*;
import org.dspace.core.*;
import org.dspace.fileaccess.factory.*;
import org.dspace.fileaccess.service.*;
import org.dspace.handle.factory.*;
import org.dspace.handle.service.*;
import org.dspace.importer.external.datamodel.*;
import org.dspace.importer.external.elsevier.entitlement.*;
import org.dspace.importer.external.exception.*;
import org.dspace.importer.external.metadatamapping.*;
import org.dspace.importer.external.service.*;
import org.dspace.utils.*;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 09/11/15
 * Time: 15:09
 */
public class UpdateElsevierItems {

    private static boolean test = false;
    private static boolean force = false;

    protected static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected static BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected static HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected static FileAccessFromMetadataService fileAccessFromMetadataService = FileAccessServiceFactory.getInstance().getFileAccessFromMetadataService();
    protected static ItemMetadataService itemMetadataService = FileAccessServiceFactory.getInstance().getItemMetadataService();
    private static final List<String> ElsevierImportSources = Arrays.asList(new String[] {"science", "scopus"});
    private static Map<String, AbstractImportMetadataSourceService> sources = new DSpace().getServiceManager().getServiceByName("ImportServices", HashMap.class);

    private static Logger log = Logger.getLogger(UpdateElsevierItems.class);

    public static void main(String[] args) {
        Context context = null;

        try {
            CommandLineParser parser = new PosixParser();

            Options options = CreateCommandLineOptions();
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("t")) {
                test = true;
            }

            if (line.hasOption("f")) {
                force = true;
            }

            context = new Context();
            context.turnOffAuthorisationSystem();

            Iterator<Item> itemIterator = IteratorUtils.emptyIterator();

            if (line.hasOption("i")) {
                DSpaceObject dSpaceObject = handleService.resolveToObject(context, line.getOptionValue("i"));

                if (dSpaceObject.getType() == Constants.ITEM) {
                    itemIterator = IteratorUtils.getIterator(dSpaceObject);
                }
            } else {
                for (String elsevierImportSource : ElsevierImportSources) {
                    Iterator<Item> sourceItemIterator = itemService.findByMetadataField(context, "workflow", "import", "source", elsevierImportSource);
                    itemIterator = IteratorUtils.chainedIterator(itemIterator, sourceItemIterator);
                }
            }

            if (itemIterator != null && itemIterator.hasNext()) {
                while (itemIterator.hasNext()) {
                    Item item = itemIterator.next();

                    if (line.hasOption("p")) {
                        printAndLog("checking permissions updates for item " + item.getHandle());
                        updatePermissions(context, item);
                    }

                    if (line.hasOption("a")) {
                        printAndLog("checking pii updates for item " + item.getHandle());
                        assignPii(context, item);
                    }

                    if (line.hasOption("m")) {
                        printAndLog("checking metadata updates for item " + item.getHandle());
                        importMetadata(context, item);
                    }

                    itemService.update(context,item);
                    context.dispatchEvents();
                }

                context.restoreAuthSystemState();
                context.complete();
            }
            else {
                printAndLog("no items found.");
            }
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }finally{
            if (context != null) {
                context.abort();
            }
        }
    }

    private static Options CreateCommandLineOptions() {
        Options options = new Options();
        Option testOption = OptionBuilder.withArgName("test").withDescription("output changes without applying them").create('t');
        Option permissionOption = OptionBuilder.withArgName("permissions").withDescription("Update the item permissions").create('p');
        Option piiOption = OptionBuilder.withArgName("assignpii").withDescription("Lookups uo the pii for items with a doi (but no pii) and add it to the metadata").create('a');
        Option metadataOption = OptionBuilder.withArgName("metadata").withDescription("import the item metadata").create('m');
        Option forceOption = OptionBuilder.withArgName("force").withDescription("force update changes from elsevier").create('f');
        Option identifierOption = OptionBuilder.withArgName("item").hasArg().withDescription("specify a handle to update a single item").create('i');

        options.addOption(testOption);
        options.addOption(permissionOption);
        options.addOption(piiOption);
        options.addOption(metadataOption);
        options.addOption(forceOption);
        options.addOption(identifierOption);
        return options;
    }

    private static void updatePermissions(Context context, Item item) throws SQLException, AuthorizeException {
        List<Bundle> bundles = item.getBundles();

        for (Bundle bundle : bundles) {
            List<Bitstream> bitstreams = bundle.getBitstreams();

            for (Bitstream bitstream : bitstreams) {
                boolean identical = fileAccessFromMetadataService.fileAccessIdentical(context, bitstream);

                if(!identical){
                    boolean overruled = Boolean.parseBoolean(bitstreamService.getMetadata(bitstream, "workflow.fileaccess.overruled"));

                    if(force || !overruled) {
                        OpenAccessArticleCheck openAccessArticleCheck = OpenAccessArticleCheck.getInstance();
                        ArticleAccess itemFileAccess = openAccessArticleCheck.check(item);

                        if (test) {
                            printAndLog("permission of bitstream with id " + bitstream.getID() + " would be updated to " + itemFileAccess.toString());
                        } else {
                            printAndLog("permission of bitstream with id " + bitstream.getID() + " is updated to " + itemFileAccess.toString());
                            fileAccessFromMetadataService.setFileAccess(context, bitstream, itemFileAccess.getAudience(), itemFileAccess.getStartDate());
                        }
                    }
                }
            }
        }
    }

    private static void assignPii(Context context, Item item) throws MetadataSourceException, SQLException {
        String pii = itemMetadataService.getPII(item);
        String doi = itemMetadataService.getDOI(item);

        String piiMdField = ConfigurationManager.getProperty("external-sources.elsevier.metadata.field.pii");

        String[] split = piiMdField.split("\\.");

        String schema = split[0];
        String element = split[1];
        String qualifier = null;

        if(split.length>2) {
            qualifier = split[2];
        }

        String source = itemService.getMetadataFirstValue(item, "workflow", "import", "source", Item.ANY);

        if(force){
            ImportRecord record = getRecord(source,"doi",doi);

            if(record!=null) {
                Collection<MetadatumDTO> values = record.getValue(schema,element,qualifier);

                if(values.size()==0){
                    if(test){
                        printAndLog("pii for item with id " + item.getID() + " would be removed");
                    }
                    else{
                        printAndLog("pii for item with id " + item.getID() + " is removed");
                        itemService.clearMetadata(context, item, schema, element, qualifier, Item.ANY);
                    }
                }
                else {
                    MetadatumDTO newPii = values.iterator().next();

                    if(!newPii.getValue().equals(pii)){
                        if(test){
                            printAndLog("pii for item with id " + item.getID() + " would be updated to " + newPii.getValue());
                        }
                        else{
                            printAndLog("pii for item with id " + item.getID() + " is updated to " + newPii.getValue());
                            itemService.clearMetadata(context, item, schema, element, qualifier, Item.ANY);
                            itemService.addMetadata(context,item,schema,element,qualifier,null,newPii.getValue());
                        }
                    }
                }
            }
        }
        else if(StringUtils.isNotBlank(doi) && StringUtils.isBlank(pii)){
            ImportRecord record = getRecord(source, "doi",doi);

            if (record!=null) {
                Collection<MetadatumDTO> values = record.getValue(schema,element,qualifier);

                MetadatumDTO newPii = values.iterator().next();

                if(test){
                    printAndLog("pii " + newPii.getValue() + " would be added to item with id " + item.getID());
                }
                else{
                    printAndLog("pii " + newPii.getValue() + " is added to item with id " + item.getID());
                    itemService.addMetadata(context, item, schema, element, qualifier,null,newPii.getValue());
                }
            }
        }
    }

    private static void importMetadata(Context context, Item item) throws MetadataSourceException, SQLException {
        String pii = itemMetadataService.getPII(item);
        String doi = itemMetadataService.getDOI(item);

        if(force){
            ImportRecord record = null;
            String source = itemService.getMetadataFirstValue(item, "workflow", "import", "source", Item.ANY);

            if(StringUtils.isNotBlank(pii)){
                record = getRecord(source, "pii",pii);
            }
            else if (StringUtils.isNotBlank(doi)){
                record = getRecord(source, "doi",doi);
            }

            if(record!=null){
                for (MetadatumDTO recordMetadatum : record.getValueList()) {
                    List<MetadataValue> metadata = itemService.getMetadata(item, recordMetadatum.getSchema(), recordMetadatum.getElement(), recordMetadatum.getQualifier(), Item.ANY);

                    boolean addMetadata = true;
                    for (MetadataValue itemMetadata : metadata) {
                        if(itemMetadata.getValue().equals(StringUtils.trim(recordMetadatum.getValue()))){
                            addMetadata = false;
                        }
                    }

                    if(addMetadata){
                        if(test){
                            printAndLog("metadata " + recordMetadatum.getField() + " would be updated with value " + recordMetadatum.getValue() + " for item with id " + item.getID());
                        }
                        else {
                            printAndLog("metadata " + recordMetadatum.getField() + " is updated with value " + recordMetadatum.getValue() + " for item with id " + item.getID());
                            itemService.addMetadata(context, item, recordMetadatum.getSchema(), recordMetadatum.getElement(), recordMetadatum.getQualifier(),recordMetadatum.getLanguage(),recordMetadatum.getValue());
                        }
                    }
                }
            }
        }
    }

    private static ImportRecord getRecord(String source, String field, String value) throws MetadataSourceException {
        AbstractImportMetadataSourceService importMetadataSourceService = sources.get(source);

        if(importMetadataSourceService != null) {
            Collection<ImportRecord> records = importMetadataSourceService.getRecords(field + "(\"" + value + "\")", 0, 1);

            if (records.size() > 0) {
                return records.iterator().next();
            }
        }

        return null;
    }

    private static void printAndLog(String message) {
        System.out.println(message);
        log.info(message);
    }
}
