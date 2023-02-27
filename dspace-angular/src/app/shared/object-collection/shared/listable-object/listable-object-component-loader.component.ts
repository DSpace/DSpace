import {
  ChangeDetectorRef,
  Component,
  ComponentRef,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';

import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';

import { ListableObject } from '../listable-object.model';
import { ViewMode } from '../../../../core/shared/view-mode.model';
import { Context } from '../../../../core/shared/context.model';
import { getListableObjectComponent } from './listable-object.decorator';
import { GenericConstructor } from '../../../../core/shared/generic-constructor';
import { ListableObjectDirective } from './listable-object.directive';
import { CollectionElementLinkType } from '../../collection-element-link.type';
import { hasValue, isNotEmpty } from '../../../empty.util';
import { DSpaceObject } from '../../../../core/shared/dspace-object.model';
import { ThemeService } from '../../../theme-support/theme.service';

@Component({
  selector: 'ds-listable-object-component-loader',
  styleUrls: ['./listable-object-component-loader.component.scss'],
  templateUrl: './listable-object-component-loader.component.html'
})
/**
 * Component for determining what component to use depending on the item's entity type (dspace.entity.type)
 */
export class ListableObjectComponentLoaderComponent implements OnInit, OnChanges, OnDestroy {
  /**
   * The item or metadata to determine the component for
   */
  @Input() object: ListableObject;

  /**
   * The index of the object in the list
   */
  @Input() index: number;

  /**
   * The preferred view-mode to display
   */
  @Input() viewMode: ViewMode;

  /**
   * The context of listable object
   */
  @Input() context: Context;

  /**
   * The type of link used to render the links inside the listable object
   */
  @Input() linkType: CollectionElementLinkType;

  /**
   * The identifier of the list this element resides in
   */
  @Input() listID: string;

  /**
   * Whether to show the badge label or not
   */
  @Input() showLabel = true;

  /**
   * The value to display for this element
   */
  @Input() value: string;

  /**
   * Whether or not informational badges (e.g. Private, Withdrawn) should be hidden
   */
  @Input() hideBadges = false;

  /**
   * Directive hook used to place the dynamic child component
   */
  @ViewChild(ListableObjectDirective, { static: true }) listableObjectDirective: ListableObjectDirective;

  /**
   * View on the badges template, to be passed on to the loaded component (which will place the badges in the desired
   * location, or on top if not specified)
   */
  @ViewChild('badges', { static: true }) badges: ElementRef;

  /**
   * Emit when the listable object has been reloaded.
   */
  @Output() contentChange = new EventEmitter<ListableObject>();

  /**
   * Whether or not the "Private" badge should be displayed for this listable object
   */
  privateBadge = false;

  /**
   * Whether or not the "Withdrawn" badge should be displayed for this listable object
   */
  withdrawnBadge = false;

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  protected subs: Subscription[] = [];

  /**
   * The reference to the dynamic component
   */
  protected compRef: ComponentRef<Component>;

  /**
   * The list of input and output names for the dynamic component
   */
  protected inAndOutputNames: string[] = [
    'object',
    'index',
    'linkType',
    'listID',
    'showLabel',
    'context',
    'viewMode',
    'value',
    'hideBadges',
    'contentChange',
  ];

  constructor(private cdr: ChangeDetectorRef, private themeService: ThemeService) {
  }

  /**
   * Setup the dynamic child component
   */
  ngOnInit(): void {
    this.instantiateComponent(this.object);
  }

  /**
   * Whenever the inputs change, update the inputs of the dynamic component
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (this.inAndOutputNames.some((name: any) => hasValue(changes[name]))) {
      this.connectInputsAndOutputs();
    }
  }

  ngOnDestroy() {
    this.subs
      .filter((subscription) => hasValue(subscription))
      .forEach((subscription) => subscription.unsubscribe());
  }

  private instantiateComponent(object) {

    this.initBadges();

    const component = this.getComponent(object.getRenderTypes(), this.viewMode, this.context);

    const viewContainerRef = this.listableObjectDirective.viewContainerRef;
    viewContainerRef.clear();

    this.compRef = viewContainerRef.createComponent(
      component, {
        index: 0,
        injector: undefined,
        projectableNodes: [
          [this.badges.nativeElement],
        ]
      }
    );

    this.connectInputsAndOutputs();

    if ((this.compRef.instance as any).reloadedObject) {
      (this.compRef.instance as any).reloadedObject.pipe(
        take(1)
      ).subscribe((reloadedObject: DSpaceObject) => {
        if (reloadedObject) {
          this.compRef.destroy();
          this.object = reloadedObject;
          this.instantiateComponent(reloadedObject);
          this.cdr.detectChanges();
          this.contentChange.emit(reloadedObject);
        }
      });
    }
  }

  /**
   * Initialize which badges should be visible in the listable component
   */
  initBadges() {
    let objectAsAny = this.object as any;
    if (hasValue(objectAsAny.indexableObject)) {
      objectAsAny = objectAsAny.indexableObject;
    }
    const objectExistsAndValidViewMode = hasValue(objectAsAny) && this.viewMode !== ViewMode.StandalonePage;
    this.privateBadge = objectExistsAndValidViewMode && hasValue(objectAsAny.isDiscoverable) && !objectAsAny.isDiscoverable;
    this.withdrawnBadge = objectExistsAndValidViewMode && hasValue(objectAsAny.isWithdrawn) && objectAsAny.isWithdrawn;
  }

  /**
   * Fetch the component depending on the item's entity type, view mode and context
   * @returns {GenericConstructor<Component>}
   */
  getComponent(renderTypes: (string | GenericConstructor<ListableObject>)[],
               viewMode: ViewMode,
               context: Context): GenericConstructor<Component> {
    return getListableObjectComponent(renderTypes, viewMode, context, this.themeService.getThemeName());
  }

  /**
   * Connect the in and outputs of this component to the dynamic component,
   * to ensure they're in sync
   */
  protected connectInputsAndOutputs(): void {
    if (isNotEmpty(this.inAndOutputNames) && hasValue(this.compRef) && hasValue(this.compRef.instance)) {
      this.inAndOutputNames.forEach((name: any) => {
        this.compRef.instance[name] = this[name];
      });
    }
  }

}
