import { Injectable } from '@angular/core';
import { ParsedResponse } from '../cache/response.models';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { DSpaceSerializer } from '../dspace-rest/dspace.serializer';
import { ContentSource } from '../shared/content-source.model';
import { MetadataConfig } from '../shared/metadata-config.model';
import { DspaceRestResponseParsingService } from './dspace-rest-response-parsing.service';
import { RestRequest } from './rest-request.model';

@Injectable()
/**
 * A ResponseParsingService used to parse RawRestResponse coming from the REST API to a ContentSource object
 */
export class ContentSourceResponseParsingService extends DspaceRestResponseParsingService {

  parse(request: RestRequest, data: RawRestResponse): ParsedResponse {
    const payload = data.payload;

    const deserialized = new DSpaceSerializer(ContentSource).deserialize(payload);

    let metadataConfigs = [];
    if (payload._embedded && payload._embedded.harvestermetadata && payload._embedded.harvestermetadata.configs) {
      metadataConfigs = new DSpaceSerializer(MetadataConfig).serializeArray(payload._embedded.harvestermetadata.configs);
    }
    deserialized.metadataConfigs = metadataConfigs;

    this.addToObjectCache(deserialized, request, data);

    return new ParsedResponse(data.statusCode, deserialized._links.self);
  }

}
