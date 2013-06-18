/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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

    public void addDsoChild(int type, int max, boolean separate, int nameLength){
        DSORepresentation rep = new DSORepresentation(type, max, separate);
        rep.setNameLength(nameLength);
        dsoRepresentations.add(rep);
    }

    public List<DSORepresentation> getDsoRepresentations() {
        return dsoRepresentations;
    }
}
