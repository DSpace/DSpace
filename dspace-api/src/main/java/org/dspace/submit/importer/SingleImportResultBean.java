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
