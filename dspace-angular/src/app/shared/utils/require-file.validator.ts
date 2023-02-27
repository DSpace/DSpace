import {Directive} from '@angular/core';
import {NG_VALIDATORS, Validator, FormControl} from '@angular/forms';

@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
    selector: '[requireFile]',
    providers: [
        { provide: NG_VALIDATORS, useExisting: FileValidator, multi: true },
    ]
})
/**
 * Validator directive to validate if a file is selected
 */
export class FileValidator implements Validator {
    static validate(c: FormControl): {[key: string]: any} {
      return c.value == null || c.value.length === 0 ? { required : true } : null;
    }

    validate(c: FormControl): {[key: string]: any} {
      return FileValidator.validate(c);
    }
}
