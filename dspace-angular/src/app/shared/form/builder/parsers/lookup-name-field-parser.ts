import { FieldParser } from './field-parser';
import {
  DynamicLookupNameModel,
  DynamicLookupNameModelConfig
} from '../ds-dynamic-form-ui/models/lookup/dynamic-lookup-name.model';
import { FormFieldMetadataValueObject } from '../models/form-field-metadata-value.model';

export class LookupNameFieldParser extends FieldParser {

  public modelFactory(fieldValue?: FormFieldMetadataValueObject | any, label?: boolean): any {
    if (this.configData.selectableMetadata[0].controlledVocabulary) {
      const lookupModelConfig: DynamicLookupNameModelConfig = this.initModel(null, label);

      this.setVocabularyOptions(lookupModelConfig);

      this.setValues(lookupModelConfig, fieldValue, true);

      return new DynamicLookupNameModel(lookupModelConfig);
    }
  }

}
