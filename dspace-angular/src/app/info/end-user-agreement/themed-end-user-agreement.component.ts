import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { EndUserAgreementComponent } from './end-user-agreement.component';

/**
 * Themed wrapper for EndUserAgreementComponent
 */
@Component({
  selector: 'ds-themed-end-user-agreement',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})
export class ThemedEndUserAgreementComponent extends ThemedComponent<EndUserAgreementComponent> {

  protected getComponentName(): string {
    return 'EndUserAgreementComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/info/end-user-agreement/end-user-agreement.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./end-user-agreement.component`);
  }

}
