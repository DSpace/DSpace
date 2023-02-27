import { Injectable } from '@angular/core';
import { ParsedResponse } from '../cache/response.models';
import { DSpaceSerializer } from '../dspace-rest/dspace.serializer';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { hasValue } from '../../shared/empty.util';
import { SearchObjects } from '../../shared/search/models/search-objects.model';
import { MetadataMap, MetadataValue } from '../shared/metadata.models';
import { DspaceRestResponseParsingService } from './dspace-rest-response-parsing.service';
import { RestRequest } from './rest-request.model';

@Injectable()
export class MyDSpaceResponseParsingService extends DspaceRestResponseParsingService {
  parse(request: RestRequest, data: RawRestResponse): ParsedResponse {
    // fallback for unexpected empty response
    const emptyPayload = {
      _embedded: {
        objects: []
      }
    };
    const payload = data.payload._embedded.searchResult || emptyPayload;
    const hitHighlights: MetadataMap[] = payload._embedded.objects
      .map((object) => object.hitHighlights)
      .map((hhObject) => {
        const mdMap: MetadataMap = {};
        if (hhObject) {
          for (const key of Object.keys(hhObject)) {
            const value: MetadataValue = Object.assign(new MetadataValue(), {
              value: hhObject[key].join('...'),
              language: null
            });
            mdMap[key] = [value];
          }
        }
        return mdMap;
      });

    const dsoSelfLinks = payload._embedded.objects
      .filter((object) => hasValue(object._embedded))
      .map((object) => object._embedded.indexableObject)
      .map((dso) => this.process(dso, request))
      .map((obj) => obj._links.self.href)
      .reduce((combined, thisElement) => [...combined, ...thisElement], []);

    const objects = payload._embedded.objects
      .filter((object) => hasValue(object._embedded))
      .map((object, index) => Object.assign({}, object, {
        indexableObject: dsoSelfLinks[index],
        hitHighlights: hitHighlights[index],
        _embedded: this.filterEmbeddedObjects(object)
      }));
    payload.objects = objects;
    const deserialized: any = new DSpaceSerializer(SearchObjects).deserialize(payload);
    deserialized.pageInfo = this.processPageInfo(payload);
    this.addToObjectCache(deserialized, request, data);
    return new ParsedResponse(data.statusCode, deserialized._links.self);
  }

  protected filterEmbeddedObjects(object) {
    const allowedEmbeddedKeys = ['submitter', 'item', 'workspaceitem', 'workflowitem'];
    if (object._embedded.indexableObject && object._embedded.indexableObject._embedded) {
      return Object.assign({}, object._embedded, {
        indexableObject: Object.assign({}, object._embedded.indexableObject, {
          _embedded: Object.keys(object._embedded.indexableObject._embedded)
            .filter((key) => allowedEmbeddedKeys.includes(key))
            .reduce((obj, key) => {
              obj[key] = object._embedded.indexableObject._embedded[key];
              return obj;
            }, {})
        })
      });
    } else {
      return object;
    }

  }
}
