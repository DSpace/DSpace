import { autoserialize } from 'cerialize';

import { LanguageCode } from './form-field-language-value.model';
import { RelationshipOptions } from './relationship-options.model';
import { FormRowModel } from '../../../../core/config/models/config-submission-form.model';

/**
 * Representing SelectableMetadata properties
 */
export interface SelectableMetadata {
  /**
   * The key of the metadata field to use to store the input
   */
  metadata: string;

  /**
   * The label of the metadata field to use to store the input
   */
  label: string;

  /**
   * The name of the controlled vocabulary used to retrieve value for the input see controlled vocabularies
   */
  controlledVocabulary: string;

  /**
   * A boolean representing if value is closely related to the controlled vocabulary entry or not
   */
  closed: boolean;
}

/**
 * A class representing a specific input-form field
 */
export class FormFieldModel {

  /**
   * The hints for this metadata field to display on form
   */
  @autoserialize
  hints: string;

  /**
   * The label for this metadata field to display on form
   */
  @autoserialize
  label: string;

  /**
   * The languages available for this metadata field to display on form
   */
  @autoserialize
  languageCodes: LanguageCode[];

  /**
   * The error message for this metadata field to display on form in case of field is required
   */
  @autoserialize
  mandatoryMessage: string;

  /**
   * Representing if this metadata field is mandatory or not
   */
  @autoserialize
  mandatory: string;

  /**
   * Representing if this metadata field is repeatable or not
   */
  @autoserialize
  repeatable: boolean;

  /**
   * Containing additional properties for this metadata field
   */
  @autoserialize
  input: {
    /**
     * Representing the type for this metadata field
     */
    type: string;

    /**
     * Containing regex to use for field validation
     */
    regex?: string;
  };

  /**
   * Representing additional vocabulary configuration for this metadata field
   */
  @autoserialize
  selectableMetadata: SelectableMetadata[];

  /**
   * Representing additional relationship configuration for this metadata field
   */
  @autoserialize
  selectableRelationship: RelationshipOptions;

  @autoserialize
  rows: FormRowModel[];

  /**
   * Representing the scope for this metadata field
   */
  @autoserialize
  scope: string;

  /**
   * Containing additional css classes for this metadata field to use on form
   */
  @autoserialize
  style: string;

  /**
   * Containing types to bind for this field
   */
  @autoserialize
  typeBind: string[];

  /**
   * Containing the value for this metadata field
   */
  @autoserialize
  value: any;
}
