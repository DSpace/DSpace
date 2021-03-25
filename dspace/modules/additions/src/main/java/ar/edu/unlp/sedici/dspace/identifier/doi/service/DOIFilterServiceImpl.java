package ar.edu.unlp.sedici.dspace.identifier.doi.service;

import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.identifier.doi.service.DOIFilterService;
import org.springframework.beans.factory.annotation.Required;

public class DOIFilterServiceImpl implements DOIFilterService {

    /**
     * List containing values of sub-tipologies (sedici.subtype) used to determine for what items 
     * the DOI provider must generate doi's. Configure this at "identifier-service" configuration file.
     */
    protected List<String> typeFilter;
    
    @Required
    public void setTypeFilter(List<String> typeFilterList) {
        this.typeFilter = typeFilterList;
    }
    
    @Override
    public boolean isEligibleDSO(DSpaceObject dso) {
        if(this.typeFilter != null && this.typeFilter.size() > 0) {
            Metadatum[] metadataList = dso.getMetadata("sedici", "subtype", null, Item.ANY);
            for (String subtipology : typeFilter) {
                for (Metadatum mdt : metadataList) {
                    if (mdt.value.equalsIgnoreCase(subtipology)) {
                        return true;
                    }
                }
            }
            //If no filter is triggered, then this item must not be approved for DOI generation.
            return false;
        }
        return true;
    }
    
}
