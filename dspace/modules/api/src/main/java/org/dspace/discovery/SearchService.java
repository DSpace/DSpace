/**
 * $Id: SearchService.java 4845 2010-04-05 01:05:48Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/modules/dspace-discovery/trunk/provider/src/main/java/org/dspace/discovery/SearchService.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.discovery;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.Date;

/**
 * User: mdiggory
 * Date: Oct 19, 2009
 * Time: 5:35:08 AM
 *
 * An extra search method has been added so that a user can search in non archived items (workflowitems)
 */
public interface SearchService {

    QueryResponse search(Context context, SolrQuery query) throws SearchServiceException;

    List<DSpaceObject> search(Context context, String query, int offset, int max, String... filterquery);

    List<DSpaceObject> search(Context context, String query, int offset, int max, boolean archived, String... filterquery);

    List<DSpaceObject> search(Context context, String query, String orderfield, boolean ascending, boolean archived, int offset, int max, String... filterquery);
}
