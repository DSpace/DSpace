import { Component, ComponentFactoryResolver, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Item } from '../../../../../core/shared/item.model';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import {
  getListableObjectComponent,
  listableObjectComponent
} from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { Context } from '../../../../../core/shared/context.model';
import { ItemSearchResult } from '../../../../../shared/object-collection/shared/item-search-result.model';
import { SearchResultGridElementComponent } from '../../../../../shared/object-grid/search-result-grid-element/search-result-grid-element.component';
import { TruncatableService } from '../../../../../shared/truncatable/truncatable.service';
import { BitstreamDataService } from '../../../../../core/data/bitstream-data.service';
import { GenericConstructor } from '../../../../../core/shared/generic-constructor';
import { ListableObjectDirective } from '../../../../../shared/object-collection/shared/listable-object/listable-object.directive';
import { ThemeService } from '../../../../../shared/theme-support/theme.service';

@listableObjectComponent(ItemSearchResult, ViewMode.GridElement, Context.AdminSearch)
@Component({
  selector: 'ds-item-admin-search-result-grid-element',
  styleUrls: ['./item-admin-search-result-grid-element.component.scss'],
  templateUrl: './item-admin-search-result-grid-element.component.html'
})
/**
 * The component for displaying a list element for an item search result on the admin search page
 */
export class ItemAdminSearchResultGridElementComponent extends SearchResultGridElementComponent<ItemSearchResult, Item> implements OnInit {
  @ViewChild(ListableObjectDirective, { static: true }) listableObjectDirective: ListableObjectDirective;
  @ViewChild('badges', { static: true }) badges: ElementRef;
  @ViewChild('buttons', { static: true }) buttons: ElementRef;

  constructor(protected truncatableService: TruncatableService,
              protected bitstreamDataService: BitstreamDataService,
              private themeService: ThemeService,
              private componentFactoryResolver: ComponentFactoryResolver
  ) {
    super(truncatableService, bitstreamDataService);
  }

  /**
   * Setup the dynamic child component
   */
  ngOnInit(): void {
    super.ngOnInit();
    const componentFactory = this.componentFactoryResolver.resolveComponentFactory(this.getComponent());

    const viewContainerRef = this.listableObjectDirective.viewContainerRef;
    viewContainerRef.clear();

    const componentRef = viewContainerRef.createComponent(
      componentFactory,
      0,
      undefined,
      [
        [this.badges.nativeElement],
        [this.buttons.nativeElement]
      ]);
    (componentRef.instance as any).object = this.object;
    (componentRef.instance as any).index = this.index;
    (componentRef.instance as any).linkType = this.linkType;
    (componentRef.instance as any).listID = this.listID;
  }

  /**
   * Fetch the component depending on the item's entity type, view mode and context
   * @returns {GenericConstructor<Component>}
   */
  private getComponent(): GenericConstructor<Component> {
    return getListableObjectComponent(this.object.getRenderTypes(), ViewMode.GridElement, undefined, this.themeService.getThemeName());
  }
}
