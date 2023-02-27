import { ThemedComponent } from '../shared/theme-support/themed.component';
import { RootComponent } from './root.component';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'ds-themed-root',
  styleUrls: [],
  templateUrl: '../shared/theme-support/themed.component.html',
})
export class ThemedRootComponent extends ThemedComponent<RootComponent> {
  /**
   * Whether or not the authentication is currently blocking the UI
   */
  @Input() shouldShowFullscreenLoader: boolean;

  /**
   * Whether or not the the application is loading;
   */
  @Input() shouldShowRouteLoader: boolean;

  protected inAndOutputNames: (keyof RootComponent & keyof this)[] = ['shouldShowRouteLoader', 'shouldShowFullscreenLoader'];

  protected getComponentName(): string {
    return 'RootComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../themes/${themeName}/app/root/root.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./root.component`);
  }

}
