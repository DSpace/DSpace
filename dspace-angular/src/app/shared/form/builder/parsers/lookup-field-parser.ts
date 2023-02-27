import { FieldParser } from './field-parser';
import { DynamicLookupModel, DynamicLookupModelConfig } from '../ds-dynamic-form-ui/models/lookup/dynamic-lookup.model';
import { FormFieldMetadataValueObject } from '../models/form-field-metadata-value.model';

export class LookupFieldParser extends FieldParser {

  public modelFactory(fieldValue?: FormFieldMetadataValueObject | any, label?: boolean): any {
    if (this.configData.selectableMetadata[0].controlledVocabulary) {
      const lookupModelConfig: DynamicLookupModelConfig = this.initModel(null, label);

      this.setVocabularyOptions(lookupModelConfig);

      this.setValues(lookupModelConfig, fieldValue, true);

      return new DynamicLookupModel(lookupModelConfig);

    }
  }
}
