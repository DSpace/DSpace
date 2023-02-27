import { FieldParser } from './field-parser';
import { FormFieldMetadataValueObject } from '../models/form-field-metadata-value.model';
import { DsDynamicDisabledModelConfig, DynamicDisabledModel } from '../ds-dynamic-form-ui/models/disabled/dynamic-disabled.model';

/**
 * Field parser for disabled fields
 */
export class DisabledFieldParser extends FieldParser {

  public modelFactory(fieldValue?: FormFieldMetadataValueObject | any, label?: boolean): any {
    const emptyModelConfig: DsDynamicDisabledModelConfig = this.initModel(null, label);
    this.setValues(emptyModelConfig, fieldValue, true);
    return new DynamicDisabledModel(emptyModelConfig);
  }
}
