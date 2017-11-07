package ar.edu.unlp.sedici.aspect.extraSubmission.submit;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

public class UploadStep extends org.dspace.submit.step.UploadStep {

    @Override
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
    	// BEGIN SeDiCI: determina si el archivo es requerido dependiendo del Grupo del usuario
    	// Si la configuración dice que el upload es optativo, es optativo para todo el mundo, en cambio, si
    	// dice que es obligatorio, es obligatorio solo para los roles que no se listan.
    	boolean fileRequired = ConfigurationManager.getBooleanProperty("webui.submit.upload.required", true);
    	if(fileRequired) {
    		String excludedGroupName = ConfigurationManager.getProperty("webui.submit.upload.excluded_group");
    		Group excludedGroup = Group.findByName(context, excludedGroupName);
    		fileRequired = excludedGroup == null || !Group.isMember(context, excludedGroup.getID());
    	}
    	// END SeDiCI
    	
        // get button user pressed
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);

        // get reference to item
        Item item = subInfo.getSubmissionItem().getItem();

        // -----------------------------------
        // Step #0: Upload new files (if any)
        // -----------------------------------
        String contentType = request.getContentType();

        // if multipart form, then we are uploading a file
        if ((contentType != null)
                && (contentType.indexOf("multipart/form-data") != -1))
        {
            // This is a multipart request, so it's a file upload
            // (return any status messages or errors reported)
            int status = processUploadFile(context, request, response, subInfo);

            // if error occurred, return immediately
            if (status != STATUS_COMPLETE)
            {
                return status;
            }
        }
            
        // if user pressed jump-to button in process bar,
        // return success (so that jump will occur)
        if (buttonPressed.startsWith(PROGRESS_BAR_PREFIX))
        {
            // check if a file is required to be uploaded
            if (fileRequired && !item.hasUploadedFiles())
            {
                return STATUS_NO_FILES_ERROR;
            }
            else
            {
                return STATUS_COMPLETE;
            }
        }

        // ---------------------------------------------
        // Step #1: Check if this was just a request to
        // edit file information.
        // (or canceled editing information)
        // ---------------------------------------------
        // check if we're already editing a specific bitstream
        if (request.getParameter("bitstream_id") != null)
        {
            if (buttonPressed.equals(CANCEL_EDIT_BUTTON))
            {
                // canceled an edit bitstream request
                subInfo.setBitstream(null);

                // this flag will just return us to the normal upload screen
                return STATUS_EDIT_COMPLETE;
            }
            else
            {
                // load info for bitstream we are editing
                Bitstream b = Bitstream.find(context, Integer.parseInt(request
                        .getParameter("bitstream_id")));

                // save bitstream to submission info
                subInfo.setBitstream(b);
            }
        }
        else if (buttonPressed.startsWith("submit_edit_"))
        {
            // get ID of bitstream that was requested for editing
            String bitstreamID = buttonPressed.substring("submit_edit_"
                    .length());

            Bitstream b = Bitstream
                    .find(context, Integer.parseInt(bitstreamID));

            // save bitstream to submission info
            subInfo.setBitstream(b);

            // return appropriate status flag to say we are now editing the
            // bitstream
            return STATUS_EDIT_BITSTREAM;
        }

        // ---------------------------------------------
        // Step #2: Process any remove file request(s)
        // ---------------------------------------------
        // Remove-selected requests come from Manakin
        if (buttonPressed.equalsIgnoreCase("submit_remove_selected"))
        {
            // this is a remove multiple request!

            if (request.getParameter("remove") != null)
            {
                // get all files to be removed
                String[] removeIDs = request.getParameterValues("remove");

                // remove each file in the list
                for (int i = 0; i < removeIDs.length; i++)
                {
                    int id = Integer.parseInt(removeIDs[i]);

                    int status = processRemoveFile(context, item, id);

                    // if error occurred, return immediately
                    if (status != STATUS_COMPLETE)
                    {
                        return status;
                    }
                }

                // remove current bitstream from Submission Info
                subInfo.setBitstream(null);
            }
        }
        else if (buttonPressed.startsWith("submit_remove_"))
        {
            // A single file "remove" button must have been pressed

            int id = Integer.parseInt(buttonPressed.substring(14));
            int status = processRemoveFile(context, item, id);

            // if error occurred, return immediately
            if (status != STATUS_COMPLETE)
            {
                return status;
            }

            // remove current bitstream from Submission Info
            subInfo.setBitstream(null);
        }

        // -------------------------------------------------
        // Step #3: Check for a change in file description
        // -------------------------------------------------
        String fileDescription = request.getParameter("description");

        if (fileDescription != null && fileDescription.length() > 0)
        {
            // save this file description
            int status = processSaveFileDescription(context, request, response,
                    subInfo);

            // if error occurred, return immediately
            if (status != STATUS_COMPLETE)
            {
                return status;
            }
        }

        // ------------------------------------------
        // Step #4: Check for a file format change
        // (if user had to manually specify format)
        // ------------------------------------------
        int formatTypeID = Util.getIntParameter(request, "format");
        String formatDesc = request.getParameter("format_description");

        // if a format id or description was found, then save this format!
        if (formatTypeID >= 0
                || (formatDesc != null && formatDesc.length() > 0))
        {
            // save this specified format
            int status = processSaveFileFormat(context, request, response,
                    subInfo);

            // if error occurred, return immediately
            if (status != STATUS_COMPLETE)
            {
                return status;
            }
        }

        // ---------------------------------------------------
        // Step #5: Check if primary bitstream has changed
        // -------------------------------------------------
        if (request.getParameter("primary_bitstream_id") != null)
        {
            Bundle[] bundles = item.getBundles("ORIGINAL");
            if (bundles.length > 0)
            {
            	bundles[0].setPrimaryBitstreamID(Integer.valueOf(request
                    .getParameter("primary_bitstream_id")).intValue());
            	bundles[0].update();
            }
        }

        // ---------------------------------------------------
        // Step #6: Determine if there is an error because no
        // files have been uploaded.
        // ---------------------------------------------------
        //check if a file is required to be uploaded
        if (fileRequired && !item.hasUploadedFiles())
        {
            return STATUS_NO_FILES_ERROR;
        }

        // commit all changes to database
        context.commit();

        return STATUS_COMPLETE;
    }
	
}
