/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
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
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.LogManager;

/**
 * Utility methods to processes MetadataImport actions. These methods are used
 * exclusively from the administrative flow scripts.
 * 
 * @author Kim Shepherd
 */

public class FlowMetadataImportUtils 
{

	/** Language Strings */
	private static final Message T_upload_successful = new Message("default", "xmlui.administrative.metadataimport.flow.upload_successful");
    private static final Message T_upload_failed = new Message("default", "xmlui.administrative.metadataimport.flow.upload_failed");
    private static final Message T_upload_badschema = new Message("default", "xmlui.administrative.metadataimport.flow.upload_badschema");
    private static final Message T_upload_badelement = new Message("default", "xmlui.administrative.metadataimport.flow.upload_badelement");
    private static final Message T_import_successful = new Message("default", "xmlui.administrative.metadataimport.flow.import_successful");
    private static final Message T_import_failed = new Message("default", "xmlui.administrative.metadataimport.flow.import_failed");
    private static final Message T_over_limit = new Message("default", "xmlui.administrative.metadataimport.flow.over_limit");
    private static final Message T_no_changes = new Message("default", "xmlui.administrative.metadataimport.general.no_changes");

    // Other variables
    private static final int limit = DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("bulkedit.gui-item-limit", 20);
    private static Logger log = Logger.getLogger(FlowMetadataImportUtils.class);

    public static FlowResult processMetadataImport(Context context,Request request) throws SQLException, AuthorizeException, IOException, Exception
    {

        FlowResult result = new FlowResult();
        result.setContinue(false);

        DSpaceCSV csv = (DSpaceCSV)request.getSession().getAttribute("csv");

        if(csv != null)
        {
            try {

                // Run the import
                MetadataImport mImport = new MetadataImport(context, csv);
                List<BulkEditChange> changes = mImport.runImport(true, false, false, false);

                request.setAttribute("changes",changes);
                request.getSession().removeAttribute("csv");

                log.debug(LogManager.getHeader(context, "metadataimport", changes.size() + " items changed"));

                if(changes.size() > 0) {
                    result.setContinue(true);
                    result.setOutcome(true);
                    result.setMessage(T_import_successful);
                }
                else {
                    result.setContinue(false);
                    result.setOutcome(false);
                    result.setMessage(T_no_changes);
                }

            }
            catch(MetadataImportException e) {
                result.setContinue(false);
                result.setOutcome(false);
                result.addError(e.getLocalizedMessage());
                result.setMessage(T_import_failed);
                log.debug(LogManager.getHeader(context, "metadataimport", "Error encountered while making changes - " + e.getMessage()));
            }
        }
        else {
            result.setContinue(false);
            result.setOutcome(false);
            result.setMessage(T_import_failed);
            log.debug(LogManager.getHeader(context, "metadataimport", "Changes cancelled"));
        }

        return result;
    }

    public static FlowResult processUploadCSV(Context context, Request request) throws SQLException, AuthorizeException, IOException, Exception
{
    FlowResult result = new FlowResult();
    result.setContinue(false);

            Object object = null;

            if(request.get("file") != null) {
                object = request.get("file");
            }

            Part filePart = null;
            File file = null;

            if (object instanceof Part)
            {
                    filePart = (Part) object;
                    file = ((PartOnDisk)filePart).getFile();
            }

            if (filePart != null && filePart.getSize() > 0)
            {
                    String name = filePart.getUploadName();

                    while (name.indexOf('/') > -1)
                    {
                            name = name.substring(name.indexOf('/') + 1);
                    }

                    while (name.indexOf('\\') > -1)
                    {
                            name = name.substring(name.indexOf('\\') + 1);
                    }

                    try {

                        log.info(LogManager.getHeader(context, "metadataimport", "loading file"));

                        // Process CSV without import
                        DSpaceCSV csv = new DSpaceCSV(file, context);
                        if (!file.delete())
                        {
                            log.error("Unable to delete CSV file");
                        }

                        MetadataImport mImport = new MetadataImport(context, csv);
                        List<BulkEditChange> changes = mImport.runImport(false, false, false, false);
                        log.debug(LogManager.getHeader(context, "metadataimport", changes.size() + " items with changes identified"));

                        if(changes.size() > 0)
                        {
                            if(changes.size() > limit)
                            {
                                result.setContinue(false);
                                result.setOutcome(false);
                                result.setMessage(T_over_limit);

                                log.info(LogManager.getHeader(context, "metadataimport", "too many changes - " +
                                                  changes.size() + " (" + limit + " allowed)"));
                            }
                            else
                            {
                                // Success!
                                // Set session and request attributes

                                request.setAttribute("changes", changes);
                                request.getSession().setAttribute("csv", csv);
                                result.setContinue(true);
                                result.setOutcome(true);
                                result.setMessage(T_upload_successful);
                            }

                        }
                        else
                        {
                            result.setContinue(false);
                            result.setOutcome(false);
                            result.setMessage(T_no_changes);
                        }
                    }
                    catch(MetadataImportInvalidHeadingException mihe) {
                        result.setContinue(false);
                        result.setOutcome(false);
                        if (mihe.getType().equals("" + MetadataImportInvalidHeadingException.SCHEMA))
                        {
                            result.setMessage(T_upload_badschema);
                        }
                        else
                        {
                            result.setMessage(T_upload_badelement);
                        }
                        result.setCharacters(mihe.getBadHeader());
                    }
                    catch(MetadataImportException e) {
                        result.setContinue(false);
                        result.setOutcome(false);
                        result.setMessage(T_upload_failed);
                        result.setCharacters(e.getMessage());
                        log.debug(LogManager.getHeader(context, "metadataimport", "Error encountered while looking for changes - " + e.getMessage()));
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
