/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.importer;

public class SingleImportResultBean
{
    public static final int ERROR = 2;

    public static final int WARNING = 1;

    public static final int SUCCESS = 0;

    private int status;

    private int witemId;

    private String message;

    private String importIdentifier;

    private String importData;

    public SingleImportResultBean(int status, int itemId, String message,
            String importIdentifier, String importData)
    {
        super();
        this.status = status;
        this.witemId = itemId;
        this.message = message;
        this.importIdentifier = importIdentifier;
        this.importData = importData;
    }

    public int getStatus()
    {
        return status;
    }

    public int getWitemId()
    {
        return witemId;
    }

    public String getMessage()
    {
        return message;
    }

    public String getImportIdentifier()
    {
        return importIdentifier;
    }

    public String getImportData()
    {
        return importData;
    }

}
