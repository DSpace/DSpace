/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JSONUploadResponse
{
    List<JSONUploadFileStatus> files = new ArrayList<JSONUploadFileStatus>();

    JSONFileSizeLimitExceeded fileSizeLimitExceeded;
    
    public void addUploadFileStatus(String name, UUID bitstreamID,
            long size, String url, int status)
    {
        JSONUploadFileStatus uploadFileStatus = new JSONUploadFileStatus();
        uploadFileStatus.name = name;
        uploadFileStatus.bitstreamID = bitstreamID.toString();
        uploadFileStatus.size = size;
        uploadFileStatus.url = url;
        uploadFileStatus.status = status;
        files.add(uploadFileStatus);
    }

    public void addUploadFileSizeLimitExceeded(long actualSize,
            long permittedSize)
    {
        this.fileSizeLimitExceeded = new JSONFileSizeLimitExceeded();
        fileSizeLimitExceeded.actualSize = actualSize;
        fileSizeLimitExceeded.permittedSize = permittedSize;        
    }
}

class JSONUploadFileStatus
{
    String name;

    String bitstreamID;

    long size;

    String url;

    int status;
}

class JSONFileSizeLimitExceeded
{
    long actualSize;
    long permittedSize;
}
