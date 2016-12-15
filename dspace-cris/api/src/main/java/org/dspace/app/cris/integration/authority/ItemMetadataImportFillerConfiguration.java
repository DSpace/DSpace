/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;

public class ItemMetadataImportFillerConfiguration {
	private Map<String, MappingDetails> mapping;
	
	private Boolean updateEnabled;
	
	public Map<String, MappingDetails> getMapping() {
		return mapping;
	}

	public void setMapping(Map<String, MappingDetails> mapping) {
		this.mapping = mapping;
	}

	public void setUpdateEnabled(Boolean updateEnabled) {
		this.updateEnabled = updateEnabled;
	}
	
	public Boolean getUpdateEnabled() {
		return updateEnabled;
	}
	
	public static class MappingDetails{
		private String shortName;
		//private String converter;
		private boolean useAll;
		
		private Integer visibility;
		
		private boolean formatAsDate;
		private boolean formatAsInteger;
		
		private String startDate;
		private String endDate;

		private boolean appendMode = false;

	    public boolean isAppendMode()
	    {
	        return appendMode;
	    }

	    public void setAppendMode(boolean appendMode)
	    {
	        this.appendMode = appendMode;
	    }

		public void setFormatAsDate(boolean isDate) {
			this.formatAsDate = isDate;
		}

		public String getShortName() {
			return shortName;
		}
		public void setShortName(String shortName) {
			this.shortName = shortName;
		}
		public boolean isUseAll() {
			return useAll;
		}
		public void setUseAll(boolean useAll) {
			this.useAll = useAll;
		}
		public Integer getVisibility() {
			return visibility;
		}
		public void setVisibility(Integer visibility){
			this.visibility = visibility;
		}
		public boolean formatAsDate() {
			return formatAsDate;
		}

        public String getStartDate()
        {
            return startDate;
        }

        public void setStartDate(String startDate)
        {
            this.startDate = startDate;
        }

        public String getEndDate()
        {
            return endDate;
        }

        public void setEndDate(String endDate)
        {
            this.endDate = endDate;
        }

        public boolean isFormatAsInteger()
        {
            return formatAsInteger;
        }

        public void setFormatAsInteger(boolean formatAsInteger)
        {
            this.formatAsInteger = formatAsInteger;
        }
	}

	   
    public static class MetricsMappingDetails extends MappingDetails {
        
        private String rangeByYear;
                
        public String getRangeByYear()
        {
            return rangeByYear;
        }

        public void setRangeByYear(String rangeByYear)
        {
            this.rangeByYear = rangeByYear;
        }

        public void computeMetricCount(int idx, Metadatum[] mm, Item item, CrisMetrics metric)
        {
            Metadatum metricValue = null;
            if (mm.length > 0)
            {
                try
                {
                    metricValue = mm[idx];
                }
                catch (Exception ex)
                {
                    metricValue = mm[0];
                }
            }

            metric.setMetricType(metricValue.qualifier);
            
            try
            {
                setupMetricCount(metricValue, mm, item, metric);
            }
            catch (NumberFormatException e)
            {
                metric.setMetricCount(0);
            }            
        }

        public void setupMetricCount(Metadatum metricValue, Metadatum[] mm, Item item, CrisMetrics metric)
        {
            metric.setMetricCount(
                    Double.parseDouble(metricValue.value));
        }

    }
    
    public static class MetricsMappingCountDetails extends MetricsMappingDetails {
        
        public void setupMetricCount(Metadatum metricValue, Metadatum[] mm, Item item, CrisMetrics metric)
        {
            metric.setMetricCount(mm.length);
        }
    }
    
    public static class MetricsMappingSumDetails extends MetricsMappingDetails {
        
        private List<String> mdFields;
        
        public void setupMetricCount(Metadatum metricValue, Metadatum[] mm, Item item, CrisMetrics metric)
        {
            double metriccount = 0;
            for(String mdField : mdFields) {
                String value = item.getMetadata(mdField);
                if(StringUtils.isNotBlank(value)) {
                    double element = Double.parseDouble(value);
                    metriccount += element;
                }
            }
            metric.setMetricCount(metriccount);
        }

        public List<String> getMdFields()
        {
            return mdFields;
        }

        public void setMdFields(List<String> mdFields)
        {
            this.mdFields = mdFields;
        }
    }
}
