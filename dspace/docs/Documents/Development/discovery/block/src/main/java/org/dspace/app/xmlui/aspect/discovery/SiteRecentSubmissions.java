/*
 * CommunityViewer.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 10:02:24 -0700 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.aspect.discovery;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;
import org.dspace.discovery.ServiceFactory;

/**
 * Display a single community. This includes a full text search, browse by list,
 * community display and a list of recent submissions.
 *     private static final Logger log = Logger.getLogger(DSpaceFeedGenerator.class);

 * @author Scott Phillips
 */
public class SiteRecentSubmissions extends AbstractRecentSubmissions
{

    private static final Message T_head_recent_submissions =
            message("xmlui.ArtifactBrowser.SiteViewer.head_recent_submissions");


    /**
     * Display a single community (and refrence any sub communites or
     * collections)
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        QueryResponse response = getRecentlySubmittedItems(null);


        if (response != null) {
            // Build the community viewer division.
            Division home = body.addDivision("site-home", "primary repository");


            Division lastSubmittedDiv = home
                    .addDivision("site-recent-submission", "secondary recent-submission");

            lastSubmittedDiv.setHead(T_head_recent_submissions);

            ReferenceSet lastSubmitted = lastSubmittedDiv.addReferenceSet(
                    "site-last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                    null, "recent-submissions");

            for (SolrDocument doc : response.getResults()) {
                lastSubmitted.addReference(
                        ServiceFactory.getSearchService().findDSpaceObject(context, doc));
            }


            for (FacetField field : response.getFacetFields()) {
                if (field.getValueCount() > 0) {
                    List facet = home.addList(
                            "site-" + field.getName(),
                            "simple",
                            "secondary " + field.getName());


                    // xmlui.Discovery.collecton.subjects.head
                    facet.setHead("xmlui.Discovery.site." + field.getName() + ".head");

                    java.util.List<FacetField.Count> values = field.getValues();

                    if (values != null) {
                        for (FacetField.Count value : values) {
                            facet.addItem().addXref(
                                    contextPath + 
                                    "/search?fq=" +
                                     value.getAsFilterQuery(),
                                     value.getName() + " (" + value.getCount() + ")"
                                );
                        }
                    }

                }
            }

        }
    }



}