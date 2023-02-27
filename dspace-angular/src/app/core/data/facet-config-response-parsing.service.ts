import { Injectable } from '@angular/core';
import { SearchFilterConfig } from '../../shared/search/models/search-filter-config.model';
import { ParsedResponse } from '../cache/response.models';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { DSpaceSerializer } from '../dspace-rest/dspace.serializer';
import { DspaceRestResponseParsingService } from './dspace-rest-response-parsing.service';
import { FacetConfigResponse } from '../../shared/search/models/facet-config-response.model';
import { RestRequest } from './rest-request.model';

@Injectable()
export class FacetConfigResponseParsingService extends DspaceRestResponseParsingService {
  parse(request: RestRequest, data: RawRestResponse): ParsedResponse {

    const config = data.payload._embedded.facets;
    const serializer = new DSpaceSerializer(SearchFilterConfig);
    const filters = serializer.deserializeArray(config);

    const _links = {
      self: data.payload._links.self
    };

    // fill in the missing links section
    filters.forEach((filterConfig: SearchFilterConfig) => {
      _links[filterConfig.name] = {
        href: filterConfig._links.self.href
      };
    });

    const facetConfigResponse = Object.assign(new FacetConfigResponse(), {
      filters,
      _links
    });

    this.addToObjectCache(facetConfigResponse, request, data);

    return new ParsedResponse(data.statusCode, facetConfigResponse._links.self);
  }
}
