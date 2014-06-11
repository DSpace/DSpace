/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics;


import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SummaryStatBean
{
    private int freq;

    private String objectName;

    private String objectURL;

    private String statURL;

    private Object object;

    private List<StatDataBean> data;

    private int type;

    public int getFreq()
    {
        return freq;
    }

    public void setFreq(int freq)
    {
        this.freq = freq;
    }

    public String getObjectName()
    {
        return objectName;
    }

    public void setObjectName(String objectName)
    {
        this.objectName = objectName;
    }

    public String getObjectURL()
    {
        return objectURL;
    }

    public void setObjectURL(String objectURL)
    {
        this.objectURL = objectURL;
    }

    public Object getObject()
    {
        return object;
    }

    public void setObject(Object object)
    {
        this.object = object;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public int getType()
    {
        return type;
    }

    public void setStatURL(String statURL)
    {
        this.statURL = statURL;
    }

    public String getStatURL()
    {
        return statURL;
    }

    public List<StatDataBean> getData()
    {
        return data;
    }

    public void setData(List<StatDataBean> data)
    {
        this.data = data;
    }

    public class StatDataBean
    {
        private Long totalSelectedView;            
        private Long periodSelectedView;
        
        private Long totalSelectedDownload;
        private Long periodSelectedDownload;
        
        // foreach key list contains period at 0 index and total stats at 1 index position
        private Map<String, List<Long>> periodAndTotalTopView;
        private Map<String, List<Long>> periodAndTotalTopDownload;
        
        
        private Date date;
        
        private boolean showSelectedObjectDownload = true;
        
        public Date getDate()
        {
            return date;
        }
        
        public void setDate(Date date)
        {
            this.date = date;
        }

        public Long getTotalSelectedView()
        {
            return totalSelectedView;
        }

        public void setTotalSelectedView(Long totalSelectedView)
        {
            this.totalSelectedView = totalSelectedView;
        }

        public Long getPeriodSelectedView()
        {
            return periodSelectedView;
        }

        public void setPeriodSelectedView(Long periodSelectedView)
        {
            this.periodSelectedView = periodSelectedView;
        }

        public Long getTotalSelectedDownload()
        {
            return totalSelectedDownload;
        }

        public void setTotalSelectedDownload(Long totalSelectedDownload)
        {
            this.totalSelectedDownload = totalSelectedDownload;
        }

        public Long getPeriodSelectedDownload()
        {
            return periodSelectedDownload;
        }

        public void setPeriodSelectedDownload(Long periodSelectedDownload)
        {
            this.periodSelectedDownload = periodSelectedDownload;
        }

        public Map<String, List<Long>> getPeriodAndTotalTopView()
        {
            if(this.periodAndTotalTopView==null) {
                this.periodAndTotalTopView = new TreeMap<String, List<Long>>();
            }
            return periodAndTotalTopView;
        }

        public void setPeriodAndTotalTopView(
                Map<String, List<Long>> totalAndPeriodTopView)
        {
            this.periodAndTotalTopView = totalAndPeriodTopView;
        }

        public Map<String, List<Long>> getPeriodAndTotalTopDownload()
        {
            if(this.periodAndTotalTopDownload==null) {
                this.periodAndTotalTopDownload = new TreeMap<String, List<Long>>();
            }
            return periodAndTotalTopDownload;
        }

        public void setPeriodAndTotalTopDownload(
                Map<String, List<Long>> totalAndPeriodTopDownload)
        {
            this.periodAndTotalTopDownload = totalAndPeriodTopDownload;
        }

        public boolean isShowSelectedObjectDownload()
        {
            return showSelectedObjectDownload;
        }

        public void setShowSelectedObjectDownload(boolean showSelectedObjectDownload)
        {
            this.showSelectedObjectDownload = showSelectedObjectDownload;
        }
    }

}
