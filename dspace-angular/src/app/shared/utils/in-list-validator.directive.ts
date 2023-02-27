import { Directive, Input } from '@angular/core';
import { FormControl, NG_VALIDATORS, ValidationErrors, Validator } from '@angular/forms';
import { inListValidator } from './validator.functions';

/**
 * Directive for validating if a ngModel value is in a given list
 */
@Directive({
  selector: '[ngModel][dsInListValidator]',
  // We add our directive to the list of existing validators
  providers: [
    { provide: NG_VALIDATORS, useExisting: InListValidator, multi: true }
  ]
})
export class InListValidator implements Validator {
  /**
   * The list to look in
   */
  @Input()
  dsInListValidator: string[];

  /**
   * The function that checks if the form control's value is currently valid
   * @param c The FormControl
   */
  validate(c: FormControl): ValidationErrors | null {
    return inListValidator(this.dsInListValidator)(c);
  }
}
