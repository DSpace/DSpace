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
import org.dspace.core.Constants;
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
 * Encapsulates all data to render the statistics as a list
 * 
 * @author kevinvandevelde at atmire.com
 * Date: 23-dec-2008
 * Time: 12:38:58
 * 
 */
public class StatisticsListing extends StatisticsDisplay {
    /*
    public StatisticsListing() {
    }

    public StatisticsListing(DSpaceObject dso) {
        super(dso);
    }
    */
    public StatisticsListing(StatisticsData statisticsData){
        super(statisticsData);
    }

    @Override
	public String getType() {
		return "listing";
	}
}
