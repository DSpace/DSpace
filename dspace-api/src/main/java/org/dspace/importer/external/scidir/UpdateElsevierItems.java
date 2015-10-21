/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scidir;

import org.apache.commons.cli.*;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.fileaccess.service.FileAccessFromMetadataService;
import org.dspace.fileaccess.service.ItemMetadataService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.fileaccess.factory.FileAccessServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.importer.external.MetadataSourceException;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.scidir.entitlement.ArticleAccess;
import org.dspace.importer.external.scidir.entitlement.OpenAccessArticleCheck;
import org.dspace.importer.external.service.ImportService;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 09/11/15
 * Time: 15:09
 */
public class UpdateElsevierItems {

    private static boolean test = false;
    private static boolean force = false;

    private static ImportService importService;
    private static String url;

    protected static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected static BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected static HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected static FileAccessFromMetadataService fileAccessFromMetadataService = FileAccessServiceFactory.getInstance().getFileAccessFromMetadataService();
    protected static ItemMetadataService itemMetadataService = FileAccessServiceFactory.getInstance().getItemMetadataService();

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

            Iterator<Item> itemIterator = null;

            if (line.hasOption("i")) {
                DSpaceObject dSpaceObject = handleService.resolveToObject(context, line.getOptionValue("i"));

                if (dSpaceObject.getType() == Constants.ITEM) {
                    itemIterator = IteratorUtils.getIterator(dSpaceObject);
                }
            } else {
                itemIterator = itemService.findAll(context);
            }

            if (itemIterator != null && itemIterator.hasNext()) {
                while (itemIterator.hasNext()) {
                    Item item = itemIterator.next();

                    if (line.hasOption("p")) {
                        updatePermissions(context, item);
                    }

                    if (line.hasOption("a")) {
                        assignPii(context, item);
                    }

                    if (line.hasOption("m")) {
                        importMetadata(context, item);
                    }

                    itemService.update(context,item);
                    context.dispatchEvents();
                }

                context.restoreAuthSystemState();
                context.complete();
            }
            else {
                System.out.println("no items found.");
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
        Option identifierOption = OptionBuilder.withArgName("i").hasArg().withDescription("specify a handle to update a single item").create('i');

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
                            System.out.println("permission of bitstream with id " + bitstream.getID() + " would be updated to " + itemFileAccess.toString());
                        } else {
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

        String piiMdField = ConfigurationManager.getProperty("elsevier-sciencedirect", "metadata.field.pii");

        String[] split = piiMdField.split("\\.");

        String schema = split[0];
        String element = split[1];
        String qualifier = null;

        if(split.length>2) {
            qualifier = split[2];
        }

        if(force){
            ImportRecord record = getRecord("doi",doi);

            if(record!=null) {
                Collection<MetadatumDTO> values = record.getValue(schema,element,qualifier);

                if(values.size()==0){
                    if(test){
                        System.out.println("pii for item with id " + item.getID() + " would be removed");
                    }
                    else{
                        itemService.clearMetadata(context, item, schema, element, qualifier, Item.ANY);
                    }
                }
                else {
                    MetadatumDTO newPii = values.iterator().next();

                    if(!newPii.getValue().equals(pii)){
                        if(test){
                            System.out.println("pii for item with id " + item.getID() + " would be updated to " + newPii.getValue());
                        }
                        else{
                            itemService.clearMetadata(context, item, schema, element, qualifier, Item.ANY);
                            itemService.addMetadata(context,item,schema,element,qualifier,null,newPii.getValue());
                        }
                    }
                }
            }
        }
        else if(StringUtils.isNotBlank(doi) && StringUtils.isBlank(pii)){
            ImportRecord record = getRecord("doi",doi);

            if (record!=null) {
                Collection<MetadatumDTO> values = record.getValue(schema,element,qualifier);

                MetadatumDTO newPii = values.iterator().next();

                if(test){
                    System.out.println("pii " + newPii.getValue() + " would be added to item with id " + item.getID());
                }
                else{
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
            if(StringUtils.isNotBlank(pii)){
                record = getRecord("pii",pii);
            }
            else if (StringUtils.isNotBlank(doi)){
                record = getRecord("doi",doi);
            }

            if(record!=null){
                for (MetadatumDTO recordMetadatum : record.getValueList()) {
                    List<MetadataValue> metadata = itemService.getMetadata(item, recordMetadatum.getSchema(), recordMetadatum.getElement(), recordMetadatum.getQualifier(), Item.ANY);

                    boolean addMetadata = true;
                    for (MetadataValue itemMetadata : metadata) {
                        if(itemMetadata.getValue().equals(recordMetadatum.getValue())){
                            addMetadata = false;
                        }
                    }

                    if(addMetadata){
                        if(test){
                            System.out.println("metadata " + recordMetadatum.getField() + " would be updated with value " + recordMetadatum.getValue() + " for item with id " + item.getID());
                        }
                        else {
                            itemService.addMetadata(context, item, recordMetadatum.getSchema(), recordMetadatum.getElement(), recordMetadatum.getQualifier(),recordMetadatum.getLanguage(),recordMetadatum.getValue());
                        }
                    }
                }
            }
        }
    }

    private static ImportRecord getRecord(String field, String value) throws MetadataSourceException {
        if(importService==null){
            importService = new DSpace().getServiceManager().getServiceByName(null, ImportService.class);
        }

        if(StringUtils.isBlank(url)) {
            url = ConfigurationManager.getProperty("elsevier-sciencedirect", "api.scidir.url");
        }

        Collection<ImportRecord> records = importService.getRecords(url,  field + "(\"" + value + "\")", 0, 1);

        if(records.size()>0) {
            return records.iterator().next();
        }

        return null;
    }
}
