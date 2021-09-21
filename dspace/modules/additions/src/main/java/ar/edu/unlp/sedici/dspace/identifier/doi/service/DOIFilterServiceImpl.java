package ar.edu.unlp.sedici.dspace.identifier.doi.service;


import org.dspace.content.DSpaceObject;
import org.dspace.content.Metadatum;
import org.dspace.identifier.DOI;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.dspace.identifier.doi.service.DOIFilterService;
import org.springframework.beans.factory.annotation.Required;

import ar.edu.unlp.sedici.dspace.identifier.doi.filters.AbstractDOIFilter;

public class DOIFilterServiceImpl implements DOIFilterService {

    protected AbstractDOIFilter mainFilter;
    /**
     * Metadata holding any value of DOI external to this repository.
     */
    protected String externalDOImetadata;
    
    @Required
    public void setMainFilter(AbstractDOIFilter mainFilter) {
        this.mainFilter = mainFilter;
    }
    
    public void setExternalDOImetadata(String metadataName) {
        this.externalDOImetadata = metadataName;
    }
    
    @Override
    public boolean isEligibleDSO(DSpaceObject dso) {
        return mainFilter.evaluate(dso);
    }
    
    @Override
    public boolean hasExternalDOI(DSpaceObject dso) {
        Metadatum[] metadataList = dso.getMetadataByMetadataString(this.externalDOImetadata);
        if (metadataList.length > 0) {
            for (Metadatum md : metadataList) {
                if (md.value != null && !md.value.isEmpty() && md.value.contains("doi")) {
                    try {
                        String doi = DOI.formatIdentifier(md.value);
                        if (!doi.isEmpty()) {
                            //If metadata can be parsed, then it is an external DOI
                            return true;
                        }
                    } catch (DOIIdentifierException e) {
                        //Do Nothing, proceed with next instance if available.
                    }
                }
            }
        }
        //if DSO has not any instance of externalDOIMetadata, then it has not external DOI.
        return false;
    }
    
    
    
}
