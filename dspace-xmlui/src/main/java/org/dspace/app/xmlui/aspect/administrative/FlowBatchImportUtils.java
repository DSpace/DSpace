/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.apache.cocoon.environment.*;
import org.apache.cocoon.servlet.multipart.*;
import org.apache.commons.io.*;
import org.apache.commons.lang.*;
import org.apache.log4j.*;
import org.dspace.app.itemimport.factory.*;
import org.dspace.app.itemimport.service.*;
import org.dspace.app.xmlui.cocoon.servlet.multipart.*;
import org.dspace.app.xmlui.wing.*;
import org.dspace.authorize.*;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.factory.*;
import org.dspace.handle.service.*;

/**
 * Utility methods to processes BatchImport actions. These methods are used
 * exclusively from the administrative flow scripts.
 *
 * @author Peter Dietz
 */

public class FlowBatchImportUtils {

    /**
     * Language Strings
     */
    private static final Message T_upload_successful = new Message("default", "xmlui.administrative.batchimport.flow.upload_successful");
    private static final Message T_upload_failed = new Message("default", "xmlui.administrative.batchimport.flow.upload_failed");
    private static final Message T_import_successful = new Message("default", "xmlui.administrative.batchimport.flow.import_successful");
    private static final Message T_import_failed = new Message("default", "xmlui.administrative.batchimport.flow.import_failed");
    private static final Message T_no_changes = new Message("default", "xmlui.administrative.batchimport.general.no_changes");
    private static final Message T_failed_no_collection = new Message("default", "xmlui.administrative.batchimport.flow.failed_no_collection");

    // Other variables
    private static Logger log = Logger.getLogger(FlowBatchImportUtils.class);

    protected static final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected static final ItemImportService itemImportService = ItemImportServiceFactory.getInstance().getItemImportService();

    public static FlowResult processBatchImport(Context context, Request request) throws SQLException, AuthorizeException, IOException, Exception {

        FlowResult result = new FlowResult();
        result.setContinue(false);

        String zipFile = (String) request.getSession().getAttribute("zip");

        if (zipFile != null) {
            request.getSession().removeAttribute("zipFile");

            log.debug(LogManager.getHeader(context, "batchimport", " items changed"));

            result.setContinue(true);
            result.setOutcome(true);
            result.setMessage(T_import_successful);
        } else {
            result.setContinue(false);
            result.setOutcome(false);
            result.setMessage(T_import_failed);
            log.debug(LogManager.getHeader(context, "batchimport", "Changes cancelled"));
        }

        return result;
    }

    public static FlowResult processUploadZIP(Context context, Request request) {
        try {
            FlowResult result = new FlowResult();
            result.setContinue(false);

            Object object = null;

            if (request.get("file") != null) {
                object = request.get("file");
            }

            Part filePart = null;
            File file = null;

            if (object instanceof Part) {
                filePart = (Part) object;
                file = ((DSpacePartOnDisk) filePart).getFile();
            }

            if (filePart != null && filePart.getSize() > 0) {
                String name = filePart.getUploadName();

                while (name.indexOf('/') > -1) {
                    name = name.substring(name.indexOf('/') + 1);
                }

                while (name.indexOf('\\') > -1) {
                    name = name.substring(name.indexOf('\\') + 1);
                }

                // Process CSV without import
                String collectionHandle = String.valueOf(request.get("collectionHandle"));
                if (StringUtils.isEmpty(collectionHandle) || !collectionHandle.contains("/")) {
                    //fail
                    log.error("UIBatchImport failed due to no collection.");
                    result.setContinue(false);
                    result.setOutcome(false);
                    result.setMessage(T_failed_no_collection);
                    return result;
                }

                List<Collection> collections = new ArrayList<>();

                try {
                    Collection collection = (Collection) handleService.resolveToObject(context, collectionHandle);
                    collections.add(collection);
                } catch (SQLException e) {
                    log.error("UIBatchImport failed due to collection not existing.", e);
                    result.setContinue(false);
                    result.setOutcome(false);
                    result.setMessage(T_failed_no_collection);
                    return result;
                }

                File mapFile = null;
                try {
                    mapFile = File.createTempFile(file.getName(), ".map", itemImportService.getTempWorkDirFile());
                } catch (IOException e) {
                    log.error("BatchImportUI Unable to create mapfile", e);
                    result.setContinue(false);
                    result.setOutcome(false);
                    result.setMessage(T_import_failed);
                    return result;
                }


                log.info("Attempt UIBatchImport to collection: " + collections.get(0).getName()
                        + ", zip: " + file.getName()
                        + ", map: " + mapFile.getAbsolutePath());

            /*
             // equivalent command-line would be:
             import -a -e <email> -c <collection/handle> -s <parent-dir-of-zip> -z <filename-of-zip> -m <mapfile> --template

             -c,--collection <arg>   destination collection(s) Handle or database ID
             -e,--eperson <arg>      email of eperson doing importing
             -m,--mapfile <arg>      mapfile items in mapfile
             -n,--notify             if sending submissions through the workflow, send
                                     notification emails
             -p,--template           apply template
             -q,--quiet              don't display metadata

             -s,--source <arg>       source of items (directory)
             -t,--test               test run - do not actually import items
             -w,--workflow           send submission through collection's workflow
             -z,--zip <arg>          name of zip file

             //Control
                  -a,--add                add items to DSpace
                  -R,--resume             resume a failed import (add only)
             */

                String sourceBatchDir = null;
                try {
                    sourceBatchDir = itemImportService.unzip(file);
                } catch (IOException e) {
                    log.error("BatchImportUI Unable to unzip", e);
                    result.setContinue(false);
                    result.setOutcome(false);
                    result.setMessage(T_import_failed);
                    return result;
                }

                //TODO, Should we run this in TEST mode first, to ensure we get a clean pass?
                try {
                    itemImportService.addItemsAtomic(context, collections, sourceBatchDir, mapFile.getAbsolutePath(), true);
                } catch (Exception e) {
                    log.error("BatchImportUI - Failure during import: " + e.getMessage());
                    result.setContinue(false);
                    result.setOutcome(false);
                    result.setMessage(T_import_failed);
                    try {
                        result.setCharacters(FileUtils.readFileToString(mapFile) + e.getMessage());
                    } catch (IOException ioe) {
                        result.setCharacters("Unable to show map file: " + ioe.getMessage());
                        log.error("BatchImportUI - Unable to print the mapfile to response", ioe);
                    }
                    return result;
                }

                // Success!
                // Set session and request attributes
                result.setContinue(true);
                result.setOutcome(true);
                result.setMessage(T_upload_successful);
                try {
                    result.setCharacters(FileUtils.readFileToString(mapFile));
                } catch (IOException e) {
                    log.error("BatchImportUI - Unable to print the mapfile to response", e);
                }

                log.info("Success! UIBatchImport to collection: " + collections.get(0).getName()
                        + ", zip: " + file.getName()
                        + ", map: " + mapFile.getAbsolutePath());
            } else {
                //No ZIP File, or upload failed
                result.setContinue(false);
                result.setOutcome(false);
                result.setMessage(T_upload_failed);
            }

            return result;
        }finally {
            itemImportService.cleanupZipTemp();
        }
    }
}
