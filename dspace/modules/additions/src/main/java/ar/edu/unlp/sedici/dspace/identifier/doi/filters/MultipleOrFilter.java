package ar.edu.unlp.sedici.dspace.identifier.doi.filters;

import java.util.ArrayList;

import org.dspace.content.DSpaceObject;

/**
 * This filter applies an OR operator along multiple conditions declared within its configuration.
 *
 */
public class MultipleOrFilter extends AbstractDOIFilter {

    protected ArrayList<AbstractDOIFilter> filtersToEval;

    public void setFiltersToEval(ArrayList<AbstractDOIFilter> filtersToEval) {
        this.filtersToEval = filtersToEval;
    }

    /**
     * Returns true if at least one condition is true.
     */
    @Override
    public boolean evaluate(DSpaceObject dso) {
        for( AbstractDOIFilter filter: filtersToEval) {
            if(!negateCondition) {
                //At first true condition, returns true.
                if (filter.evaluate(dso)) {
                    return true;
                }
            } else {
                if (filter.evaluate(dso)) {
                    return false;
                }
            }
        }
        //If reaching this point, then all conditions are false.
        if(!negateCondition) {
            return false;
        } else {
            return true;
        }
    }

}
