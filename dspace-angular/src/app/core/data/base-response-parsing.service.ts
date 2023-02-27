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
import { CacheableObject } from '../cache/cacheable-object.model';
import { RestRequest } from './rest-request.model';


/**
 * Return true if halObj has a value for `_links.self`
 *
 * @param {any} halObj The object to test
 */
export function isRestDataObject(halObj: any): boolean {
  return isNotEmpty(halObj._links) && hasValue(halObj._links.self);
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

export abstract class BaseResponseParsingService {
  protected abstract objectCache: ObjectCacheService;
  protected abstract toCache: boolean;
  protected shouldDirectlyAttachEmbeds = false;
  protected serializerConstructor: GenericConstructor<Serializer<any>> = DSpaceSerializer;

  protected process<ObjectDomain>(data: any, request: RestRequest, alternativeURL?: string): any {
    if (isNotEmpty(data)) {
      if (hasNoValue(data) || (typeof data !== 'object')) {
        return data;
      } else if (isRestPaginatedList(data)) {
        return this.processPaginatedList(data, request);
      } else if (Array.isArray(data)) {
        return this.processArray(data, request);
      } else if (isRestDataObject(data)) {
        const object = this.deserialize(data);
        if (isNotEmpty(data._embedded)) {
          Object
            .keys(data._embedded)
            .filter((property) => data._embedded.hasOwnProperty(property))
            .forEach((property) => {
              const parsedObj = this.process<ObjectDomain>(data._embedded[property], request, data._links[property].href);
              if (hasValue(object) && this.shouldDirectlyAttachEmbeds && isNotEmpty(parsedObj)) {
                if (isRestPaginatedList(data._embedded[property])) {
                  object[property] = parsedObj;
                  object[property].page = parsedObj.page.map((obj) => this.retrieveObjectOrUrl(obj));
                } else if (isRestDataObject(data._embedded[property])) {
                  object[property] = this.retrieveObjectOrUrl(parsedObj);
                } else if (Array.isArray(parsedObj)) {
                    object[property] = parsedObj.map((obj) => this.retrieveObjectOrUrl(obj));
                }
              }
            });
        }

        this.cache(object, request, data, alternativeURL);
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

  protected processPaginatedList<ObjectDomain>(data: any, request: RestRequest): PaginatedList<ObjectDomain> {
    const pageInfo: PageInfo = this.processPageInfo(data);
    let list = data._embedded;

    // Workaround for inconsistency in rest response. Issue: https://github.com/DSpace/dspace-angular/issues/238
    if (hasNoValue(list)) {
      list = [];
    } else if (!Array.isArray(list)) {
      list = this.flattenSingleKeyObject(list);
    }
    const page: ObjectDomain[] = this.processArray(list, request);
    return buildPaginatedList<ObjectDomain>(pageInfo, page,);
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
    const type: string = obj.type;
    if (hasValue(type)) {
      const objConstructor = getClassForType(type) as GenericConstructor<ObjectDomain>;

      if (hasValue(objConstructor)) {
        const serializer = new this.serializerConstructor(objConstructor);
        return serializer.deserialize(obj);
      } else {
        console.warn('cannot deserialize type ' + type);
        return null;
      }

    } else {
      console.warn('cannot deserialize type ' + type);
      return null;
    }
  }

  protected cache<ObjectDomain>(obj, request: RestRequest, data: any, alternativeURL: string) {
    if (this.toCache) {
      this.addToObjectCache(obj, request, data, alternativeURL);
    }
  }

  protected addToObjectCache(co: CacheableObject, request: RestRequest, data: any, alternativeURL: string): void {
    if (hasNoValue(co) || hasNoValue(co._links) || hasNoValue(co._links.self) || hasNoValue(co._links.self.href)) {
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

    this.objectCache.add(co, hasValue(request.responseMsToLive) ? request.responseMsToLive : environment.cache.msToLive.default, request.uuid, alternativeURL);
  }

  processPageInfo(payload: any): PageInfo {
    if (hasValue(payload.page)) {
      const pageObj = Object.assign({}, payload.page, { _links: payload._links });
      const pageInfoObject = new DSpaceSerializer(PageInfo).deserialize(pageObj);
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

  protected retrieveObjectOrUrl(obj: any): any {
    return this.toCache ? obj._links.self.href : obj;
  }

  protected isSuccessStatus(statusCode: number) {
    return statusCode >= 200 && statusCode < 300;
  }
}
