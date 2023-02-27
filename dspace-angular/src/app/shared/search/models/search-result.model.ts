import { autoserialize, deserialize } from 'cerialize';
import { typedObject } from '../../../core/cache/builders/build-decorators';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { GenericConstructor } from '../../../core/shared/generic-constructor';
import { HALLink } from '../../../core/shared/hal-link.model';
import { MetadataMap } from '../../../core/shared/metadata.models';
import { excludeFromEquals, fieldsForEquals } from '../../../core/utilities/equals.decorators';
import { ListableObject } from '../../object-collection/shared/listable-object.model';
import { HALResource } from '../../../core/shared/hal-resource.model';
import { SEARCH_RESULT } from './types/search-result.resource-type';

/**
 * Represents a search result object of a certain (<T>) DSpaceObject
 */
@typedObject
export class SearchResult<T extends DSpaceObject> extends ListableObject implements HALResource {
  static type = SEARCH_RESULT;

  /**
   * Doesn't get a type in the rest response, so it's hardcoded
   */
  type = SEARCH_RESULT;

  /**
   * The metadata that was used to find this item, hithighlighted
   */
  @excludeFromEquals
  @autoserialize
  hitHighlights: MetadataMap;

  /**
   * The {@link HALLink}s for this SearchResult
   */
  @deserialize
  _links: {
    self: HALLink;
    indexableObject: HALLink;
  };

  /**
   * The DSpaceObject that was found
   */
  @fieldsForEquals('uuid')
  indexableObject: T;

  /**
   * Method that returns as which type of object this object should be rendered
   */
  getRenderTypes(): (string | GenericConstructor<ListableObject>)[] {
    return [this.constructor as GenericConstructor<ListableObject>];
  }
}
