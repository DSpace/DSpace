import { ThemedComponent } from '../../shared/theme-support/themed.component';
import { CommunityPageSubCollectionListComponent } from './community-page-sub-collection-list.component';
import { Component, Input } from '@angular/core';
import { Community } from '../../core/shared/community.model';

@Component({
  selector: 'ds-themed-community-page-sub-collection-list',
  styleUrls: [],
  templateUrl: '../../shared/theme-support/themed.component.html',
})
export class ThemedCollectionPageSubCollectionListComponent extends ThemedComponent<CommunityPageSubCollectionListComponent> {
  @Input() community: Community;
  @Input() pageSize: number;
  protected inAndOutputNames: (keyof CommunityPageSubCollectionListComponent & keyof this)[] = ['community', 'pageSize'];

  protected getComponentName(): string {
    return 'CommunityPageSubCollectionListComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/community-page/sub-collection-list/community-page-sub-collection-list.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./community-page-sub-collection-list.component`);
  }

}
