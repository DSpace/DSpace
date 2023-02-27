import { Injectable } from '@angular/core';

import {
  DspaceRestResponseParsingService,
  isCacheableObject
} from './dspace-rest-response-parsing.service';
import { hasValue } from '../../shared/empty.util';
import { getClassForType } from '../cache/builders/build-decorators';
import { GenericConstructor } from '../shared/generic-constructor';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { ParsedResponse } from '../cache/response.models';
import { DSpaceObject } from '../shared/dspace-object.model';
import { environment } from '../../../environments/environment';
import { CacheableObject } from '../cache/cacheable-object.model';
import { RestRequest } from './rest-request.model';

/**
 * ResponseParsingService able to deal with HAL Endpoints that are only needed as steps
 * on the way when discovering the path to a HAL Resource, and aren't properly typed.
 *
 * When all endpoints are properly typed, it can be removed.
 */
@Injectable()
export class EndpointMapResponseParsingService extends DspaceRestResponseParsingService {

  /**
   * Parse an endpoint map response.
   *
   * More lenient than DspaceRestResponseParsingService, in that it also allows objects we don't
   * have a constructor for so their _links section can still be used to discover the path to a HAL
   * resource.
   *
   * @param request   the request that was sent to the backend
   * @param response  the response returned by the backend
   */
  parse(request: RestRequest, response: RawRestResponse): ParsedResponse {
    try {
      response = this.ensureSelfLink(request, response);
      const processRequestDTO = this.process<DSpaceObject>(response.payload, request);

      if (hasValue(processRequestDTO)) {
        const type: string = processRequestDTO.type;
        let objConstructor;
        if (hasValue(type)) {
          objConstructor = getClassForType(type);
        }

        if (isCacheableObject(processRequestDTO) && hasValue(objConstructor)) {
          return new ParsedResponse(response.statusCode, processRequestDTO._links.self);
        } else {
          return new ParsedResponse(response.statusCode, undefined, processRequestDTO);
        }
      } else {
        return new ParsedResponse(response.statusCode);
      }
    } catch (e) {
      console.warn(`Couldn't parse endpoint request at ${request.href}`);
      return new ParsedResponse(response.statusCode, undefined, {
        _links: response.payload._links
      });
    }
  }

  /**
   * Try to deserialize the object the way DspaceRestResponseParsingService does, but if
   * it doesn't work, return the plain object stripped of its type, instead of throwing an error
   *
   * That way it can still be used to determine HAL links.
   *
   * @param obj the object to deserialize
   * @protected
   */
  protected deserialize<ObjectDomain>(obj): any {
    const type: string = obj.type;
    if (hasValue(type)) {
      const objConstructor = getClassForType(type) as GenericConstructor<ObjectDomain>;

      if (hasValue(objConstructor)) {
        const serializer = new this.serializerConstructor(objConstructor);
        return serializer.deserialize(obj);
      }
    }
    return obj;
  }

  /**
   * Add the given object to the object cache
   *
   * This differs from the version in DspaceRestResponseParsingService in that it has to take in
   * to account deserialized objects that aren't properly typed. So it will only add objects to the
   * cache if we can find a constructor for them
   *
   * @param co              the {@link CacheableObject} to add
   * @param request         the {@link RestRequest} that was sent to the backend
   * @param data            the (partial) response from the server
   * @param alternativeURL  an alternative url that can be used to retrieve the object
   */
  addToObjectCache(co: CacheableObject, request: RestRequest, data: any, alternativeURL?: string): void {
    if (!isCacheableObject(co)) {
      const type = hasValue(data) && hasValue(data.type) ? data.type : 'object';
      let dataJSON: string;
      if (hasValue(data._embedded)) {
        dataJSON = JSON.stringify(Object.assign({}, data, {
          _embedded: '...'
        }));
      } else {
        dataJSON = JSON.stringify(data);
      }
      console.warn(`Can't cache incomplete ${type}: ${JSON.stringify(co)}, parsed from (partial) response: ${dataJSON}`);
      return;
    }

    if (hasValue(this.getConstructorFor<any>((co as any).type))) {
      this.objectCache.add(co, hasValue(request.responseMsToLive) ? request.responseMsToLive : environment.cache.msToLive.default, request.uuid, alternativeURL);
    }
  }

}
