package org.dspace.submit.importer;

import java.util.List;

public class ImportResultBean
{
    private boolean scheduled;

    private int tot;

    private int failure;

    private int success;

    private int warning;

    private List<SingleImportResultBean> details;

    public boolean isScheduled()
    {
        return scheduled;
    }

    public void setScheduled(boolean scheduled)
    {
        this.scheduled = scheduled;
    }

    public int getTot()
    {
        return tot;
    }

    public void setTot(int tot)
    {
        this.tot = tot;
    }

    public int getFailure()
    {
        return failure;
    }

    public int getSuccess()
    {
        return success;
    }

    public int getWarning()
    {
        return warning;
    }

    public List<SingleImportResultBean> getDetails()
    {
        return details;
    }

    public void setDetails(List<SingleImportResultBean> details)
    {
        this.details = details;
        int success = 0;
        int failure = 0;
        int warning = 0;
        if (details != null)
        {
            for (SingleImportResultBean result : details)
            {
                switch (result.getStatus())
                {
                case SingleImportResultBean.ERROR:
                    failure++;
                    break;
                case SingleImportResultBean.WARNING:
                    warning++;
                    break;
                case SingleImportResultBean.SUCCESS:
                    success++;
                    break;
                }
            }
        }
        this.failure = failure;
        this.success = success;
        this.warning = warning;
    }
}
