/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.*;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;
import org.dspace.statistics.util.LocationUtils;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandleManager;
import org.dspace.app.util.Util;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.sql.SQLException;
import java.text.ParseException;
import java.io.UnsupportedEncodingException;

/**
 * Query factory associated with a DSpaceObject.
 * Encapsulates the raw data, independent of rendering.
 * <p>
 * To use:
 * <ol>
 *  <li>Instantiate, passing a reference to the interesting DSO.</li>
 *  <li>Add a {@link DatasetDSpaceObjectGenerator} for the appropriate object type.</li>
 *  <li>Add other generators as required to get the statistic you want.</li>
 *  <li>Add {@link org.dspace.statistics.content.filter filters} as required.</li>
 *  <li>{@link #createDataset(Context)} will run the query and return a result matrix.
 *      Subsequent calls skip the query and return the same matrix.</li>
 * </ol>
 *
 * @author kevinvandevelde at atmire.com
 * Date: 23-feb-2009
 * Time: 12:25:20
 */
public class StatisticsDataVisits extends StatisticsData
{
    /** Current DSpaceObject for which to generate the statistics. */
    private DSpaceObject currentDso;

    /** Construct a completely uninitialized query. */
    public StatisticsDataVisits()
    {
    }

    /** Construct an empty query concerning a given DSpaceObject. */
    public StatisticsDataVisits(DSpaceObject dso)
    {
        super();
        this.currentDso = dso;
    }

    /** Construct an unconfigured query around a given DSO and Dataset. */
    public StatisticsDataVisits(DSpaceObject currentDso, Dataset dataset)
    {
        super(dataset);
        this.currentDso = currentDso;
    }

    /** Construct an unconfigured query around a given Dataset. */
    public StatisticsDataVisits(Dataset dataset)
    {
        super(dataset);
    }

    public Dataset createDataset(Context context) throws SQLException,
            SolrServerException, ParseException
    {
        // Check if we already have one.
        // If we do then give it back.
        if(getDataset() != null)
        {
            return getDataset();
        }

        ///////////////////////////
        // 1. DETERMINE OUR AXIS //
        ///////////////////////////
        ArrayList<DatasetQuery> datasetQueries = new ArrayList<DatasetQuery>();
        for (int i = 0; i < getDatasetGenerators().size(); i++) {
            DatasetGenerator dataSet = getDatasetGenerators().get(i);
            processAxis(dataSet, datasetQueries);
        }

        // Now lets determine our values.
        // First check if we have a date facet & if so find it.
        DatasetTimeGenerator dateFacet = null;
        if (getDatasetGenerators().get(0) instanceof DatasetTimeGenerator
                || (1 < getDatasetGenerators().size() && getDatasetGenerators()
                        .get(1) instanceof DatasetTimeGenerator))
        {
            if(getDatasetGenerators().get(0) instanceof DatasetTimeGenerator)
            {
                dateFacet = (DatasetTimeGenerator) getDatasetGenerators().get(0);
            }
            else
            {
                dateFacet = (DatasetTimeGenerator) getDatasetGenerators().get(1);
            }
        }

        /////////////////////////
        // 2. DETERMINE VALUES //
        /////////////////////////
        boolean showTotal = false;
        // Check if we need our total
        if ((getDatasetGenerators().get(0) != null && getDatasetGenerators()
                .get(0).isIncludeTotal())
                || (1 < getDatasetGenerators().size()
                        && getDatasetGenerators().get(1) != null && getDatasetGenerators()
                        .get(1).isIncludeTotal()))
        {
            showTotal = true;
        }

        if (dateFacet != null && dateFacet.getActualStartDate() != null
                && dateFacet.getActualEndDate() != null)
        {
            StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
            dateFilter.setStartDate(dateFacet.getActualStartDate());
            dateFilter.setEndDate(dateFacet.getActualEndDate());
            dateFilter.setTypeStr(dateFacet.getDateType());
            addFilters(dateFilter);
        }
        else if (dateFacet != null && dateFacet.getStartDate() != null
                && dateFacet.getEndDate() != null)
        {
            StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
            dateFilter.setStartStr(dateFacet.getStartDate());
            dateFilter.setEndStr(dateFacet.getEndDate());
            dateFilter.setTypeStr(dateFacet.getDateType());
            addFilters(dateFilter);
        }

        // Determine our filterQuery
        String filterQuery = "";
        for (int i = 0; i < getFilters().size(); i++) {
            StatisticsFilter filter = getFilters().get(i);

            filterQuery += "(" + filter.toQuery() + ")";
            if(i != (getFilters().size() -1))
            {
                filterQuery += " AND ";
            }
        }
        if(StringUtils.isNotBlank(filterQuery)){
            filterQuery += " AND ";
        }
        //Only use the view type and make sure old data (where no view type is present) is also supported
        //Solr doesn't explicitly apply boolean logic, so this query cannot be simplified to an OR query
        filterQuery += "-(statistics_type:[* TO *] AND -statistics_type:" + SolrLogger.StatisticsType.VIEW.text() + ")";


//        System.out.println("FILTERQUERY: " + filterQuery);

        // We determine our values on the queries resolved above
        Dataset dataset = null;

        // Run over our queries.
        // First how many queries do we have ?
        if(dateFacet != null){
            // So do all the queries and THEN do the date facet
            for (int i = 0; i < datasetQueries.size(); i++) {
                DatasetQuery dataSetQuery = datasetQueries.get(i);
                if(dataSetQuery.getQueries().size() != 1){
                    // TODO: do this
                }else{
                    String query = dataSetQuery.getQueries().get(0).getQuery();
                    if(dataSetQuery.getMax() == -1){
                        // We are asking from our current query all the visits faceted by date
                        ObjectCount[] results = SolrLogger.queryFacetDate(query, filterQuery, dataSetQuery.getMax(), dateFacet.getDateType(), dateFacet.getStartDate(), dateFacet.getEndDate(), showTotal, context);
                        dataset = new Dataset(1, results.length);
                        // Now that we have our results put em in a matrix
                        for(int j = 0; j < results.length; j++){
                            dataset.setColLabel(j, results[j].getValue());
                            dataset.addValueToMatrix(0, j, results[j].getCount());
                        }
                        // TODO: change this !
                        // Now add the column label
                        dataset.setRowLabel(0, getResultName(dataSetQuery.getName(), dataSetQuery, context));
                        dataset.setRowLabelAttr(0, getAttributes(dataSetQuery.getName(), dataSetQuery, context));
                    }else{
                        // We need to get the max objects and the next part of the query on them (next part beeing the datasettimequery
                        ObjectCount[] maxObjectCounts = SolrLogger.queryFacetField(query, filterQuery, dataSetQuery.getFacetField(), dataSetQuery.getMax(), false, null);
                        for (int j = 0; j < maxObjectCounts.length; j++) {
                            ObjectCount firstCount = maxObjectCounts[j];
                            String newQuery = dataSetQuery.getFacetField() + ": " + ClientUtils.escapeQueryChars(firstCount.getValue()) + " AND " + query;
                            ObjectCount[] maxDateFacetCounts = SolrLogger.queryFacetDate(newQuery, filterQuery, dataSetQuery.getMax(), dateFacet.getDateType(), dateFacet.getStartDate(), dateFacet.getEndDate(), showTotal, context);


                            // Make sure we have a dataSet
                            if(dataset == null)
                            {
                                dataset = new Dataset(maxObjectCounts.length, maxDateFacetCounts.length);
                            }

                            // TODO: this is a very dirty fix change this ! ! ! ! ! !
                            dataset.setRowLabel(j, getResultName(firstCount.getValue(), dataSetQuery, context));
                            dataset.setRowLabelAttr(j, getAttributes(firstCount.getValue(), dataSetQuery, context));

                            for (int k = 0; k < maxDateFacetCounts.length; k++) {
                                ObjectCount objectCount = maxDateFacetCounts[k];
                                // No need to add this many times
                                if(j == 0)
                                {
                                    dataset.setColLabel(k, objectCount.getValue());
                                }
                                dataset.addValueToMatrix(j, k, objectCount.getCount());
                            }
                        }
                        if(dataset != null && !(getDatasetGenerators().get(0) instanceof DatasetTimeGenerator)){
                            dataset.flipRowCols();
                        }
                    }
                }
            }
        }else{
            // We do NOT have a date facet so just do queries after each other
            /*
            for (int i = 0; i < datasetQueries.size(); i++) {
                DatasetQuery datasetQuery = datasetQueries.get(i);
                if(datasetQuery.getQueries().size() != 1){
                    // TODO: do this
                }else{
                    String query = datasetQuery.getQueries().get(0);
                    // Loop over the queries & do em
//                    ObjectCount[] topCounts = SolrLogger.queryFacetField(query, );
                }
            }
            */
            DatasetQuery firsDataset = datasetQueries.get(0);
            //Do the first query

            ObjectCount[] topCounts1 = null;
//            if(firsDataset.getQueries().size() == 1){
            topCounts1 = queryFacetField(firsDataset, firsDataset.getQueries().get(0).getQuery(), filterQuery);
//            }else{
//                TODO: do this
//            }
            // Check if we have more queries that need to be done
            if(datasetQueries.size() == 2){
                DatasetQuery secondDataSet = datasetQueries.get(1);
                // Now do the second one
                ObjectCount[] topCounts2 = queryFacetField(secondDataSet, secondDataSet.getQueries().get(0).getQuery(), filterQuery);
                // Now that have results for both of them lets do x.y queries
                List<String> facetQueries = new ArrayList<String>();
                for (ObjectCount count2 : topCounts2) {
                    String facetQuery = secondDataSet.getFacetField() + ":" + ClientUtils.escapeQueryChars(count2.getValue());
                    // Check if we also have a type present (if so this should be put into the query)
                    if ("id".equals(secondDataSet.getFacetField()) && secondDataSet.getQueries().get(0).getDsoType() != -1)
                    {
                        facetQuery += " AND type:" + secondDataSet.getQueries().get(0).getDsoType();
                    }

                    facetQueries.add(facetQuery);
                }
                for (int i = 0; i < topCounts1.length; i++){
                    ObjectCount count1 = topCounts1[i];
                    ObjectCount[] currentResult = new ObjectCount[topCounts2.length];

                    // Make sure we have a dataSet
                    if(dataset == null)
                    {
                        dataset = new Dataset(topCounts2.length, topCounts1.length);
                    }
                    dataset.setColLabel(i, getResultName(count1.getValue(), firsDataset, context));
                    dataset.setColLabelAttr(i, getAttributes(count1.getValue(), firsDataset, context));

                    String query = firsDataset.getFacetField() + ":" + ClientUtils.escapeQueryChars(count1.getValue());
                    // Check if we also have a type present (if so this should be put into the query)
                    if("id".equals(firsDataset.getFacetField()) && firsDataset.getQueries().get(0).getDsoType() != -1)
                    {
                        query += " AND type:" + firsDataset.getQueries().get(0).getDsoType();
                    }

                    Map<String, Integer> facetResult = SolrLogger.queryFacetQuery(query, filterQuery, facetQueries);
                    
                    
                    // TODO: the show total
                    // No need to add this many times
                    // TODO: dit vervangen door te displayen value
                    for (int j = 0; j < topCounts2.length; j++) {
                        ObjectCount count2 = topCounts2[j];
                        if(i == 0) {
                            dataset.setRowLabel(j, getResultName(count2.getValue(), secondDataSet, context));
                            dataset.setRowLabelAttr(j, getAttributes(count2.getValue(), secondDataSet, context));

                        }
                        // Get our value the value is the same as the query
                        String facetQuery = secondDataSet.getFacetField() + ":" + ClientUtils.escapeQueryChars(count2.getValue());
                        // Check if we also have a type present (if so this should be put into the query
                        if ("id".equals(secondDataSet.getFacetField()) && secondDataSet.getQueries().get(0).getDsoType() != -1)
                        {
                            facetQuery += " AND type:" + secondDataSet.getQueries().get(0).getDsoType();
                        }

                        // We got our query so now get the value
                        dataset.addValueToMatrix(j, i, facetResult.get(facetQuery));
                    }

                    /*
                    for (int j = 0; j < topCounts2.length; j++) {
                        ObjectCount count2 = topCounts2[j];
                        String query = firsDataset.getFacetField() + ":" + count1.getValue();
                        // Check if we also have a type present (if so this should be put into the query
                        if("id".equals(firsDataset.getFacetField()) && firsDataset.getQueries().get(0).getDsoType() != -1)
                            query += " AND type:" + firsDataset.getQueries().get(0).getDsoType();

                        query += " AND " + secondDataSet.getFacetField() + ":" + count2.getValue();
                        // Check if we also have a type present (if so this should be put into the query
                        if("id".equals(secondDataSet.getFacetField()) && secondDataSet.getQueries().get(0).getDsoType() != -1)
                            query += " AND type:" + secondDataSet.getQueries().get(0).getDsoType();

                        long count = SolrLogger.queryFacetQuery(query, filterQuery);

                        // TODO: the show total
                        // No need to add this many times
                        // TODO: dit vervangen door te displayen value
                        if(i == 0) {
                            dataset.setRowLabel(j, getResultName(count2.getValue(), secondDataSet, context));
                            dataset.setRowLabelAttr(j, getAttributes(count2.getValue(), secondDataSet, context));

                        }

                        dataset.addValueToMatrix(j, i, count);
                    }
                    */
                }

//                System.out.println("BOTH");

            } else{
                // Make sure we have a dataSet
                dataset = new Dataset(1, topCounts1.length);
                for (int i = 0; i < topCounts1.length; i++) {
                    ObjectCount count = topCounts1[i];
                    dataset.setColLabel(i, getResultName(count.getValue(), firsDataset, context));
                    dataset.setColLabelAttr(i, getAttributes(count.getValue(), firsDataset, context));
                    dataset.addValueToMatrix(0, i, count.getCount());
                }
            }

        }
        if(dataset != null){
            dataset.setRowTitle("Dataset 1");
            dataset.setColTitle("Dataset 2");
        }else
        {
            dataset = new Dataset(0, 0);
        }
        return dataset;
    }

    private void processAxis(DatasetGenerator datasetGenerator, List<DatasetQuery> queries) throws SQLException {
        if(datasetGenerator instanceof DatasetDSpaceObjectGenerator){
            DatasetDSpaceObjectGenerator dspaceObjAxis = (DatasetDSpaceObjectGenerator) datasetGenerator;
            // Get the types involved
            List<DSORepresentation> dsoRepresentations = dspaceObjAxis.getDsoRepresentations();
            for (int i = 0; i < dsoRepresentations.size(); i++){
                DatasetQuery datasetQuery = new DatasetQuery();
                Integer dsoType = dsoRepresentations.get(i).getType();
                boolean separate = dsoRepresentations.get(i).getSeparate();
                Integer dsoLength = dsoRepresentations.get(i).getNameLength();
                // Check if our type is our current object
                if(currentDso != null && dsoType == currentDso.getType()){
                    Query query = new Query();
                    query.setDso(currentDso.getID(), currentDso.getType(), dsoLength);
                    datasetQuery.addQuery(query);
                }else{
                    // TODO: only do this for bitstreams from an item
                    Query query = new Query();
                    if(currentDso != null && separate && dsoType == Constants.BITSTREAM){
                        // CURRENTLY THIS IS ONLY POSSIBLE FOR AN ITEM ! ! ! ! ! ! !
                        // We need to get the separate bitstreams from our item and make a query for each of them
                        Item item = (Item) currentDso;
                        for (int j = 0; j < item.getBundles().length; j++) {
                            Bundle bundle = item.getBundles()[j];
                            for (int k = 0; k < bundle.getBitstreams().length; k++) {
                                Bitstream bitstream = bundle.getBitstreams()[k];
                                if(!bitstream.getFormat().isInternal()){
                                    // Add a separate query for each bitstream
                                    query.setDso(bitstream.getID(), bitstream.getType(), dsoLength);
                                }
                            }
                        }
                    } else {
                        // We have something else than our current object.
                        // So we need some kind of children from it, so put this in our query
                        query.setOwningDso(currentDso);
                        query.setDsoLength(dsoLength);

                        String title = "";
                        switch(dsoType){
                            case Constants.BITSTREAM:
                                title = "Files";
                                break;
                            case Constants.ITEM:
                                title = "Items";
                                break;
                            case Constants.COLLECTION:
                                title = "Collections";
                                break;
                            case Constants.COMMUNITY:
                                title = "Communities";
                                break;
                        }
                        datasetQuery.setName(title);
                        // Put the type in so we only get the children of the type specified
                        query.setDsoType(dsoType);
                    }
                    datasetQuery.addQuery(query);
                }
                datasetQuery.setFacetField("id");
                datasetQuery.setMax(dsoRepresentations.get(i).getMax());

                queries.add(datasetQuery);
            }
        }else
        if(datasetGenerator instanceof DatasetTypeGenerator){
            DatasetTypeGenerator typeAxis = (DatasetTypeGenerator) datasetGenerator;
            DatasetQuery datasetQuery = new DatasetQuery();

            // First make sure our query is in order
            Query query = new Query();
            if(currentDso != null)
            {
                query.setDso(currentDso.getID(), currentDso.getType());
            }
            datasetQuery.addQuery(query);

            // Then add the rest
            datasetQuery.setMax(typeAxis.getMax());
            datasetQuery.setFacetField(typeAxis.getType());
            datasetQuery.setName(typeAxis.getType());

            queries.add(datasetQuery);
        }
    }

    /**
     * Gets the name of the DSO (example for collection: ((Collection) dso).getname();
     * @return the name of the given DSO
     */
    private String getResultName(String value, DatasetQuery datasetQuery,
            Context context) throws SQLException
    {
        if("continent".equals(datasetQuery.getName())){
            value = LocationUtils.getContinentName(value, context
                    .getCurrentLocale());
        }else
        if("countryCode".equals(datasetQuery.getName())){
            value = LocationUtils.getCountryName(value, context
                    .getCurrentLocale());
        }else{
            Query query = datasetQuery.getQueries().get(0);
            //TODO: CHANGE & THROW AWAY THIS ENTIRE METHOD
            //Check if int
            int dsoId;
            int dsoLength = query.getDsoLength();
            try {
                dsoId = Integer.parseInt(value);
            }catch(Exception e){
                dsoId = -1;
            }
            if(dsoId == -1 && query.getDsoId() != -1 && value == null)
            {
                dsoId = query.getDsoId();
            }

            if(dsoId != -1 && query.dsoType != -1){
                DSpaceObject dso = DSpaceObject.find(context, query.getDsoType(), dsoId);
                if(dso != null){
                    switch(dso.getType()){
                        case Constants.BITSTREAM:
                            Bitstream bit = (Bitstream) dso;
                            return bit.getName();
                        case Constants.ITEM:
                            Item item = (Item) dso;
                            String name = "untitled";
                            Metadatum[] vals = item.getMetadata("dc", "title", null, Item.ANY);
                            if(vals != null && 0 < vals.length)
                            {
                                name = vals[0].value;
                            }
                            if(dsoLength != -1 && name.length() > dsoLength){
                                //Cut it off at the first space
                                int firstSpace = name.indexOf(' ', dsoLength);
                                if(firstSpace != -1){
                                    name = name.substring(0, firstSpace) + " ...";
                                }
                            }

                            return name;

                        case Constants.COLLECTION:
                            Collection coll = (Collection) dso;
                            name = coll.getName();

                            if(dsoLength != -1 && name.length() > dsoLength){
                                //Cut it off at the first space
                                int firstSpace = name.indexOf(' ', dsoLength);
                                if(firstSpace != -1){
                                    name = name.substring(0, firstSpace) + " ...";
                                }
                            }
                            return name;

                        case Constants.COMMUNITY:
                            Community comm = (Community) dso;
                            name = comm.getName();

                            if(dsoLength != -1 && name.length() > dsoLength){
                                //Cut it off at the first space
                                int firstSpace = name.indexOf(' ', dsoLength);
                                if(firstSpace != -1){
                                    name = name.substring(0, firstSpace) + " ...";
                                }
                            }
                            return name;
                    }
                }
            }
        }
        return value;
    }

    private Map<String, String> getAttributes(String value,
            DatasetQuery datasetQuery, Context context) throws SQLException
    {
        HashMap<String, String> attrs = new HashMap<String, String>();
        Query query = datasetQuery.getQueries().get(0);
        //TODO: CHANGE & THROW AWAY THIS ENTIRE METHOD
        //Check if int
        int dsoId;
        try {
            dsoId = Integer.parseInt(value);
        }catch(Exception e){
            dsoId = -1;
        }
        if(dsoId == -1 && query.getDsoId() != -1 && value == null)
        {
            dsoId = query.getDsoId();
        }

        if(dsoId != -1 && query.dsoType != -1){
            DSpaceObject dso = DSpaceObject.find(context, query.getDsoType(), dsoId);
            if(dso != null){
                switch(dso.getType()){
                    case Constants.BITSTREAM:
                        Bitstream bit = (Bitstream) dso;

                        //Get our owning item
                        Item owningItem = null;
                        Bundle[] bunds = bit.getBundles();
                        if(0 < bunds.length && 0 < bunds[0].getItems().length)
                        {
                            owningItem = bunds[0].getItems()[0];
                        }

                        // If possible reference this bitstream via a handle, however this may
                        // be null if a handle has not yet been assigned. In this case reference the
                        // item its internal id. In the last case where the bitstream is not associated
                        // with an item (such as a community logo) then reference the bitstreamID directly.
                        String identifier = null;
                        if (owningItem != null && owningItem.getHandle() != null)
                        {
                            identifier = "handle/" + owningItem.getHandle();
                        }
                        else if (owningItem != null)
                        {
                            identifier = "item/" + owningItem.getID();
                        }
                        else
                        {
                            identifier = "id/" + bit.getID();
                        }


                        String url = ConfigurationManager.getProperty("dspace.url") + "/bitstream/"+identifier+"/";

                        // If we can put the pretty name of the bitstream on the end of the URL
                        try
                        {
                            if (bit.getName() != null)
                            {
                                url += Util.encodeBitstreamName(bit.getName(), "UTF-8");
                            }
                        }
                        catch (UnsupportedEncodingException uee)
                        {
                            // Just ignore it:  we don't have to have a pretty
                            // name on the end of the URL because the sequence id will
                            // locate it. However it means that links in this file might
                            // not work....
                        }

                        url += "?sequence="+bit.getSequenceID();

                        attrs.put("url", url);
                        break;

                    case Constants.ITEM:
                        Item item = (Item) dso;

                        attrs.put("url", HandleManager.resolveToURL(context, item.getHandle()));
                        break;

                    case Constants.COLLECTION:
                        Collection coll = (Collection) dso;

                        attrs.put("url", HandleManager.resolveToURL(context, coll.getHandle()));
                        break;

                    case Constants.COMMUNITY:
                        Community comm = (Community) dso;

                        attrs.put("url", HandleManager.resolveToURL(context, comm.getHandle()));
                        break;
                }
            }
        }
        return attrs;
    }


    private ObjectCount[] queryFacetField(DatasetQuery dataset, String query,
            String filterQuery) throws SolrServerException
    {
        String facetType = dataset.getFacetField() == null ? "id" : dataset
                .getFacetField();
        return SolrLogger.queryFacetField(query, filterQuery, facetType,
                dataset.getMax(), false, null);
    }

    public static class DatasetQuery {
        private String name;
        private int max;
        private String facetField;
        private List<Query> queries;

        public DatasetQuery() {
            queries = new ArrayList<Query>();
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }

        public void addQuery(Query q){
            queries.add(q);
        }

        public List<Query> getQueries() {
            return queries;
        }

        public String getFacetField() {
            return facetField;
        }

        public void setFacetField(String facetField) {
            this.facetField = facetField;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public class Query {
            private int dsoType;
            private int dsoId;
            private int dsoLength;
            private DSpaceObject owningDso;


            public Query() {
                dsoId = -1;
                dsoType = -1;
                dsoLength = -1;
                owningDso = null;
            }

            public void setOwningDso(DSpaceObject owningDso) {
                this.owningDso = owningDso;
            }

            public void setDso(int dsoId, int dsoType){
                this.dsoId = dsoId;
                this.dsoType = dsoType;
            }

            public void setDso(int dsoId, int dsoType, int length){
                this.dsoId = dsoId;
                this.dsoType = dsoType;
                this.dsoLength = length;
            }

            public void setDsoType(int dsoType) {
                this.dsoType = dsoType;
            }


            public int getDsoLength() {
                return dsoLength;
            }


            public void setDsoLength(int dsoLength) {
                this.dsoLength = dsoLength;
            }

            public int getDsoId() {
                return dsoId;
            }

            public int getDsoType(){
                return dsoType;
            }

            public String getQueryResultName(){
                //TODO: This has got to be done differently in case we have a string query.
                //This is just a temporary solution so we can get on with our work.
                return dsoType + ":" + dsoId;
            }

            public String getQuery() {
                //Time to construct our query
                String query = "";
                //Check (& add if needed) the dsoType
                if(dsoType != -1)
                {
                    query += "type: " + dsoType;
                }

                //Check (& add if needed) the dsoId
                if(dsoId != -1)
                {
                    query += (query.equals("") ? "" : " AND ") + " id:" + dsoId;
                }


                if(owningDso != null && currentDso != null){
                    query += (query.equals("") ? "" : " AND " );

                    String owningStr = "";
                    switch(currentDso.getType()){
                        case Constants.ITEM:
                            owningStr = "owningItem";
                            break;
                        case Constants.COLLECTION:
                            owningStr = "owningColl";
                            break;
                        case Constants.COMMUNITY:
                            owningStr = "owningComm";
                            break;
                    }
                    owningStr += ":" + currentDso.getID();
                    query += owningStr;
                }

                if(query.equals(""))
                {
                    query = "*:*";
                }

                return query;
            }
        }

}
