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

import org.dspace.content.DSpaceObject;
import org.dspace.statistics.Dataset;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.io.StringWriter;
import java.util.Map;
import java.util.List;

/**
 * Encapsulates all data to render the statistics as a table
 *
 * @author kevinvandevelde at atmire.com
 * Date: 23-dec-2008
 * Time: 9:27:52
 * 
 */
public class StatisticsTable extends StatisticsDisplay{
    /*
    public StatisticsTable() {
        
    }
    */
    public StatisticsTable(StatisticsData statisticsData){
        super(statisticsData);
    }
    /*
    public StatisticsTable(DSpaceObject currentDso, Dataset dataset) {
        super(currentDso, dataset);
    }

    public StatisticsTable(Dataset dataset) {
        super(dataset);
    }

    public StatisticsTable(DSpaceObject dso){
        super(dso);
    }
    */
    
    @Override
	public String getType() {
		return "table";
	}
}
