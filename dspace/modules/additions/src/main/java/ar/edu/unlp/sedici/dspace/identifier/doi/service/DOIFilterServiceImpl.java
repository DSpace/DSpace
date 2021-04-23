package ar.edu.unlp.sedici.dspace.identifier.doi.service;

import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.identifier.doi.service.DOIFilterService;
import org.springframework.beans.factory.annotation.Required;

import ar.edu.unlp.sedici.dspace.identifier.doi.filters.AbstractDOIFilter;

public class DOIFilterServiceImpl implements DOIFilterService {

    protected AbstractDOIFilter mainFilter;
    
    @Required
    public void setMainFilter(AbstractDOIFilter mainFilter) {
        this.mainFilter = mainFilter;
    }
    @Override
    public boolean isEligibleDSO(DSpaceObject dso) {
        return mainFilter.evaluate(dso);
    }
    
}
