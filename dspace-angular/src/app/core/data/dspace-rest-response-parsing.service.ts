/* eslint-disable max-classes-per-file */
import { hasNoValue, hasValue, isNotEmpty } from '../../shared/empty.util';
import { DSpaceSerializer } from '../dspace-rest/dspace.serializer';
import { Serializer } from '../serializer';
import { PageInfo } from '../shared/page-info.model';
import { ObjectCacheService } from '../cache/object-cache.service';
import { GenericConstructor } from '../shared/generic-constructor';
import { PaginatedList, buildPaginatedList } from './paginated-list.model';
import { getClassForType } from '../cache/builders/build-decorators';
import { environment } from '../../../environments/environment';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { DSpaceObject } from '../shared/dspace-object.model';
import { Injectable } from '@angular/core';
import { ResponseParsingService } from './parsing.service';
import { ParsedResponse } from '../cache/response.models';
import { RestRequestMethod } from './rest-request-method';
import { getUrlWithoutEmbedParams, getEmbedSizeParams } from '../index/index.selectors';
import { URLCombiner } from '../url-combiner/url-combiner';
import { CacheableObject } from '../cache/cacheable-object.model';
import { RestRequest } from './rest-request.model';


/**
 * Return true if obj has a value for `_links.self`
 *
 * @param {any} obj The object to test
 */
export function isCacheableObject(obj: any): boolean {
  return hasValue(obj) && hasValue(obj._links) && hasValue(obj._links.self) && hasValue(obj._links.self.href);
}

/**
 * Return true if halObj has a value for `page` with properties
 * `size`, `totalElements`, `totalPages`, `number`
 *
 * @param {any} halObj The object to test
 */
export function isRestPaginatedList(halObj: any): boolean {
  return hasValue(halObj.page) &&
    hasValue(halObj.page.size) &&
    hasValue(halObj.page.totalElements) &&
    hasValue(halObj.page.totalPages) &&
    hasValue(halObj.page.number);
}

/**
 * Split a url into parts
 *
 * @param url the url to split
 */
const splitUrlInParts = (url: string): string[] => {
  return url.split('?')
    .map((part) => part.split('&'))
    .reduce((combined, current) => [...combined, ...current]);
};

@Injectable({ providedIn: 'root' })
export class DspaceRestResponseParsingService implements ResponseParsingService {
  protected serializerConstructor: GenericConstructor<Serializer<any>> = DSpaceSerializer;

  constructor(
    protected objectCache: ObjectCacheService,
  ) {
  }

  parse(request: RestRequest, response: RawRestResponse): ParsedResponse {
    response = this.ensureSelfLink(request, response);

    let alternativeURL: string;
    if (request.method === RestRequestMethod.GET) {
      // only store an alternative URL when parsing a GET request, as there are cases when e.g. a
      // POST or a PUT would have a different response
      alternativeURL = getUrlWithoutEmbedParams(request.href);
    }

    const processRequestDTO = this.process<DSpaceObject>(response.payload, request, alternativeURL);

    if (hasValue(processRequestDTO)) {
      if (isCacheableObject(processRequestDTO)) {
        return new ParsedResponse(response.statusCode, processRequestDTO._links.self);
      } else {
        return new ParsedResponse(response.statusCode, undefined, processRequestDTO);
      }
    } else {
      return new ParsedResponse(response.statusCode);
    }
  }

  public process<ObjectDomain>(data: any, request: RestRequest, alternativeURL?: string): any {
    const embedSizeParams = getEmbedSizeParams(request.href);

    if (isNotEmpty(data)) {
      if (hasNoValue(data) || (typeof data !== 'object')) {
        return data;
      } else if (isRestPaginatedList(data)) {
        return this.processPaginatedList(data, request, alternativeURL);
      } else if (Array.isArray(data)) {
        return this.processArray(data, request);
      } else if (isCacheableObject(data)) {
        const object = this.deserialize(data);
        if (isNotEmpty(data._embedded)) {
          Object
            .keys(data._embedded)
            .filter((property) => data._embedded.hasOwnProperty(property))
            .forEach((property) => {
              let embedAltUrl = data._links[property].href;
              const match = embedSizeParams
                .find((param: { name: string, size: number }) => param.name === property);
              if (hasValue(match)) {
                embedAltUrl = new URLCombiner(embedAltUrl, `?size=${match.size}`).toString();
              }
              this.process<ObjectDomain>(data._embedded[property], request, embedAltUrl);
            });
        }

        this.addToObjectCache(object, request, data, alternativeURL);
        return object;
      }
      const result = {};
      Object.keys(data)
        .filter((property) => data.hasOwnProperty(property))
        .filter((property) => hasValue(data[property]))
        .forEach((property) => {
          result[property] = this.process(data[property], request);
        });
      return result;

    }
  }

  /**
   * Some rest endpoints don't return a self link in their response. This method will fix that for
   * the root resource in the response by filling in the requested href, without any embed params.
   * It will print a warning in the console, as this could indicate an issue on the REST side.
   *
   * @param request   the {@RestRequest} that was sent to the server
   * @param response  the {@link RawRestResponse} returned by the server
   * @protected
   */
  protected ensureSelfLink(request: RestRequest, response: RawRestResponse): RawRestResponse {
    const urlWithoutEmbedParams = getUrlWithoutEmbedParams(request.href);
    if (request.method === RestRequestMethod.GET && hasValue(response) && hasValue(response.payload) && hasValue(response.payload._links)) {
      if (hasNoValue(response.payload._links.self) || hasNoValue(response.payload._links.self.href)) {
        console.warn(`The response for '${request.href}' doesn't have a self link. This could mean there's an issue with the REST endpoint`);
        response.payload._links = Object.assign({}, response.payload._links, {
          self: {
            href: urlWithoutEmbedParams
          }
        });

      } else {
        const expected = splitUrlInParts(urlWithoutEmbedParams);
        const actual = splitUrlInParts(response.payload._links.self.href);
        if (expected[0] === actual[0] && (expected.some((e) => !actual.includes(e)) || actual.some((e) => !expected.includes(e)))) {
          console.warn(`The response for '${urlWithoutEmbedParams}' has the self link '${response.payload._links.self.href}'. These don't match. This could mean there's an issue with the REST endpoint`);
          response.payload._links = Object.assign({}, response.payload._links, {
            self: {
              href: urlWithoutEmbedParams
            }
          });
        }
      }
    }
    return response;
  }

  protected processPaginatedList<ObjectDomain>(data: any, request: RestRequest, alternativeURL?: string): PaginatedList<ObjectDomain> {
    const pageInfo: PageInfo = this.processPageInfo(data);
    let list = data._embedded;

    // Workaround for inconsistency in rest response. Issue: https://github.com/DSpace/dspace-angular/issues/238
    if (hasNoValue(list)) {
      list = [];
    } else if (!Array.isArray(list)) {
      list = this.flattenSingleKeyObject(list);
    }

    const page: ObjectDomain[] = this.processArray(list, request);
    const paginatedList = buildPaginatedList<ObjectDomain>(pageInfo, page, true, data._links);
    this.addToObjectCache(paginatedList, request, data, alternativeURL);
    return paginatedList;
  }

  protected processArray<ObjectDomain>(data: any, request: RestRequest): ObjectDomain[] {
    let array: ObjectDomain[] = [];
    data.forEach((datum) => {
        array = [...array, this.process(datum, request)];
      }
    );
    return array;
  }

  protected deserialize<ObjectDomain>(obj): any {
    const type = obj.type;
    const objConstructor = this.getConstructorFor<ObjectDomain>(type);
    if (hasValue(objConstructor)) {
      const serializer = new this.serializerConstructor(objConstructor);
      return serializer.deserialize(obj);
    } else {
      console.warn('cannot deserialize type ' + type);
      return null;
    }
  }

  /**
   * Returns the constructor for the given type, or null if there isn't a registered model for that
   * type
   *
   * @param type the object to find the constructor for.
   * @protected
   */
  protected getConstructorFor<ObjectDomain>(type: string): GenericConstructor<ObjectDomain> {
    if (hasValue(type)) {
      return getClassForType(type) as GenericConstructor<ObjectDomain>;
    } else {
      return null;
    }
  }

  /**
   * Add the given object to the object cache
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

    if (alternativeURL === co._links.self.href) {
      alternativeURL = undefined;
    }

    this.objectCache.add(co, hasValue(request.responseMsToLive) ? request.responseMsToLive : environment.cache.msToLive.default, request.uuid, alternativeURL);
  }

  processPageInfo(payload: any): PageInfo {
    if (hasValue(payload.page)) {
      const pageInfoObject = new DSpaceSerializer(PageInfo).deserialize(payload.page);
      if (pageInfoObject.currentPage >= 0) {
        Object.assign(pageInfoObject, { currentPage: pageInfoObject.currentPage + 1 });
      }
      return pageInfoObject;
    } else {
      return undefined;
    }
  }

  protected flattenSingleKeyObject(obj: any): any {
    const keys = Object.keys(obj);
    if (keys.length !== 1) {
      throw new Error(`Expected an object with a single key, got: ${JSON.stringify(obj)}`);
    }
    return obj[keys[0]];
  }

  protected isSuccessStatus(statusCode: number) {
    return statusCode >= 200 && statusCode < 300;
  }
}
