import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { BehaviorSubject, Observable, of as observableOf, Subscription } from 'rxjs';
import { MenuService } from './menu.service';
import { distinctUntilChanged, map, mergeMap, switchMap } from 'rxjs/operators';
import { GenericConstructor } from '../../core/shared/generic-constructor';
import { hasValue, isNotEmptyOperator } from '../empty.util';
import { MenuSectionComponent } from './menu-section/menu-section.component';
import { getComponentForMenu } from './menu-section.decorator';
import { compareArraysUsingIds } from '../../item-page/simple/item-types/shared/item-relationships-utils';
import { MenuSection } from './menu-section.model';
import { MenuID } from './menu-id.model';
import { ActivatedRoute } from '@angular/router';
import { AuthorizationDataService } from '../../core/data/feature-authorization/authorization-data.service';
import { FeatureID } from '../../core/data/feature-authorization/feature-id';
import { ThemeService } from '../theme-support/theme.service';

/**
 * A basic implementation of a MenuComponent
 */
@Component({
  selector: 'ds-menu',
  template: ''
})
export class MenuComponent implements OnInit, OnDestroy {
  /**
   * The ID of the Menu (See MenuID)
   */
  menuID: MenuID;

  /**
   * Observable that emits whether or not this menu is currently collapsed
   */
  menuCollapsed: Observable<boolean>;

  /**
   * Observable that emits whether or not this menu's preview is currently collapsed
   */
  menuPreviewCollapsed: Observable<boolean>;

  /**
   * Observable that emits whether or not this menu is currently visible
   */
  menuVisible: Observable<boolean>;

  /**
   * List of top level sections in this Menu
   */
  sections: Observable<MenuSection[]>;

  /**
   * Map of components and injectors for each dynamically rendered menu section
   */
  sectionMap$: BehaviorSubject<Map<string, {
    injector: Injector,
    component: GenericConstructor<MenuSectionComponent>
  }>> = new BehaviorSubject(new Map());

  /**
   * Prevent unnecessary rerendering
   */
  changeDetection: ChangeDetectionStrategy.OnPush;

  /**
   * Timer to briefly delay the sidebar preview from opening or closing
   */
  private previewTimer;

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  subs: Subscription[] = [];

  private activatedRouteLastChild: ActivatedRoute;

  constructor(protected menuService: MenuService, protected injector: Injector, public authorizationService: AuthorizationDataService,
              public route: ActivatedRoute, protected themeService: ThemeService
  ) {
  }

  /**
   * Sets all instance variables to their initial values
   */
  ngOnInit(): void {
    this.activatedRouteLastChild = this.getActivatedRoute(this.route);
    this.menuCollapsed = this.menuService.isMenuCollapsed(this.menuID);
    this.menuPreviewCollapsed = this.menuService.isMenuPreviewCollapsed(this.menuID);
    this.menuVisible = this.menuService.isMenuVisible(this.menuID);
    this.sections = this.menuService.getMenuTopSections(this.menuID).pipe(distinctUntilChanged(compareArraysUsingIds()));

    this.subs.push(
      this.sections.pipe(
        // if you return an array from a switchMap it will emit each element as a separate event.
        // So this switchMap is equivalent to a subscribe with a forEach inside
        switchMap((sections: MenuSection[]) => sections),
        mergeMap((section: MenuSection) => {
          if (section.id.includes('statistics')) {
            return this.getAuthorizedStatistics(section);
          }
          return observableOf(section);
        }),
        isNotEmptyOperator(),
        switchMap((section: MenuSection) => this.getSectionComponent(section).pipe(
          map((component: GenericConstructor<MenuSectionComponent>) => ({ section, component }))
        )),
        distinctUntilChanged((x, y) => x.section.id === y.section.id)
      ).subscribe(({ section, component }) => {
        const nextMap = this.sectionMap$.getValue();
        nextMap.set(section.id, {
          injector: this.getSectionDataInjector(section),
          component
        });
        this.sectionMap$.next(nextMap);
      })
    );
  }

  /**
   *  Get activated route of the deepest activated route
   */
  getActivatedRoute(route) {
    if (route.children.length > 0) {
      return this.getActivatedRoute(route.firstChild);
    } else {
      return route;
    }
  }

  /**
   *  Get section of statistics after checking authorization
   */
  getAuthorizedStatistics(section) {
    return this.activatedRouteLastChild.data.pipe(
      switchMap((data) => {
        return this.authorizationService.isAuthorized(FeatureID.CanViewUsageStatistics, this.getObjectUrl(data)).pipe(
          map((canViewUsageStatistics: boolean) => {
            if (!canViewUsageStatistics) {
              return {};
            } else {
              return section;
            }
          }));
      })
    );
  }

  /**
   *  Get statistics route dso data
   */
  getObjectUrl(data) {
    const object = data.site ? data.site : data.dso?.payload;
    return object?._links?.self?.href;
  }

  /**
   *  Collapse this menu when it's currently expanded, expand it when its currently collapsed
   * @param {Event} event The user event that triggered this method
   */
  toggle(event: Event) {
    event.preventDefault();
    this.menuService.toggleMenu(this.menuID);
  }

  /**
   * Expand this menu
   * @param {Event} event The user event that triggered this method
   */
  expand(event: Event) {
    event.preventDefault();
    this.menuService.expandMenu(this.menuID);
  }

  /**
   * Collapse this menu
   * @param {Event} event The user event that triggered this method
   */
  collapse(event: Event) {
    event.preventDefault();
    this.menuService.collapseMenu(this.menuID);
  }

  /**
   * Expand this menu's preview
   * @param {Event} event The user event that triggered this method
   */
  expandPreview(event: Event) {
    event.preventDefault();
    this.previewToggleDebounce(() => this.menuService.expandMenuPreview(this.menuID), 100);
  }

  /**
   * Collapse this menu's preview
   * @param {Event} event The user event that triggered this method
   */
  collapsePreview(event: Event) {
    event.preventDefault();
    this.previewToggleDebounce(() => this.menuService.collapseMenuPreview(this.menuID), 400);
  }

  /**
   * delay the handler function by the given amount of time
   *
   * @param {Function} handler The function to delay
   * @param {number} ms The amount of ms to delay the handler function by
   */
  private previewToggleDebounce(handler: () => void, ms: number): void {
    if (hasValue(this.previewTimer)) {
      clearTimeout(this.previewTimer);
    }
    this.previewTimer = setTimeout(handler, ms);
  }

  /**
   * Retrieve the component for a given MenuSection object
   * @param {MenuSection} section The given MenuSection
   * @returns {Observable<GenericConstructor<MenuSectionComponent>>} Emits the constructor of the Component that should be used to render this object
   */
  private getSectionComponent(section: MenuSection): Observable<GenericConstructor<MenuSectionComponent>> {
    return this.menuService.hasSubSections(this.menuID, section.id).pipe(
      map((expandable: boolean) => {
        return getComponentForMenu(this.menuID, expandable, this.themeService.getThemeName());
      }
      ),
    );
  }

  /**
   * Retrieve the Injector for a given MenuSection object
   * @param {MenuSection} section The given MenuSection
   * @returns {Injector} The Injector that injects the data for this menu section into the section's component
   */
  private getSectionDataInjector(section: MenuSection) {
    return Injector.create({
      providers: [{ provide: 'sectionDataProvider', useFactory: () => (section), deps: [] }],
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
