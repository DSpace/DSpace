import {Component, Input} from '@angular/core';
import { ThemedComponent } from '../../theme-support/themed.component';
import { ComcolPageHandleComponent } from './comcol-page-handle.component';

/**
 * Themed wrapper for BreadcrumbsComponent
 */
@Component({
  selector: 'ds-themed-comcol-page-handle',
  styleUrls: [],
  templateUrl: '../../theme-support/themed.component.html',
})


export class ThemedComcolPageHandleComponent extends ThemedComponent<ComcolPageHandleComponent> {

// Optional title
  @Input() title: string;

// The value of "handle"
  @Input() content: string;

  inAndOutputNames: (keyof ComcolPageHandleComponent & keyof this)[] = ['title', 'content'];

  protected getComponentName(): string {
    return 'ComcolPageHandleComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../../themes/${themeName}/app/shared/comcol/comcol-page-handle/comcol-page-handle.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./comcol-page-handle.component`);
  }
}
