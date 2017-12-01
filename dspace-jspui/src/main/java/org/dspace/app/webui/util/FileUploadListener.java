/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import org.apache.commons.fileupload.ProgressListener;

public class FileUploadListener implements ProgressListener
{
    private volatile long bytesRead = 0L, contentLength = 0L, item = 0L;

    public FileUploadListener()
    {
        super();
    }

    public void update(long aBytesRead, long aContentLength, int anItem)
    {
        bytesRead = aBytesRead;
        contentLength = aContentLength;
        item = anItem;
    }

    public long getBytesRead()
    {
        return bytesRead;
    }

    public long getContentLength()
    {
        return contentLength;
    }

    public long getItem()
    {
        return item;
    }

    public boolean isCompleted()
    {
        return bytesRead == contentLength;
    }
}
