import { AbstractControl, ValidatorFn } from '@angular/forms';
import { isNotEmpty } from '../empty.util';

/**
 * Returns a validator function to check if the control's value is in a given list
 * @param list The list to look in
 */
export function inListValidator(list: string[]): ValidatorFn {
  return (control: AbstractControl): { [key: string]: any } | null => {
    const hasValue = isNotEmpty(control.value);
    let inList = true;
    if (isNotEmpty(list)) {
      inList = list.indexOf(control.value) > -1;
    }
    return (hasValue && inList) ? null : { inList: { value: control.value } };
  };
}
