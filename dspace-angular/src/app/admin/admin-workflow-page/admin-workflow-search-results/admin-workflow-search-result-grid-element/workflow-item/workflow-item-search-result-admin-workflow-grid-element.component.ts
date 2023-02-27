import { Component, ComponentFactoryResolver, ElementRef, ViewChild } from '@angular/core';
import { Item } from '../../../../../core/shared/item.model';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import {
  getListableObjectComponent,
  listableObjectComponent
} from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { Context } from '../../../../../core/shared/context.model';
import { SearchResultGridElementComponent } from '../../../../../shared/object-grid/search-result-grid-element/search-result-grid-element.component';
import { TruncatableService } from '../../../../../shared/truncatable/truncatable.service';
import { BitstreamDataService } from '../../../../../core/data/bitstream-data.service';
import { GenericConstructor } from '../../../../../core/shared/generic-constructor';
import { ListableObjectDirective } from '../../../../../shared/object-collection/shared/listable-object/listable-object.directive';
import { WorkflowItem } from '../../../../../core/submission/models/workflowitem.model';
import { Observable } from 'rxjs';
import { LinkService } from '../../../../../core/cache/builders/link.service';
import { followLink } from '../../../../../shared/utils/follow-link-config.model';
import { RemoteData } from '../../../../../core/data/remote-data';
import {
  getAllSucceededRemoteData,
  getRemoteDataPayload
} from '../../../../../core/shared/operators';
import { take } from 'rxjs/operators';
import { WorkflowItemSearchResult } from '../../../../../shared/object-collection/shared/workflow-item-search-result.model';
import { ThemeService } from '../../../../../shared/theme-support/theme.service';

@listableObjectComponent(WorkflowItemSearchResult, ViewMode.GridElement, Context.AdminWorkflowSearch)
@Component({
  selector: 'ds-workflow-item-search-result-admin-workflow-grid-element',
  styleUrls: ['./workflow-item-search-result-admin-workflow-grid-element.component.scss'],
  templateUrl: './workflow-item-search-result-admin-workflow-grid-element.component.html'
})
/**
 * The component for displaying a grid element for an workflow item on the admin workflow search page
 */
export class WorkflowItemSearchResultAdminWorkflowGridElementComponent extends SearchResultGridElementComponent<WorkflowItemSearchResult, WorkflowItem> {
  /**
   * Directive used to render the dynamic component in
   */
  @ViewChild(ListableObjectDirective, { static: true }) listableObjectDirective: ListableObjectDirective;

  /**
   * The html child that contains the badges html
   */
  @ViewChild('badges', { static: true }) badges: ElementRef;

  /**
   * The html child that contains the button html
   */
  @ViewChild('buttons', { static: true }) buttons: ElementRef;

  /**
   * The item linked to the workflow item
   */
  public item$: Observable<Item>;

  constructor(
    private componentFactoryResolver: ComponentFactoryResolver,
    private linkService: LinkService,
    protected truncatableService: TruncatableService,
    private themeService: ThemeService,
    protected bitstreamDataService: BitstreamDataService
  ) {
    super(truncatableService, bitstreamDataService);
  }

  /**
   * Setup the dynamic child component
   * Initialize the item object from the workflow item
   */
  ngOnInit(): void {
    super.ngOnInit();
    this.dso = this.linkService.resolveLink(this.dso, followLink('item'));
    this.item$ = (this.dso.item as Observable<RemoteData<Item>>).pipe(getAllSucceededRemoteData(), getRemoteDataPayload());
    this.item$.pipe(take(1)).subscribe((item: Item) => {
        const componentFactory = this.componentFactoryResolver.resolveComponentFactory(this.getComponent(item));

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
        (componentRef.instance as any).object = item;
        (componentRef.instance as any).index = this.index;
        (componentRef.instance as any).linkType = this.linkType;
        (componentRef.instance as any).listID = this.listID;
        componentRef.changeDetectorRef.detectChanges();
      }
    );
  }

  /**
   * Fetch the component depending on the item's entity type, view mode and context
   * @returns {GenericConstructor<Component>}
   */
  private getComponent(item: Item): GenericConstructor<Component> {
    return getListableObjectComponent(item.getRenderTypes(), ViewMode.GridElement, undefined, this.themeService.getThemeName());
  }

}
