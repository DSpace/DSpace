import { autoserialize, autoserializeAs, deserialize, deserializeAs } from 'cerialize';
import { hasNoValue, hasValue, isUndefined } from '../../shared/empty.util';
import { ListableObject } from '../../shared/object-collection/shared/listable-object.model';
import { typedObject } from '../cache/builders/build-decorators';
import { excludeFromEquals } from '../utilities/equals.decorators';
import { DSPACE_OBJECT } from './dspace-object.resource-type';
import { GenericConstructor } from './generic-constructor';
import { HALLink } from './hal-link.model';
import {
  MetadataMap,
  MetadataMapSerializer,
  MetadataValue,
  MetadataValueFilter,
  MetadatumViewModel
} from './metadata.models';
import { Metadata } from './metadata.utils';
import { ResourceType } from './resource-type';
import { CacheableObject } from '../cache/cacheable-object.model';

/**
 * An abstract model class for a DSpaceObject.
 */
@typedObject
export class DSpaceObject extends ListableObject implements CacheableObject {
  /**
   * A string representing the kind of DSpaceObject, e.g. community, item, …
   */
  static type = DSPACE_OBJECT;

  @excludeFromEquals
  @deserializeAs('name')
  protected _name: string;

  /**
   * The human-readable identifier of this DSpaceObject
   */
  @excludeFromEquals
  @autoserializeAs(String, 'uuid')
  id: string;

  /**
   * The universally unique ide ntifier of this DSpaceObject
   */
  @autoserializeAs(String)
  uuid: string;

  /**
   * A string representing the kind of DSpaceObject, e.g. community, item, …
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * A shorthand to get this DSpaceObject's self link
   */
  get self(): string {
    return this._links.self.href;
  }

  /**
   * A shorthand to set this DSpaceObject's self link
   */
  set self(v: string) {
    this._links.self = {
      href: v
    };
  }

  /**
   * The name for this DSpaceObject
   * @deprecated use {@link DSONameService} instead
   */
  get name(): string {
    return (isUndefined(this._name)) ? this.firstMetadataValue('dc.title') : this._name;
  }

  /**
   * The name for this DSpaceObject
   */
  set name(name) {
    if (hasValue(this.firstMetadata('dc.title'))) {
      this.firstMetadata('dc.title').value = name;
    }
    this._name = name;
  }

  /**
   * All metadata of this DSpaceObject
   */
  @excludeFromEquals
  @autoserializeAs(MetadataMapSerializer)
  metadata: MetadataMap;

  @deserialize
  _links: {
    self: HALLink;
  };

  /**
   * Retrieve the current metadata as a list of MetadatumViewModels
   */
  get metadataAsList(): MetadatumViewModel[] {
    return Metadata.toViewModelList(this.metadata);
  }

  /**
   * Gets all matching metadata in this DSpaceObject.
   *
   * @param {string|string[]} keyOrKeys The metadata key(s) in scope. Wildcards are supported; see [[Metadata]].
   * @param {MetadataValueFilter} filter The value filter to use. If unspecified, no filtering will be done.
   * @returns {MetadataValue[]} the matching values or an empty array.
   */
  allMetadata(keyOrKeys: string | string[], valueFilter?: MetadataValueFilter): MetadataValue[] {
    return Metadata.all(this.metadata, keyOrKeys, valueFilter);
  }

  /**
   * Like [[allMetadata]], but only returns string values.
   *
   * @param {string|string[]} keyOrKeys The metadata key(s) in scope. Wildcards are supported; see [[Metadata]].
   * @param {MetadataValueFilter} filter The value filter to use. If unspecified, no filtering will be done.
   * @returns {string[]} the matching string values or an empty array.
   */
  allMetadataValues(keyOrKeys: string | string[], valueFilter?: MetadataValueFilter): string[] {
    return Metadata.allValues(this.metadata, keyOrKeys, valueFilter);
  }

  /**
   * Gets the first matching MetadataValue object in this DSpaceObject, or `undefined`.
   *
   * @param {string|string[]} keyOrKeys The metadata key(s) in scope. Wildcards are supported; see [[Metadata]].
   * @param {MetadataValueFilter} filter The value filter to use. If unspecified, no filtering will be done.
   * @returns {MetadataValue} the first matching value, or `undefined`.
   */
  firstMetadata(keyOrKeys: string | string[], valueFilter?: MetadataValueFilter): MetadataValue {
    return Metadata.first(this.metadata, keyOrKeys, valueFilter);
  }

  /**
   * Like [[firstMetadata]], but only returns a string value, or `undefined`.
   *
   * @param {string|string[]} keyOrKeys The metadata key(s) in scope. Wildcards are supported; see [[Metadata]].
   * @param {MetadataValueFilter} valueFilter The value filter to use. If unspecified, no filtering will be done.
   * @returns {string} the first matching string value, or `undefined`.
   */
  firstMetadataValue(keyOrKeys: string | string[], valueFilter?: MetadataValueFilter): string {
    return Metadata.firstValue(this.metadata, keyOrKeys, valueFilter);
  }

  /**
   * Checks for a matching metadata value in this DSpaceObject.
   *
   * @param {string|string[]} keyOrKeys The metadata key(s) in scope. Wildcards are supported; see [[Metadata]].
   * @param {MetadataValueFilter} filter The value filter to use. If unspecified, no filtering will be done.
   * @returns {boolean} whether a match is found.
   */
  hasMetadata(keyOrKeys: string | string[], valueFilter?: MetadataValueFilter): boolean {
    return Metadata.has(this.metadata, keyOrKeys, valueFilter);
  }

  /**
   * Find metadata on a specific field and order all of them using their "place" property.
   * @param key
   */
  findMetadataSortedByPlace(keyOrKeys: string | string[]): MetadataValue[] {
    return this.allMetadata(keyOrKeys).sort((a: MetadataValue, b: MetadataValue) => {
      if (hasNoValue(a.place) && hasNoValue(b.place)) {
        return 0;
      }
      if (hasNoValue(a.place)) {
        return -1;
      }
      if (hasNoValue(b.place)) {
        return 1;
      }
      return a.place - b.place;
    });
  }

  /**
   * Method that returns as which type of object this object should be rendered
   */
  getRenderTypes(): (string | GenericConstructor<ListableObject>)[] {
    return [this.constructor as GenericConstructor<ListableObject>];
  }

  setMetadata(key: string, language?: string, ...values: string[]) {
    const mdValues: MetadataValue[] = values.map((value: string, index: number) => {
      const md = new MetadataValue();
      md.value = value;
      md.authority = null;
      md.confidence = -1;
      md.language = language || null;
      md.place = index;
      return md;
    });
    if (hasNoValue(this.metadata)) {
      this.metadata = Object.create({});
    }
    this.metadata[key] = mdValues;
  }

  removeMetadata(key: string) {
    delete this.metadata[key];
  }

}
