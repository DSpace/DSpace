import { FormBuilder, FormControl } from '@angular/forms';
import { fakeAsync, waitForAsync } from '@angular/core/testing';
import { ConfirmedValidator } from './confirmed.validator';

describe('ConfirmedValidator', () => {
  let passwordForm;

  beforeEach(waitForAsync(() => {

    passwordForm = (new FormBuilder()).group({
      password: new FormControl('', {}),
      confirmPassword: new FormControl('', {})
    }, {
      validator: ConfirmedValidator('password', 'confirmPassword')
    });
  }));

  it('should validate that the password and confirm password match', fakeAsync(() => {

    passwordForm.get('password').patchValue('test-password');
    passwordForm.get('confirmPassword').patchValue('test-password-mismatch');

    expect(passwordForm.valid).toBe(false);
  }));

  it('should invalidate that the password and confirm password match', fakeAsync(() => {
    passwordForm.get('password').patchValue('test-password');
    passwordForm.get('confirmPassword').patchValue('test-password');

    expect(passwordForm.valid).toBe(true);
  }));
});
