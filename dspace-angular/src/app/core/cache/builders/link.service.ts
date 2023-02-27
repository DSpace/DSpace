import { Inject, Injectable, Injector } from '@angular/core';
import { hasNoValue, hasValue, isNotEmpty } from '../../../shared/empty.util';
import { FollowLinkConfig } from '../../../shared/utils/follow-link-config.model';
import { GenericConstructor } from '../../shared/generic-constructor';
import { HALResource } from '../../shared/hal-resource.model';
import { DATA_SERVICE_FACTORY } from '../../data/base/data-service.decorator';
import {
  LINK_DEFINITION_FACTORY,
  LINK_DEFINITION_MAP_FACTORY,
  LinkDefinition,
} from './build-decorators';
import { RemoteData } from '../../data/remote-data';
import { EMPTY, Observable } from 'rxjs';
import { ResourceType } from '../../shared/resource-type';
import { HALDataService } from '../../data/base/hal-data-service.interface';
import { PaginatedList } from '../../data/paginated-list.model';

/**
 * A Service to handle the resolving and removing
 * of resolved {@link HALLink}s on HALResources
 */
@Injectable({
  providedIn: 'root',
})
export class LinkService {

  constructor(
    protected parentInjector: Injector,
    @Inject(DATA_SERVICE_FACTORY) private getDataServiceFor: (resourceType: ResourceType) => GenericConstructor<HALDataService<any>>,
    @Inject(LINK_DEFINITION_FACTORY) private getLinkDefinition: <T extends HALResource>(source: GenericConstructor<T>, linkName: keyof T['_links']) => LinkDefinition<T>,
    @Inject(LINK_DEFINITION_MAP_FACTORY) private getLinkDefinitions: <T extends HALResource>(source: GenericConstructor<T>) => Map<keyof T['_links'], LinkDefinition<T>>,
  ) {
  }

  /**
   * Resolve the given {@link FollowLinkConfig}s for the given model
   *
   * @param model the {@link HALResource} to resolve the links for
   * @param linksToFollow the {@link FollowLinkConfig}s to resolve
   */
  public resolveLinks<T extends HALResource>(model: T, ...linksToFollow: FollowLinkConfig<T>[]): T {
    linksToFollow.forEach((linkToFollow: FollowLinkConfig<T>) => {
      this.resolveLink(model, linkToFollow);
    });
    return model;
  }

  /**
   * Resolve the given {@link FollowLinkConfig} for the given model and return the result. This does
   * not attach the link result to the property on the model. Useful when you're working with a
   * readonly object
   *
   * @param model the {@link HALResource} to resolve the link for
   * @param linkToFollow the {@link FollowLinkConfig} to resolve
   */
  public resolveLinkWithoutAttaching<T extends HALResource, U extends HALResource>(model, linkToFollow: FollowLinkConfig<T>): Observable<RemoteData<U | PaginatedList<U>>> {
    const matchingLinkDef = this.getLinkDefinition(model.constructor, linkToFollow.name);

    if (hasValue(matchingLinkDef)) {
      const provider = this.getDataServiceFor(matchingLinkDef.resourceType);

      if (hasNoValue(provider)) {
        throw new Error(`The @link() for ${linkToFollow.name} on ${model.constructor.name} models uses the resource type ${matchingLinkDef.resourceType.value.toUpperCase()}, but there is no service with an @dataService(${matchingLinkDef.resourceType.value.toUpperCase()}) annotation in order to retrieve it`);
      }

      const service: HALDataService<any> = Injector.create({
        providers: [],
        parent: this.parentInjector,
      }).get(provider);

      const link = model._links[matchingLinkDef.linkName];
      if (hasValue(link)) {
        const href = link.href;

        try {
          if (matchingLinkDef.isList) {
            return service.findListByHref(href, linkToFollow.findListOptions, linkToFollow.useCachedVersionIfAvailable, linkToFollow.reRequestOnStale, ...linkToFollow.linksToFollow);
          } else {
            return service.findByHref(href, linkToFollow.useCachedVersionIfAvailable, linkToFollow.reRequestOnStale, ...linkToFollow.linksToFollow);
          }
        } catch (e) {
          console.error(`Something went wrong when using @dataService(${matchingLinkDef.resourceType.value}) ${hasValue(service) ? '' : '(undefined) '}to resolve link ${linkToFollow.name} at ${href}`);
          throw e;
        }
      }
    } else if (!linkToFollow.isOptional) {
      throw new Error(`followLink('${linkToFollow.name}') was used as a required link for a ${model.constructor.name}, but there is no property on ${model.constructor.name} models with an @link() for ${linkToFollow.name}`);
    }

    return EMPTY;
  }

  /**
   * Resolve the given {@link FollowLinkConfig} for the given model and return the model with the
   * link property attached.
   *
   * @param model the {@link HALResource} to resolve the link for
   * @param linkToFollow the {@link FollowLinkConfig} to resolve
   */
  public resolveLink<T extends HALResource>(model, linkToFollow: FollowLinkConfig<T>): T {
    model[linkToFollow.name] = this.resolveLinkWithoutAttaching(model, linkToFollow);
    return model;
  }

  /**
   * Remove any resolved links that the model may have.
   *
   * @param model the {@link HALResource} to remove the links from
   * @returns a copy of the given model, without resolved links.
   */
  public removeResolvedLinks<T extends HALResource>(model: T): T {
    const result = Object.assign(new (model.constructor as GenericConstructor<T>)(), model);
    const linkDefs = this.getLinkDefinitions(model.constructor as GenericConstructor<T>);
    if (isNotEmpty(linkDefs)) {
      linkDefs.forEach((linkDef: LinkDefinition<T>) => {
        result[linkDef.propertyName] = undefined;
      });
    }
    return result;
  }

}
