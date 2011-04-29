/*
 * FlowMetadataImportUtils.java
 *
 * Version: $Revision: $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.io.File;

import org.apache.log4j.Logger;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.servlet.multipart.Part;
import org.apache.cocoon.servlet.multipart.PartOnDisk;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.LogManager;

import org.dspace.app.bulkedit.MetadataImport;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.BulkEditChange;

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
        private static final Message T_import_successful = new Message("default", "xmlui.administrative.metadataimport.flow.import_successful");
        private static final Message T_import_failed = new Message("default", "xmlui.administrative.metadataimport.flow.import_failed");
        private static final Message T_over_limit = new Message("default", "xmlui.administrative.metadataimport.flow.over_limit");
        private static final Message T_no_changes = new Message("default", "xmlui.administrative.metadataimport.general.no_changes");

        // Other variables
        private static final int limit = ConfigurationManager.getIntProperty("bulkedit.gui-item-limit", 20);
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
                    MetadataImport mImport = new MetadataImport(context, csv.getCSVLines());
                    ArrayList<BulkEditChange> changes = mImport.runImport(true, false, false, false);

                    // Commit the changes
                    context.commit();
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
                            DSpaceCSV csv = new DSpaceCSV(file);
                            file.delete();

                            MetadataImport mImport = new MetadataImport(context, csv.getCSVLines());
                            ArrayList<BulkEditChange> changes = mImport.runImport(false, false, false, false);
                            log.debug(LogManager.getHeader(context, "metadataimport", changes.size() + " items with changes identifed"));

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
                        catch(MetadataImportException e) {
                            result.setContinue(false);
                            result.setOutcome(false);
                            result.setMessage(T_upload_failed);
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
