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
        //At first true condition, returns true.
        for( AbstractDOIFilter filter: filtersToEval) {
            if (filter.evaluate(dso)) {
                return true;
            }
        }
        return false;
    }

}
