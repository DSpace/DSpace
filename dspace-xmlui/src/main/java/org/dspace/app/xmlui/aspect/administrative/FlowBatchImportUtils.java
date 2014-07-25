/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.servlet.multipart.Part;
import org.apache.cocoon.servlet.multipart.PartOnDisk;
import org.dspace.app.bulkedit.BulkEditChange;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.MetadataImport;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.app.bulkedit.MetadataImportInvalidHeadingException;
import org.dspace.app.itemimport.ItemImport;
import org.dspace.app.itemimport.ItemImportOptions;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

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
    private static final Message T_upload_badschema = new Message("default", "xmlui.administrative.batchimport.flow.upload_badschema");
    private static final Message T_upload_badelement = new Message("default", "xmlui.administrative.batchimport.flow.upload_badelement");
    private static final Message T_import_successful = new Message("default", "xmlui.administrative.batchimport.flow.import_successful");
    private static final Message T_import_failed = new Message("default", "xmlui.administrative.batchimport.flow.import_failed");
    private static final Message T_over_limit = new Message("default", "xmlui.administrative.batchimport.flow.over_limit");
    private static final Message T_no_changes = new Message("default", "xmlui.administrative.batchimport.general.no_changes");

    // Other variables
    private static final int limit = ConfigurationManager.getIntProperty("bulkedit", "gui-item-limit", 20);
    private static Logger log = Logger.getLogger(FlowBatchImportUtils.class);

    public static FlowResult processBatchImport(Context context, Request request) throws SQLException, AuthorizeException, IOException, Exception {

        FlowResult result = new FlowResult();
        result.setContinue(false);

        String zipFile = (String) request.getSession().getAttribute("zip");
        log.info(zipFile);

        if (zipFile != null) {
            // Commit the changes
            context.commit();
            request.getSession().removeAttribute("zipFile");

            log.debug(LogManager.getHeader(context, "batchimport", " items changed"));

            if (true) {
                result.setContinue(true);
                result.setOutcome(true);
                result.setMessage(T_import_successful);
            } else {
                result.setContinue(false);
                result.setOutcome(false);
                result.setMessage(T_no_changes);
            }
        } else {
            result.setContinue(false);
            result.setOutcome(false);
            result.setMessage(T_import_failed);
            log.debug(LogManager.getHeader(context, "batchimport", "Changes cancelled"));
        }

        return result;
    }

    public static FlowResult processUploadZIP(Context context, Request request) throws SQLException, AuthorizeException, IOException, Exception {
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
            file = ((PartOnDisk) filePart).getFile();
        }

        if (filePart != null && filePart.getSize() > 0) {
            String name = filePart.getUploadName();

            while (name.indexOf('/') > -1) {
                name = name.substring(name.indexOf('/') + 1);
            }

            while (name.indexOf('\\') > -1) {
                name = name.substring(name.indexOf('\\') + 1);
            }


            log.info(LogManager.getHeader(context, "batchimport", "loading file"));

            // Process CSV without import
            ItemImport itemImport = new ItemImport();

            /*
            ItemImportOptions itemImportOptions = new ItemImportOptions();

            itemImportOptions.setZipFilePath(file);

            Collection destCollection = Collection.find(context, 10);
            itemImportOptions.setCollection(destCollection);

            EPerson epersonSubmitter = EPerson.findByEmail(context, "peter@longsight.com");
            itemImportOptions.setEpersonSubmitter(epersonSubmitter);

            itemImportOptions.setUseTemplate(true);
*/
            File mapFile = File.createTempFile("batch", "map");
/*               PrintWriter mapOut = new PrintWriter(new FileWriter(mapFile));
            itemImportOptions.setMapFileOut(mapOut);
            log.info("MapFile: " + mapFile.getAbsolutePath());
            //Process ItemImport with ZIP, and other options.
*/

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


            String sourceBatchDir = ItemImport.unzip(file);



            Collection col10 = Collection.find(context, 10);
            Collection[] collections = new Collection[1];
            collections[0] = col10;
            itemImport.addItems(context, collections, sourceBatchDir, mapFile.getAbsolutePath(), true);



            if(true)
            {
                // Success!
                // Set session and request attributes

                //request.setAttribute("changes", changes);
                //request.getSession().setAttribute("csv", csv);
                log.info("batch success");

                result.setContinue(true);
                result.setOutcome(true);
                result.setMessage(T_upload_successful);
            }
            else
            {
                //fail
                log.info("batch fail");
                result.setContinue(false);
                result.setOutcome(false);
                result.setMessage(T_no_changes);
            }
        }
        else
        {
            result.setContinue(false);
            result.setOutcome(false);
            result.setMessage(T_upload_failed);
        }

            return result;
        }
}
