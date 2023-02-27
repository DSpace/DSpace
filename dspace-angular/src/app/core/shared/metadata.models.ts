/* eslint-disable max-classes-per-file */
import { v4 as uuidv4 } from 'uuid';
import { autoserialize, Serialize, Deserialize } from 'cerialize';
import { hasValue } from '../../shared/empty.util';

export const VIRTUAL_METADATA_PREFIX = 'virtual::';

/** A single metadata value and its properties. */
export interface MetadataValueInterface {

  /** The language. */
  language: string;

  /** The string value. */
  value: string;
}

/** A map of metadata keys to an ordered list of MetadataValue objects. */
export interface MetadataMapInterface {
  [key: string]: MetadataValueInterface[];
}

/** A map of metadata keys to an ordered list of MetadataValue objects. */
export class MetadataMap implements MetadataMapInterface {
  [key: string]: MetadataValue[];
}

/** A single metadata value and its properties. */
export class MetadataValue implements MetadataValueInterface {
  /** The uuid. */
  uuid: string = uuidv4();

  /** The language. */
  @autoserialize
  language: string;

  /** The string value. */
  @autoserialize
  value: string;

  /**
   * The place of this MetadataValue within its list of metadata
   * This is used to render metadata in a specific custom order
   */
  @autoserialize
  place: number;

  /** The authority key used for authority-controlled metadata */
  @autoserialize
  authority: string;

  /** The authority confidence value */
  @autoserialize
  confidence: number;

  /**
   * Returns true if this Metadatum's authority key starts with 'virtual::'
   */
  get isVirtual(): boolean {
    return hasValue(this.authority) && this.authority.startsWith(VIRTUAL_METADATA_PREFIX);
  }

  /**
   * If this is a virtual Metadatum, it returns everything in the authority key after 'virtual::'.
   * Returns undefined otherwise.
   */
  get virtualValue(): string {
    if (this.isVirtual) {
      return this.authority.substring(this.authority.indexOf(VIRTUAL_METADATA_PREFIX) + VIRTUAL_METADATA_PREFIX.length);
    } else {
      return undefined;
    }
  }
}

/** Constraints for matching metadata values. */
export interface MetadataValueFilter {
  /** The language constraint. */
  language?: string;

  /** The value constraint. */
  value?: string;

  /** The authority constraint. */
  authority?: string;

  /** Whether the value constraint should match without regard to case. */
  ignoreCase?: boolean;

  /** Whether the value constraint should match as a substring. */
  substring?: boolean;
}

export class MetadatumViewModel {
  /** The uuid. */
  uuid: string = uuidv4();

  /** The metadatafield key. */
  key: string;

  /** The language. */
  language: string;

  /** The string value. */
  value: string;

  /**
   * The place of this MetadataValue within its list of metadata
   * This is used to render metadata in a specific custom order
   */
  place: number;

  /** The authority key used for authority-controlled metadata */
  authority: string;

  /** The authority confidence value */
  confidence: number;
}

/** Serializer used for MetadataMaps.
 * This is necessary because Cerialize has trouble instantiating the MetadataValues using their constructor
 * when they are inside arrays which also represent the values in a map.
 */
export const MetadataMapSerializer = {
  Serialize(map: MetadataMap): any {
    const json = {};
    Object.keys(map).forEach((key: string) => {
      json[key] = Serialize(map[key], MetadataValue);
    });
    return json;
  },

  Deserialize(json: any): MetadataMap {
    const metadataMap: MetadataMap = {};
    Object.keys(json).forEach((key: string) => {
      metadataMap[key] = Deserialize(json[key], MetadataValue);
    });
    return metadataMap;
  }
};
