import { Component } from '@angular/core';
import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { ObjectNotFoundComponent } from './objectnotfound.component';

/**
 * Themed wrapper for ObjectNotFoundComponent
 */
@Component({
  selector: 'ds-themed-objnotfound',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})
export class ThemedObjectNotFoundComponent extends ThemedComponent<ObjectNotFoundComponent> {
  protected getComponentName(): string {
    return 'ObjectNotFoundComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/lookup-by-id/objectnotfound/objectnotfound.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./objectnotfound.component`);
  }

}
