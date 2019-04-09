/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.solr.handler.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.response.ResultContext;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.SolrIndexSearcher;
import org.dspace.solr.util.CrisMetricsUpdateListener;
import org.dspace.solr.util.ExtraInfo;

public class CrisMetricsExtractComponent extends SearchComponent
{

    @Override
    public void prepare(ResponseBuilder rb) throws IOException
    {
        /* NOOP */
    }

    @Override
    public void process(ResponseBuilder rb) throws IOException
    {
        Set<String> returnFields = getReturnFields(rb);
        SolrIndexSearcher searcher = rb.req.getSearcher();
		if (returnFields.contains("clearcache-crismetrics") 
				|| CrisMetricsUpdateListener.isCacheInvalid(searcher))
        {
            CrisMetricsUpdateListener.renewCache(searcher);
        }
        List<String> metricsField = CrisMetricsExtractComponent
                .containsWord(returnFields, "crismetrics_");
        if (!metricsField.isEmpty())
        {
            // only trigger this code if user explicitly lists rank
            // in the field list. This changes the DocSlice in the
            // result returned by the standard component and replaces
            // it with a SolrDocumentList (whose attributes are more
            // amenable to modification).
            ResultContext slice = (ResultContext) rb.rsp.getValues()
                    .get("response");
            IndexReader reader = searcher.getIndexReader();
            if (slice.docs == null)
                return;
            SolrDocumentList rl = new SolrDocumentList();
            rl.setNumFound(slice.docs.matches());
            rl.setStart(slice.docs.offset());
            String coreName = searcher.getCore().getName();
            for (DocIterator it = slice.docs.iterator(); it.hasNext();)
            {
                int docId = it.nextDoc();
                Document doc = reader.document(docId);
                SolrDocument sdoc = new SolrDocument();
                List<IndexableField> fields = doc.getFields();
                for (IndexableField field : fields)
                {
                    String fn = field.name();
                        try
                        {
                            org.apache.lucene.document.Field number = (org.apache.lucene.document.Field)doc.getField(fn);
                            sdoc.addField(fn, number);
                        }
                        catch (Exception ex)
                        {
                            sdoc.addField(fn, doc.get(fn));
                        }
                }
                if (returnFields.contains("score"))
                {
                    sdoc.addField("score", it.score());
                }
                for (String metric : metricsField)
                {
					Double result = CrisMetricsUpdateListener.getMetric(coreName, metric,
                            docId);
                    ExtraInfo extraInfo = CrisMetricsUpdateListener.getRemark(coreName, metric,
                            docId);
                    if (result != null)
                    {
                        sdoc.addField(metric, result);
                    }
                    if (extraInfo != null && extraInfo.remark != null)
                    {
                        sdoc.addField(metric+"_remark", extraInfo.remark);
                    }
                    if (extraInfo != null && extraInfo.acquisitionTime != null)
                    {
                        sdoc.addField(metric+"_time", extraInfo.acquisitionTime);
                    }
                    if (extraInfo != null && extraInfo.startTime != null)
                    {
                        sdoc.addField(metric+"_starttime", extraInfo.startTime);
                    }
                    if (extraInfo != null && extraInfo.endTime != null)
                    {
                        sdoc.addField(metric+"_endtime", extraInfo.endTime);
                    }
                }
                rl.add(sdoc);
            }
            rb.rsp.getValues().remove("response");
            rb.rsp.add("response", rl);
        }
    }

    private Set<String> getReturnFields(ResponseBuilder rb)
    {
        Set<String> fields = new HashSet<String>();
        String flp = rb.req.getParams().get(CommonParams.FL);
        if (StringUtils.isEmpty(flp))
        {
            // called on startup with a null ResponseBuilder, so
            // we want to prevent a spurious NPE in the logs...
            return fields;
        }
        String[] fls = StringUtils.split(flp, ",");
        IndexSchema schema = rb.req.getSchema();
        String coreName = rb.req.getSearcher().getCore().getName();
        for (String fl : fls)
        {
            if ("*".equals(fl))
            {
                Map<String, SchemaField> fm = schema.getFields();
                for (String fieldname : fm.keySet())
                {
                    SchemaField sf = fm.get(fieldname);
                    if (sf.stored() && (!"content".equals(fieldname)))
                    {
                        fields.add(fieldname);
                    }
                    Map<String, Map<Integer, Double>> tmp = CrisMetricsUpdateListener
                            .getMetrics(coreName);
                    if (tmp != null && !tmp.isEmpty())
                    {
                        for (String key : tmp.keySet())
                        {
                            fields.add(key);
                        }
                    }
                }
            }
            else if ("search.uniqueid".equals(fl))
            {
                SchemaField usf = schema.getUniqueKeyField();
                fields.add(usf.getName());
            }
            else
            {
                fields.add(fl);
            }
        }
        return fields;
    }

    ///////////////////// SolrInfoMBean methods ///////////////////

    @Override
    public String getDescription()
    {
        return "Cris Metrics Extraction Component";
    }

    @Override
    public String getSource()
    {
        return "$Source$";
    }

    @Override
    public String getVersion()
    {
        return "$Revision$";
    }

    private static List<String> containsWord(Set<String> words, String sentence)
    {
        List<String> result = new ArrayList<String>();
        for (String word : words)
        {
            if (word.contains(sentence))
            {
                result.add(word);
            }
        }

        return result;
    }
}
