import { hasValue, isEmpty, isNotEmpty, isNotNull } from '../../../empty.util';
import { ConfidenceType } from '../../../../core/shared/confidence-type';
import { MetadataValueInterface, VIRTUAL_METADATA_PREFIX } from '../../../../core/shared/metadata.models';
import { PLACEHOLDER_PARENT_METADATA } from '../ds-dynamic-form-ui/ds-dynamic-form-constants';

export interface OtherInformation {
  [name: string]: string;
}

/**
 * A class representing a specific input-form field's value
 */
export class FormFieldMetadataValueObject implements MetadataValueInterface {
  metadata?: string;
  value: any;
  display: string;
  language: any;
  authority: string;
  confidence: ConfidenceType;
  place: number;
  label: string;
  otherInformation: OtherInformation;

  constructor(value: any = null,
              language: any = null,
              authority: string = null,
              display: string = null,
              place: number = 0,
              confidence: number = null,
              otherInformation: any = null,
              metadata: string = null) {
    this.value = isNotNull(value) ? ((typeof value === 'string') ? value.trim() : value) : null;
    this.language = language;
    this.authority = authority;
    this.display = display || value;

    this.confidence = confidence;
    if (authority != null && (isEmpty(confidence) || confidence === -1)) {
      this.confidence = ConfidenceType.CF_ACCEPTED;
    } else if (isNotEmpty(confidence)) {
      this.confidence = confidence;
    } else {
      this.confidence = ConfidenceType.CF_UNSET;
    }

    this.place = place;
    if (isNotEmpty(metadata)) {
      this.metadata = metadata;
    }

    this.otherInformation = otherInformation;
  }

  /**
   * Returns true if this this object has an authority value
   */
  hasAuthority(): boolean {
    return isNotEmpty(this.authority);
  }

  /**
   * Returns true if this this object has a value
   */
  hasValue(): boolean {
    return isNotEmpty(this.value);
  }

  /**
   * Returns true if this this object has otherInformation property with value
   */
  hasOtherInformation(): boolean {
    return isNotEmpty(this.otherInformation);
  }

  /**
   * Returns true if this object value contains a placeholder
   */
  hasPlaceholder() {
    return this.hasValue() && this.value === PLACEHOLDER_PARENT_METADATA;
  }

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

  toString() {
    return this.display || this.value;
  }
}
