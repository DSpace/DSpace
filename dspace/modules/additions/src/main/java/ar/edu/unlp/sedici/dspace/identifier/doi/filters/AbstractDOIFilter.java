package ar.edu.unlp.sedici.dspace.identifier.doi.filters;

import org.dspace.content.DSpaceObject;
import org.springframework.beans.factory.annotation.Required;

public abstract class AbstractDOIFilter {
    /**
     * Contains the metadata element in the condition that determines partially if the filter applies.
     */
    protected MetadataComponentFilter metadata;

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

    public void setMetadata(MetadataComponentFilter metadata) {
        this.metadata = metadata;
    }

    public void setNegateCondition(boolean negateCondition) {
        this.negateCondition = negateCondition;
    }
    
    
}
