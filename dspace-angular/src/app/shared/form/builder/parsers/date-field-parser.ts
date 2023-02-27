import { FieldParser } from './field-parser';
import {
  DynamicDsDatePickerModel,
  DynamicDsDateControlModelConfig
} from '../ds-dynamic-form-ui/models/date-picker/date-picker.model';
import { isNotEmpty } from '../../../empty.util';
import { DS_DATE_PICKER_SEPARATOR } from '../ds-dynamic-form-ui/models/date-picker/date-picker.component';
import { FormFieldMetadataValueObject } from '../models/form-field-metadata-value.model';

export class DateFieldParser extends FieldParser {

  public modelFactory(fieldValue?: FormFieldMetadataValueObject | any, label?: boolean): any {
    let malformedDate = false;
    const inputDateModelConfig: DynamicDsDateControlModelConfig = this.initModel(null, false, true);
    inputDateModelConfig.legend = this.configData.label;
    inputDateModelConfig.disabled = inputDateModelConfig.readOnly;
    inputDateModelConfig.toggleIcon = 'fas fa-calendar';
    this.setValues(inputDateModelConfig as any, fieldValue);
    // Init Data and validity check
    if (isNotEmpty(inputDateModelConfig.value)) {
      const value = inputDateModelConfig.value.toString();
      if (value.length >= 4) {
        const valuesArray = value.split(DS_DATE_PICKER_SEPARATOR);
        if (valuesArray.length < 4) {
          for (let i = 0; i < valuesArray.length; i++) {
            const len = i === 0 ? 4 : 2;
            if (valuesArray[i].length !== len) {
              malformedDate = true;
            }
          }
        }
      }

    }
    const dateModel = new DynamicDsDatePickerModel(inputDateModelConfig);
    dateModel.malformedDate = malformedDate;
    return dateModel;
  }
}
