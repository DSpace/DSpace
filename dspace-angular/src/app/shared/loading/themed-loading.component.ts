import { Component, Input, ComponentFactoryResolver, ChangeDetectorRef } from '@angular/core';
import { ThemedComponent } from '../theme-support/themed.component';
import { LoadingComponent } from './loading.component';
import { ThemeService } from '../theme-support/theme.service';

/**
 * Themed wrapper for LoadingComponent
 */
@Component({
  selector: 'ds-themed-loading',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})
export class ThemedLoadingComponent extends ThemedComponent<LoadingComponent> {

  @Input() message: string;
  @Input() showMessage = true;
  @Input() spinner = false;

  protected inAndOutputNames: (keyof LoadingComponent & keyof this)[] = ['message', 'showMessage', 'spinner'];

  constructor(
    protected resolver: ComponentFactoryResolver,
    protected cdr: ChangeDetectorRef,
    protected themeService: ThemeService
  ) {
    super(resolver, cdr, themeService);
  }

  protected getComponentName(): string {
    return 'LoadingComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/shared/loading/loading.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import('./loading.component');
  }
}
