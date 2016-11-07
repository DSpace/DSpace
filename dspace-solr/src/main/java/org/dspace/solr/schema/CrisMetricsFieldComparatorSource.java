/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.solr.schema;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.dspace.solr.util.CrisMetricsUpdateListener;

public class CrisMetricsFieldComparatorSource extends FieldComparatorSource
{
	private String coreName;
    public CrisMetricsFieldComparatorSource(String coreName) {
		super();
    	this.coreName = coreName;
	}
	
	
    @Override
    public FieldComparator newComparator(String fieldname, int numHits,
            int sortPos, boolean reversed) throws IOException
    {
        return new CrisMetricFieldComparator(coreName, fieldname, numHits);
    }

    // adapted from DocFieldComparator
    public static final class CrisMetricFieldComparator
            extends FieldComparator<Double>
    {
        private final int[] docIDs;

        private int docBase;

        private int bottom;

        private double topValue;

        private String fieldName;
        
        private String coreName;
        
        CrisMetricFieldComparator(String coreName, String fieldName, int numHits)
        {
        	this.coreName = coreName;
            this.fieldName = fieldName;
            docIDs = new int[numHits];
        }

        @Override
        public int compare(int slot1, int slot2)
        {
            Double metric = getMetric(coreName, fieldName, docIDs[slot1]);
            Double metric2 = getMetric(coreName, fieldName, docIDs[slot2]);
            if(metric!=null && metric2==null) {
                return 1; 
            } else {
                if(metric==null && metric2!=null) {
                    return -1;
                }
                else {
                    if(metric==null && metric2==null) {
                        return 0;
                    }
                }
            }
            
            Double result = metric - metric2;
            return result.intValue();
        }

        @Override
        public int compareBottom(int doc)
        {
            Double metric = getMetric(coreName, fieldName, bottom);
            Double metric2 = getMetric(coreName, fieldName, docBase + doc);
            if(metric!=null && metric2==null) {
                return 1; 
            } else {
                if(metric==null && metric2!=null) {
                    return -1;
                }
                else {
                    if(metric==null && metric2==null) {
                        return 0;
                    }
                }
            }
            Double result = metric - metric2;
            return result.intValue();
        }

        @Override
        public void copy(int slot, int doc)
        {
            docIDs[slot] = docBase + doc;
        }

        @Override
        public void setBottom(final int bottom)
        {
            this.bottom = docIDs[bottom];
        }

        @Override
        public Double value(int slot)
        {
            return getMetric(coreName, fieldName, docIDs[slot]);
        }

        private Double getMetric(String coreName, String metric, int docId)
        {
            return CrisMetricsUpdateListener.getMetric(coreName, metric, docId);
        }

        @Override
        public int compareTop(int doc) throws IOException
        {
            int docValue = docBase + doc;
            return Double.compare(topValue, docValue);
        }

        @Override
        public FieldComparator setNextReader(AtomicReaderContext context)
                throws IOException
        {
            // TODO: can we "map" our docIDs to the current
            // reader? saves having to then subtract on every
            // compare call
            this.docBase = context.docBase;
            return this;
        }

        @Override
        public void setTopValue(Double value)
        {
            topValue = value;

        }
    }

}