/**
 * $Id: DatasetDSpaceObjectGenerator.java 4440 2009-10-10 19:03:27Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/dspace/tags/dspace-1.6.2/dspace-stats/src/main/java/org/dspace/statistics/content/DatasetDSpaceObjectGenerator.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.content;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a dspace object based facet for filtering.
 *
 * @author kevinvandevelde at atmire.com
 * Date: 23-dec-2008
 * Time: 11:38:20
 * 
 */
public class DatasetDSpaceObjectGenerator extends DatasetGenerator {

    /** The children of our dspaceobject to be shown **/
    private List<DSORepresentation> dsoRepresentations;

    public DatasetDSpaceObjectGenerator() {
        dsoRepresentations = new ArrayList<DSORepresentation>();
    }


    public void addDsoChild(DSORepresentation representation){
        dsoRepresentations.add(representation);
    }

    public void addDsoChild(int type, int max, boolean seperate, int nameLength){
        DSORepresentation rep = new DSORepresentation(type, max, seperate);
        rep.setNameLength(nameLength);
        dsoRepresentations.add(rep);
    }

    public List<DSORepresentation> getDsoRepresentations() {
        return dsoRepresentations;
    }
}
