package ar.edu.unlp.sedici.dspace.identifier.doi.filters;

import java.util.ArrayList;

import org.dspace.content.DSpaceObject;

/**
 * This filter applies an AND operator along multiple conditions declared within its configuration.
 *
 */
public class MultipleAndFilter extends AbstractDOIFilter {

    protected ArrayList<AbstractDOIFilter> filtersToEval;
    
    public void setFiltersToEval(ArrayList<AbstractDOIFilter> filtersToEval) {
        this.filtersToEval = filtersToEval;
    }

    /**
     * Returns true if all conditions are true.
     */
    @Override
    public boolean evaluate(DSpaceObject dso) {
        for (AbstractDOIFilter filter: filtersToEval) {
            if(!negateCondition) {
                if(!filter.evaluate(dso)) {
                    return false;
                }
            } else {
                if(!filter.evaluate(dso)) {
                    return true;
                }
            }
        }
        //If reaching this point, then all conditions are true.
        if(!negateCondition) {
            return true;
        } else {
            return false;
        }
    }

}
