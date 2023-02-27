import { ChangeDetectorRef, Component, ComponentFactoryResolver, Input } from '@angular/core';
import { ThemedComponent } from '../../../theme-support/themed.component';
import { ItemListPreviewComponent } from './item-list-preview.component';
import { Item } from '../../../../core/shared/item.model';
import { MyDspaceItemStatusType } from '../../../object-collection/shared/mydspace-item-status/my-dspace-item-status-type';
import { SearchResult } from '../../../search/models/search-result.model';
import { WorkflowItem } from 'src/app/core/submission/models/workflowitem.model';
import { ThemeService } from 'src/app/shared/theme-support/theme.service';

/**
 * Themed wrapper for ItemListPreviewComponent
 */
@Component({
  selector: 'ds-themed-item-list-preview',
  styleUrls: [],
  templateUrl: '../../../theme-support/themed.component.html'
})
export class ThemedItemListPreviewComponent extends ThemedComponent<ItemListPreviewComponent> {
  protected inAndOutputNames: (keyof ItemListPreviewComponent & keyof this)[] = ['item', 'object', 'status', 'showSubmitter', 'workflowItem'];

  @Input() item: Item;

  @Input() object: SearchResult<any>;

  @Input() status: MyDspaceItemStatusType;

  @Input() showSubmitter = false;

  @Input() workflowItem: WorkflowItem;

  constructor(
    protected resolver: ComponentFactoryResolver,
    protected cdr: ChangeDetectorRef,
    protected themeService: ThemeService,
  ) {
    super(resolver, cdr, themeService);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  protected getComponentName(): string {
    return 'ItemListPreviewComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../../../themes/${themeName}/app/shared/object-list/my-dspace-result-list-element/item-list-preview/item-list-preview.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import('./item-list-preview.component');
  }
}
