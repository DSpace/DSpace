/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.content;

import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.apache.solr.client.solrj.SolrServerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.util.List;
import java.util.ArrayList;
import java.io.StringWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

/**
 * Encapsulates all data to render the statistics
 *
 * @author kevinvandevelde at atmire.com
 * Date: 23-dec-2008
 * Time: 9:27:09
 * 
 */
public abstract class StatisticsDisplay {
    private String id;
    private StatisticsData statisticsData;
    private String title;

    /** css information used to position the display object in a html page**/
    private List<String> css;



    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    protected StatisticsDisplay(StatisticsData statisticsData){
        this.statisticsData = statisticsData;
    }



    public List<DatasetGenerator> getDatasetGenerators() {
        return statisticsData.getDatasetGenerators();
    }

    public void addDatasetGenerator(DatasetGenerator set){
        statisticsData.addDatasetGenerator(set);
    }

    public void addFilter(StatisticsFilter filter){
        statisticsData.addFilters(filter);
    }

    public List<StatisticsFilter> getFilters(){
        return statisticsData.getFilters();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDataset(Dataset dataset) {
        statisticsData.setDataset(dataset);
    }

    public abstract String getType();
    
    public Dataset getDataset() {
        return statisticsData.getDataset();
    }



    public Dataset getDataset(Context context) throws SQLException, SolrServerException, IOException, ParseException {
        return statisticsData.createDataset(context);
    }



    /**
     * Method used to create the xml for the dataset of values
     * @param doc the doc needed to create elements
     * @param root the root xml node on which to add our xml
     */
    public void datasetToXml(Document doc, Node root){
        Dataset dataset = getDataset();
        if(dataset != null){
            Node datasetNode = doc.createElement("dataset");
            root.appendChild(datasetNode);
            ////////////////////
            // Add the labels //
            ////////////////////
            //TODO: zien dat de links ook bewaard blijven ?
            //First add the row labels
            List<String> rowLabels = dataset.getRowLabels();
            if(0 < rowLabels.size()){
                Element rowsNode = doc.createElement("rows");
                datasetNode.appendChild(rowsNode);
                for (String rowLabel : rowLabels) {
                    //Create a new row element
                    Element rowNode = doc.createElement("row");
                    rowsNode.appendChild(rowNode);
                    rowNode.setAttribute("label", rowLabel);
                }
            }
            //Second add the column labels
            List<String> colLabels = dataset.getColLabels();
            if(0 < colLabels.size()){
                Element colsNode = doc.createElement("columns");
                datasetNode.appendChild(colsNode);
                for (String colLabel : colLabels) {
                    //Create a new col element
                    Element colNode = doc.createElement("column");
                    colsNode.appendChild(colNode);
                    colNode.setAttribute("label", colLabel);
                }
            }
            ////////////////////
            // Add the values //
            ////////////////////
            Node dataNode = doc.createElement("data");
            datasetNode.appendChild(dataNode);

            float[][] matrix = dataset.getMatrix();
            for (float[] row : matrix) {
                Element rowNode = doc.createElement("row");
                dataNode.appendChild(rowNode);
                for (float value : row) {
                    Element cellNode = doc.createElement("cell");
                    rowNode.appendChild(cellNode);
                    cellNode.appendChild(doc.createTextNode(String.valueOf(value)));
                }
            }
        }
    }

    public void addCss(String style){
        if (style != null) {
            if (css == null)
                css = new ArrayList<String>();
            css.add(style.trim());
        }
    }

    public String getCss() {
        if (css != null) {
            String result = "";
            for (String s : css) {
                if (!s.endsWith(";"))
                    s += ";";
                result += s;
            }
            return result;
        }
        else
            return "";
    }
}


