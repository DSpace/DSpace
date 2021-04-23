package ar.edu.unlp.sedici.dspace.identifier.doi.filters;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Metadatum;
import org.springframework.beans.factory.annotation.Required;

/**
 * Filter that applies if the metadata component tied with this filter is present in 
 * the object being filtered, and also if the regex expressions in the metadata component applies.
 */
public class HasSingleMetadataFilter extends AbstractDOIFilter {

    /**
     * Contains the metadata element in the condition that determines partially if the filter applies.
     */
    protected MetadataComponentFilter metadata;

    @Required
    public void setMetadata(MetadataComponentFilter metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean evaluate(DSpaceObject dso) {
        Metadatum[] metadataList = dso.getMetadataByMetadataString(metadata.getMetadataName());
        if (metadataList.length > 0) {
            for (Metadatum md : metadataList) {
                if (!negateCondition) {
                    if (metadata.matchRegex(md.value)) {
                        return true;
                    }
                } else {
                    if (!metadata.matchRegex(md.value)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
