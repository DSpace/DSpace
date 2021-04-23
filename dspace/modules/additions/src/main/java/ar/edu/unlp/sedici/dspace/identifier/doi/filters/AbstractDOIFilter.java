package ar.edu.unlp.sedici.dspace.identifier.doi.filters;

import org.dspace.content.DSpaceObject;

public abstract class AbstractDOIFilter {
    /**
     * Determines if the filter condition must be negated or not.
     */
    protected boolean negateCondition;

    /**
     * Used to evaluate if an DSpace object fulfill a condition for DOI generation.
     * 
     * @return true if the filter condition applies.
     */
    public abstract boolean evaluate(DSpaceObject dso);

    public void setNegateCondition(boolean negateCondition) {
        this.negateCondition = negateCondition;
    }
    
    
}
