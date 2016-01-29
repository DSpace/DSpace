package org.dspace.solr.schema;

import java.io.IOException;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.DoubleFieldSource;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.SortField;
import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TrieField;
import org.apache.solr.search.QParser;

public class CrisMetricsFieldType extends TrieField
{

    @Override
    public ValueSource getValueSource(SchemaField field, QParser qparser)
    {
        return new DoubleFieldSource( field.getName(), FieldCache.NUMERIC_UTILS_DOUBLE_PARSER );
    }
    
    @Override
    public SortField getSortField(SchemaField field, boolean top) {
      return new SortField(field.getName(), 
        new CrisMetricsFieldComparatorSource(), top);
    }

    @Override
    // copied verbatim from GeoHashField method
    public void write(TextResponseWriter writer, String name, IndexableField f)
        throws IOException {
      writer.writeStr(name, f.stringValue(), false);
    }
}
