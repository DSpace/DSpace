package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SearchSupportRest;
import org.springframework.stereotype.Component;

/**
 * Created by raf on 26/09/2017.
 */
@Component
public class DiscoverSearchSupportConverter {

    public SearchSupportRest convert(){
        return new SearchSupportRest();
    }
}
