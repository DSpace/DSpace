/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.json;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.dspace.app.webui.util.FileUploadListener;
import org.dspace.app.webui.util.FileUploadRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

import com.google.gson.Gson;

public class UploadProgressJSON extends JSONRequest
{
    @Override
    public void doJSONRequest(Context context, HttpServletRequest req,
            HttpServletResponse resp) throws AuthorizeException, IOException
    {
        HttpSession session = req.getSession(false);
        if (session == null)
        {
            return;
        }

        FileUploadListener listner = (FileUploadListener) session
                .getAttribute(FileUploadRequest.FILE_UPLOAD_LISTNER);
        if (listner == null || listner.getContentLength() == 0)
        {
            return;
        }
        else
        {
            long contentLength = listner.getContentLength();
            UploadProgressDTO dto = new UploadProgressDTO();
            long bytesRead = listner.getBytesRead();
            dto.readBytes = bytesRead;
            dto.totalBytes = contentLength;
            Gson gson = new Gson();
            resp.getWriter().write(gson.toJson(dto));
            if (listner.isCompleted())
            {
                session.removeAttribute(FileUploadRequest.FILE_UPLOAD_LISTNER);
            }
        }

    }
}

class UploadProgressDTO {
    long totalBytes;
    long readBytes;
}
