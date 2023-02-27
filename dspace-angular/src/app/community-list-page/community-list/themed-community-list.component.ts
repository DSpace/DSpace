import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { CommunityListComponent } from './community-list.component';
import { Component } from '@angular/core';


@Component({
  selector: 'ds-themed-community-list',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})
export class ThemedCommunityListComponent extends ThemedComponent<CommunityListComponent> {
  protected getComponentName(): string {
    return 'CommunityListComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/community-list-page/community-list/community-list.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./community-list.component`);
  }

}
