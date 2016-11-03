/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import java.util.Date;
import java.util.Map;

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
		
		private String startDate;
		private String endDate;
		
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
	}

	   
    public static class MetricsMappingDetails extends MappingDetails {
        
        private String operator;
        
        private String rangeByYear;
                
        public String getOperator()
        {
            return operator;
        }

        public void setOperator(String operator)
        {
            this.operator = operator;
        }

        public String getRangeByYear()
        {
            return rangeByYear;
        }

        public void setRangeByYear(String rangeByYear)
        {
            this.rangeByYear = rangeByYear;
        }

    }
}
