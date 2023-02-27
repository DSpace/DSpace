import { Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { MenuService } from '../menu.service';
import { getComponentForMenuItemType } from '../menu-item.decorator';
import { hasNoValue, hasValue } from '../../empty.util';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { MenuItemModel } from '../menu-item/models/menu-item.model';
import { distinctUntilChanged, switchMap } from 'rxjs/operators';
import { GenericConstructor } from '../../../core/shared/generic-constructor';
import { MenuSection } from '../menu-section.model';
import { MenuID } from '../menu-id.model';
import { MenuItemType } from '../menu-item-type.model';

/**
 * A basic implementation of a menu section's component
 */
@Component({
  selector: 'ds-menu-section',
  template: ''
})
export class MenuSectionComponent implements OnInit, OnDestroy {

  /**
   * Observable that emits whether or not this section is currently active
   */
  active: Observable<boolean>;

  /**
   * The ID of the menu this section resides in
   */
  menuID: MenuID;

  /**
   * List of available subsections in this section
   */
  subSections$: Observable<MenuSection[]>;

  /**
   * Map of components and injectors for each dynamically rendered menu section
   */
  sectionMap$: BehaviorSubject<Map<string, {
    injector: Injector,
    component: GenericConstructor<MenuSectionComponent>
  }>> = new BehaviorSubject(new Map());

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  subs: Subscription[] = [];

  constructor(public section: MenuSection, protected menuService: MenuService, protected injector: Injector) {
  }

  /**
   * Set initial values for instance variables
   */
  ngOnInit(): void {
    this.active = this.menuService.isSectionActive(this.menuID, this.section.id).pipe(distinctUntilChanged());
    this.initializeInjectorData();
  }

  /**
   * Activate this section if it's currently inactive, deactivate it when it's currently active
   * @param {Event} event The user event that triggered this method
   */
  toggleSection(event: Event) {
    event.preventDefault();
    if (!this.section.model?.disabled) {
      this.menuService.toggleActiveSection(this.menuID, this.section.id);
    }
  }

  /**
   * Activate this section
   * @param {Event} event The user event that triggered this method
   */
  activateSection(event: Event) {
    event.preventDefault();
    if (!this.section.model?.disabled) {
      this.menuService.activateSection(this.menuID, this.section.id);
    }
  }

  /**
   * Deactivate this section
   * @param {Event} event The user event that triggered this method
   */
  deactivateSection(event: Event) {
    event.preventDefault();
    this.menuService.deactivateSection(this.menuID, this.section.id);
  }

  /**
   * Method for initializing all injectors and component constructors for the menu items in this section
   */
  private initializeInjectorData() {
    this.updateSectionMap(
      this.section.id,
      this.getItemModelInjector(this.section.model),
      this.getMenuItemComponent(this.section.model)
    );
    this.subSections$ = this.menuService.getSubSectionsByParentID(this.menuID, this.section.id);
    this.subs.push(
      this.subSections$.pipe(
        // if you return an array from a switchMap it will emit each element as a separate event.
        // So this switchMap is equivalent to a subscribe with a forEach inside
        switchMap((sections: MenuSection[]) => sections)
      ).subscribe((section: MenuSection) => {
        this.updateSectionMap(
          section.id,
          this.getItemModelInjector(section.model),
          this.getMenuItemComponent(section.model)
        );
      })
    );
  }

  /**
   * Update the sectionMap
   */
  private updateSectionMap(id, injector, component) {
    const nextMap = this.sectionMap$.getValue();
    nextMap.set(id, { injector, component });
    this.sectionMap$.next(nextMap);
  }

  /**
   * Retrieve the component for a given MenuItemModel object
   * @param {MenuItemModel} itemModel The given MenuItemModel
   * @returns {GenericConstructor} Emits the constructor of the Component that should be used to render this menu item model
   */
  private getMenuItemComponent(itemModel?: MenuItemModel) {
    if (hasNoValue(itemModel)) {
      itemModel = this.section.model;
    }
    const type: MenuItemType = itemModel.type;
    return getComponentForMenuItemType(type);
  }

  /**
   * Retrieve the Injector for a given MenuItemModel object
   * @param {MenuItemModel} itemModel The given MenuItemModel
   * @returns {Injector} The Injector that injects the data for this menu item into the item's component
   */
  private getItemModelInjector(itemModel?: MenuItemModel) {
    if (hasNoValue(itemModel)) {
      itemModel = this.section.model;
    }
    return Injector.create({
      providers: [{ provide: 'itemModelProvider', useFactory: () => (itemModel), deps: [] }],
      parent: this.injector
    });
  }

  /**
   * Unsubscribe from open subscriptions
   */
  ngOnDestroy(): void {
    this.subs
      .filter((subscription) => hasValue(subscription))
      .forEach((subscription) => subscription.unsubscribe());
  }
}
