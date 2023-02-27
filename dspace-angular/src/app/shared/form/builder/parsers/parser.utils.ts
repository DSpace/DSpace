import { isNull, isUndefined } from '../../../empty.util';
import { DynamicFormControlLayout, DynamicFormControlLayoutConfig } from '@ng-dynamic-forms/core';

export function setLayout(model: any, controlLayout: string, controlLayoutConfig: string, style: string) {
  if (isNull(model.layout)) {
    model.layout = {} as DynamicFormControlLayout;
    model.layout[controlLayout] = {} as DynamicFormControlLayoutConfig;
    model.layout[controlLayout][controlLayoutConfig] = style;
  } else if (isUndefined(model.layout[controlLayout])) {
    model.layout[controlLayout] = {} as DynamicFormControlLayoutConfig;
    model.layout[controlLayout][controlLayoutConfig] = style;
  } else if (isUndefined(model.layout[controlLayout][controlLayoutConfig])) {
    model.layout[controlLayout][controlLayoutConfig] = style;
  } else {
    model.layout[controlLayout][controlLayoutConfig] = model.layout[controlLayout][controlLayoutConfig].concat(` ${style}`);
  }
}
