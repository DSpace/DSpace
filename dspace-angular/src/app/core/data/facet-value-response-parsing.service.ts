import { Injectable } from '@angular/core';
import { FacetValue } from '../../shared/search/models/facet-value.model';
import { ParsedResponse } from '../cache/response.models';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { DSpaceSerializer } from '../dspace-rest/dspace.serializer';
import { FacetValues } from '../../shared/search/models/facet-values.model';
import { DspaceRestResponseParsingService } from './dspace-rest-response-parsing.service';
import { RestRequest } from './rest-request.model';

@Injectable()
export class FacetValueResponseParsingService extends DspaceRestResponseParsingService {
  parse(request: RestRequest, data: RawRestResponse): ParsedResponse {
    const payload = data.payload;
    const facetValues = new DSpaceSerializer(FacetValues).deserialize(payload);
    facetValues.pageInfo = this.processPageInfo(payload);
    facetValues.page = new DSpaceSerializer(FacetValue).deserializeArray(payload._embedded.values);
    this.addToObjectCache(facetValues, request, data);
    return new ParsedResponse(data.statusCode, facetValues._links.self);
  }
}
