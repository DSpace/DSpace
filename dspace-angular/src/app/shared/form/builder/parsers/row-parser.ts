import { Injectable, Injector } from '@angular/core';

import { DYNAMIC_FORM_CONTROL_TYPE_ARRAY, DynamicFormGroupModelConfig } from '@ng-dynamic-forms/core';
import uniqueId from 'lodash/uniqueId';

import { isEmpty } from '../../../empty.util';
import { DynamicRowGroupModel } from '../ds-dynamic-form-ui/models/ds-dynamic-row-group-model';
import { FormFieldModel } from '../models/form-field.model';
import { CONFIG_DATA, FieldParser, INIT_FORM_VALUES, PARSER_OPTIONS, SUBMISSION_ID } from './field-parser';
import { ParserFactory } from './parser-factory';
import { ParserOptions } from './parser-options';
import { ParserType } from './parser-type';
import { setLayout } from './parser.utils';
import { DYNAMIC_FORM_CONTROL_TYPE_RELATION_GROUP } from '../ds-dynamic-form-ui/ds-dynamic-form-constants';

export const ROW_ID_PREFIX = 'df-row-group-config-';

@Injectable({
  providedIn: 'root'
})

/**
 * Parser the submission data for a single row
 */
export class RowParser {
  constructor(private parentInjector: Injector) {
  }

  public parse(submissionId: string,
               rowData,
               scopeUUID,
               initFormValues: any,
               submissionScope,
               readOnly: boolean,
               typeField: string): DynamicRowGroupModel {
    let fieldModel: any = null;
    let parsedResult = null;
    const config: DynamicFormGroupModelConfig = {
      id: uniqueId(ROW_ID_PREFIX),
      group: [],
    };

    const scopedFields: FormFieldModel[] = this.filterScopedFields(rowData.fields, submissionScope);

    const layoutDefaultGridClass = ' col-sm-' + Math.trunc(12 / scopedFields.length);
    const layoutClass = ' d-flex flex-column justify-content-start';

    const parserOptions: ParserOptions = {
      readOnly: readOnly,
      submissionScope: submissionScope,
      collectionUUID: scopeUUID,
      typeField: typeField
    };

    // Iterate over row's fields
    scopedFields.forEach((fieldData: FormFieldModel) => {

      const layoutFieldClass = (fieldData.style || layoutDefaultGridClass) + layoutClass;
      const parserProvider = ParserFactory.getProvider(fieldData.input.type as ParserType);
      if (parserProvider) {
        const fieldInjector = Injector.create({
          providers: [
            parserProvider,
            { provide: SUBMISSION_ID, useValue: submissionId },
            { provide: CONFIG_DATA, useValue: fieldData },
            { provide: INIT_FORM_VALUES, useValue: initFormValues },
            { provide: PARSER_OPTIONS, useValue: parserOptions }
          ],
          parent: this.parentInjector
        });

        fieldModel = fieldInjector.get(FieldParser).parse();
      } else {
        throw new Error(`unknown form control model type "${fieldData.input.type}" defined for Input field with label "${fieldData.label}".`,);
      }

      if (fieldModel) {
        if (fieldModel.type === DYNAMIC_FORM_CONTROL_TYPE_ARRAY || fieldModel.type === DYNAMIC_FORM_CONTROL_TYPE_RELATION_GROUP) {
          if (rowData.fields.length > 1) {
            setLayout(fieldModel, 'grid', 'host', layoutFieldClass);
            config.group.push(fieldModel);
            // if (isEmpty(parsedResult)) {
            //   parsedResult = [];
            // }
            // parsedResult.push(fieldModel);
          } else {
            parsedResult = fieldModel;
          }
          return;
        } else {
          if (Array.isArray(fieldModel)) {
            fieldModel.forEach((model) => {
              parsedResult = model;
              return;
            });
          } else {
            setLayout(fieldModel, 'grid', 'host', layoutFieldClass);
            config.group.push(fieldModel);
          }
        }
        fieldModel = null;
      }
    });

    if (config && !isEmpty(config.group)) {
      const clsGroup = {
        element: {
          control: 'form-row',
        }
      };
      const groupModel = new DynamicRowGroupModel(config, clsGroup);
      if (Array.isArray(parsedResult)) {
        parsedResult.push(groupModel);
      } else {
        parsedResult = groupModel;
      }
    }
    return parsedResult;
  }

  checksFieldScope(fieldScope, submissionScope) {
    return (isEmpty(fieldScope) || isEmpty(submissionScope) || fieldScope === submissionScope);
  }

  filterScopedFields(fields: FormFieldModel[], submissionScope): FormFieldModel[] {
    const filteredFields: FormFieldModel[] = [];
    fields.forEach((field: FormFieldModel) => {
      // Whether field scope doesn't match the submission scope, skip it
      if (this.checksFieldScope(field.scope, submissionScope)) {
        filteredFields.push(field);
      }
    });
    return filteredFields;
  }
}
