import { Injectable, Inject, Injector } from '@angular/core';
import { Store, createFeatureSelector, createSelector, select } from '@ngrx/store';
import { BehaviorSubject, EMPTY, Observable, of as observableOf } from 'rxjs';
import { ThemeState } from './theme.reducer';
import { SetThemeAction, ThemeActionTypes } from './theme.actions';
import { expand, filter, map, switchMap, take, toArray } from 'rxjs/operators';
import { hasNoValue, hasValue, isNotEmpty } from '../empty.util';
import { RemoteData } from '../../core/data/remote-data';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import {
  getFirstCompletedRemoteData,
  getFirstSucceededRemoteData,
  getRemoteDataPayload
} from '../../core/shared/operators';
import { HeadTagConfig, Theme, ThemeConfig, themeFactory } from '../../../config/theme.model';
import { NO_OP_ACTION_TYPE, NoOpAction } from '../ngrx/no-op.action';
import { followLink } from '../utils/follow-link-config.model';
import { LinkService } from '../../core/cache/builders/link.service';
import { environment } from '../../../environments/environment';
import { DSpaceObjectDataService } from '../../core/data/dspace-object-data.service';
import { ActivatedRouteSnapshot, ResolveEnd, Router } from '@angular/router';
import { GET_THEME_CONFIG_FOR_FACTORY } from '../object-collection/shared/listable-object/listable-object.decorator';
import { distinctNext } from 'src/app/core/shared/distinct-next';
import { DOCUMENT } from '@angular/common';
import { getDefaultThemeConfig } from '../../../config/config.util';
import { BASE_THEME_NAME } from './theme.constants';

export const themeStateSelector = createFeatureSelector<ThemeState>('theme');

export const currentThemeSelector = createSelector(
  themeStateSelector,
  (state: ThemeState): string => hasValue(state) ? state.currentTheme : undefined
);

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  /**
   * The list of configured themes
   */
  themes: Theme[];

  /**
   * True if at least one theme depends on the route
   */
  hasDynamicTheme: boolean;

  private _isThemeLoading$ = new BehaviorSubject<boolean>(false);
  private _isThemeCSSLoading$ = new BehaviorSubject<boolean>(false);

  constructor(
    private store: Store<ThemeState>,
    private linkService: LinkService,
    private dSpaceObjectDataService: DSpaceObjectDataService,
    protected injector: Injector,
    @Inject(GET_THEME_CONFIG_FOR_FACTORY) private gtcf: (str) => ThemeConfig,
    private router: Router,
    @Inject(DOCUMENT) private document: any,
  ) {
    // Create objects from the theme configs in the environment file
    this.themes = environment.themes.map((themeConfig: ThemeConfig) => themeFactory(themeConfig, injector));
    this.hasDynamicTheme = environment.themes.some((themeConfig: any) =>
      hasValue(themeConfig.regex) ||
      hasValue(themeConfig.handle) ||
      hasValue(themeConfig.uuid)
    );
  }

  /**
   * Set the current theme
   * @param newName
   */
  setTheme(newName: string) {
    this.store.dispatch(new SetThemeAction(newName));
  }

  /**
   * The name of the current theme (synchronous)
   */
  getThemeName(): string {
    let currentTheme: string;
    this.store.pipe(
      select(currentThemeSelector),
      take(1)
    ).subscribe((name: string) =>
      currentTheme = name
    );
    return currentTheme;
  }

  /**
   * The name of the current theme (asynchronous, tracks changes)
   */
  getThemeName$(): Observable<string> {
    return this.store.pipe(
      select(currentThemeSelector)
    );
  }

  /**
   * Whether the theme is currently loading
   */
  get isThemeLoading$(): Observable<boolean> {
    return this._isThemeLoading$;
  }

  /**
   * Every time the theme is changed
   *   - if the theme name is valid, load it (CSS + <head> tags)
   *   - otherwise fall back to {@link getDefaultThemeConfig} or {@link BASE_THEME_NAME}
   * Should be called when initializing the app.
   * @param isBrowser
   */
  listenForThemeChanges(isBrowser: boolean): void {
    this.getThemeName$().subscribe((themeName: string) => {
      if (isBrowser) {
        // the theme css will never download server side, so this should only happen on the browser
        distinctNext(this._isThemeCSSLoading$, true);
      }
      if (hasValue(themeName)) {
        this.loadGlobalThemeConfig(themeName);
      } else {
        const defaultThemeConfig = getDefaultThemeConfig();
        if (hasValue(defaultThemeConfig)) {
          this.loadGlobalThemeConfig(defaultThemeConfig.name);
        } else {
          this.loadGlobalThemeConfig(BASE_THEME_NAME);
        }
      }
    });
  }

  /**
   * For every resolved route, check if it matches a dynamic theme. If it does, load that theme.
   * Should be called when initializing the app.
   */
  listenForRouteChanges(): void {
    this.router.events.pipe(
      filter(event => event instanceof ResolveEnd),
      switchMap((event: ResolveEnd) => this.updateThemeOnRouteChange$(event.urlAfterRedirects, event.state.root)),
      switchMap((changed) => {
        if (changed) {
          return this._isThemeCSSLoading$;
        } else {
          return [false];
        }
      })
    ).subscribe((changed) => {
      distinctNext(this._isThemeLoading$, changed);
    });
  }

  /**
   * Load a theme's configuration
   *   - CSS
   *   - <head> tags
   * @param themeName
   * @private
   */
  private loadGlobalThemeConfig(themeName: string): void {
    this.setThemeCss(themeName);
    this.setHeadTags(themeName);
  }

  /**
   * Update the theme css file in <head>
   *
   * @param themeName The name of the new theme
   * @private
   */
  private setThemeCss(themeName: string): void {
    const head = this.document.getElementsByTagName('head')[0];
    if (hasNoValue(head)) {
      return;
    }

    // Array.from to ensure we end up with an array, not an HTMLCollection, which would be
    // automatically updated if we add nodes later
    const currentThemeLinks = Array.from(head.getElementsByClassName('theme-css'));
    const link = this.document.createElement('link');
    link.setAttribute('rel', 'stylesheet');
    link.setAttribute('type', 'text/css');
    link.setAttribute('class', 'theme-css');
    link.setAttribute('href', `${encodeURIComponent(themeName)}-theme.css`);
    // wait for the new css to download before removing the old one to prevent a
    // flash of unstyled content
    link.onload = () => {
      if (isNotEmpty(currentThemeLinks)) {
        currentThemeLinks.forEach((currentThemeLink: any) => {
          if (hasValue(currentThemeLink)) {
            currentThemeLink.remove();
          }
        });
      }
      // the fact that this callback is used, proves we're on the browser.
      distinctNext(this._isThemeCSSLoading$, false);
    };
    head.appendChild(link);
  }

  /**
   * Update the page to add a theme's <head> tags
   * @param themeName the theme in question
   * @private
   */
  private setHeadTags(themeName: string): void {
    const head = this.document.getElementsByTagName('head')[0];
    if (hasNoValue(head)) {
      return;
    }

    // clear head tags
    const currentHeadTags = Array.from(head.getElementsByClassName('theme-head-tag'));
    if (hasValue(currentHeadTags)) {
      currentHeadTags.forEach((currentHeadTag: any) => currentHeadTag.remove());
    }

    // create new head tags (not yet added to DOM)
    const headTagFragment = this.document.createDocumentFragment();
    this.createHeadTags(themeName)
        .forEach(newHeadTag => headTagFragment.appendChild(newHeadTag));

    // add new head tags to DOM
    head.appendChild(headTagFragment);
  }

  /**
   * Create HTML elements for a theme's <head> tags
   * (including those defined in the parent theme, if applicable)
   * @param themeName the theme in question
   * @private
   */
  private createHeadTags(themeName: string): HTMLElement[] {
    const themeConfig = this.getThemeConfigFor(themeName);
    const headTagConfigs = themeConfig?.headTags;

    if (hasNoValue(headTagConfigs)) {
      const parentThemeName = themeConfig?.extends;
      if (hasValue(parentThemeName)) {
        // inherit the head tags of the parent theme
        return this.createHeadTags(parentThemeName);
      }
      const defaultThemeConfig = getDefaultThemeConfig();
      const defaultThemeName = defaultThemeConfig.name;
      if (
        hasNoValue(defaultThemeName) ||
        themeName === defaultThemeName ||
        themeName === BASE_THEME_NAME
      ) {
        // last resort, use fallback favicon.ico
        return [
          this.createHeadTag({
            'tagName': 'link',
            'attributes': {
              'rel': 'icon',
              'href': 'assets/images/favicon.ico',
              'sizes': 'any',
            }
          })
        ];
      }

      // inherit the head tags of the default theme
      return this.createHeadTags(defaultThemeConfig.name);
    }

    return headTagConfigs.map(this.createHeadTag.bind(this));
  }

  /**
   * Create a single <head> tag element
   * @param headTagConfig the configuration for this <head> tag
   * @private
   */
  private createHeadTag(headTagConfig: HeadTagConfig): HTMLElement {
    const tag = this.document.createElement(headTagConfig.tagName);

    if (hasValue(headTagConfig.attributes)) {
      Object.entries(headTagConfig.attributes)
            .forEach(([key, value]) => tag.setAttribute(key, value));
    }

    // 'class' attribute should always be 'theme-head-tag' for removal
    tag.setAttribute('class', 'theme-head-tag');

    return tag;
  }


  /**
   * Determine whether or not the theme needs to change depending on the current route's URL and snapshot data
   * If the snapshot contains a dso, this will be used to match a theme
   * If the snapshot contains a scope parameters, this will be used to match a theme
   * Otherwise the URL is matched against
   * If none of the above find a match, the theme doesn't change
   * @param currentRouteUrl
   * @param activatedRouteSnapshot
   * @return Observable boolean emitting whether or not the theme has been changed
   */
  updateThemeOnRouteChange$(currentRouteUrl: string, activatedRouteSnapshot: ActivatedRouteSnapshot): Observable<boolean> {
    // and the current theme from the store
    const currentTheme$: Observable<string> = this.store.pipe(select(currentThemeSelector));

    const action$ = currentTheme$.pipe(
      switchMap((currentTheme: string) => {
        const snapshotWithData = this.findRouteData(activatedRouteSnapshot);
        if (this.hasDynamicTheme === true && isNotEmpty(this.themes)) {
          if (hasValue(snapshotWithData) && hasValue(snapshotWithData.data) && hasValue(snapshotWithData.data.dso)) {
            const dsoRD: RemoteData<DSpaceObject> = snapshotWithData.data.dso;
            if (dsoRD.hasSucceeded) {
              // Start with the resolved dso and go recursively through its parents until you reach the top-level community
              return observableOf(dsoRD.payload).pipe(
                this.getAncestorDSOs(),
                map((dsos: DSpaceObject[]) => {
                  const dsoMatch = this.matchThemeToDSOs(dsos, currentRouteUrl);
                  return this.getActionForMatch(dsoMatch, currentTheme);
                })
              );
            }
          }
          if (hasValue(activatedRouteSnapshot.queryParams) && hasValue(activatedRouteSnapshot.queryParams.scope)) {
            const dsoFromScope$: Observable<RemoteData<DSpaceObject>> = this.dSpaceObjectDataService.findById(activatedRouteSnapshot.queryParams.scope);
            // Start with the resolved dso and go recursively through its parents until you reach the top-level community
            return dsoFromScope$.pipe(
              getFirstSucceededRemoteData(),
              getRemoteDataPayload(),
              this.getAncestorDSOs(),
              map((dsos: DSpaceObject[]) => {
                const dsoMatch = this.matchThemeToDSOs(dsos, currentRouteUrl);
                return this.getActionForMatch(dsoMatch, currentTheme);
              })
            );
          }

          // check whether the route itself matches
          const routeMatch = this.themes.find((theme: Theme) => theme.matches(currentRouteUrl, undefined));

          return [this.getActionForMatch(routeMatch, currentTheme)];
        }

        // If there are no themes configured, do nothing
        return [new NoOpAction()];
      }),
      take(1),
    );

    action$.pipe(
      filter((action) => action.type !== NO_OP_ACTION_TYPE),
    ).subscribe((action) => {
      this.store.dispatch(action);
    });

    return action$.pipe(
      map((action) => action.type === ThemeActionTypes.SET),
    );
  }

  /**
   * Find a DSpaceObject in one of the provided route snapshots their data
   * Recursively looks for the dso in the routes their child routes until it reaches a dead end or finds one
   * @param routes
   */
  findRouteData(...routes: ActivatedRouteSnapshot[]) {
    const result = routes.find((route) => hasValue(route.data) && hasValue(route.data.dso));
    if (hasValue(result)) {
      return result;
    } else {
      const nextLevelRoutes = routes
        .map((route: ActivatedRouteSnapshot) => route.children)
        .reduce((combined: ActivatedRouteSnapshot[], current: ActivatedRouteSnapshot[]) => [...combined, ...current]);
      if (isNotEmpty(nextLevelRoutes)) {
        return this.findRouteData(...nextLevelRoutes);
      } else {
        return undefined;
      }
    }
  }

  /**
   * An rxjs operator that will return an array of all the ancestors of the DSpaceObject used as
   * input. The initial DSpaceObject will be the first element of the output array, followed by
   * its parent, its grandparent etc
   *
   * @private
   */
  private getAncestorDSOs() {
    return (source: Observable<DSpaceObject>): Observable<DSpaceObject[]> =>
      source.pipe(
        expand((dso: DSpaceObject) => {
          // Check if the dso exists and has a parent link
          if (hasValue(dso) && typeof (dso as any).getParentLinkKey === 'function') {
            const linkName = (dso as any).getParentLinkKey();
            // If it does, retrieve it.
            return this.linkService.resolveLinkWithoutAttaching<DSpaceObject, DSpaceObject>(dso, followLink(linkName)).pipe(
              getFirstCompletedRemoteData(),
              map((rd: RemoteData<DSpaceObject>) => {
                if (hasValue(rd.payload)) {
                  // If there's a parent, use it for the next iteration
                  return rd.payload;
                } else {
                  // If there's no parent, or an error, return null, which will stop recursion
                  // in the next iteration
                  return null;
                }
              }),
            );
          }

          // The current dso has no value, or no parent. Return EMPTY to stop recursion
          return EMPTY;
        }),
        // only allow through DSOs that have a value
        filter((dso: DSpaceObject) => hasValue(dso)),
        // Wait for recursion to complete, and emit all results at once, in an array
        toArray()
      );
  }

  /**
   * return the action to dispatch based on the given matching theme
   *
   * @param newTheme The theme to create an action for
   * @param currentThemeName The name of the currently active theme
   * @private
   */
  private getActionForMatch(newTheme: Theme, currentThemeName: string): SetThemeAction | NoOpAction {
    if (hasValue(newTheme) && newTheme.config.name !== currentThemeName) {
      // If we have a match, and it isn't already the active theme, set it as the new theme
      return new SetThemeAction(newTheme.config.name);
    } else {
      // Otherwise, do nothing
      return new NoOpAction();
    }
  }

  /**
   * Check the given DSpaceObjects in order to see if they match the configured themes in order.
   * If a match is found, the matching theme is returned
   *
   * @param dsos The DSpaceObjects to check
   * @param currentRouteUrl The url for the current route
   * @private
   */
  private matchThemeToDSOs(dsos: DSpaceObject[], currentRouteUrl: string): Theme {
    // iterate over the themes in order, and return the first one that matches
    return this.themes.find((theme: Theme) => {
      // iterate over the dsos's in order (most specific one first, so Item, Collection,
      // Community), and return the first one that matches the current theme
      const match = dsos.find((dso: DSpaceObject) => theme.matches(currentRouteUrl, dso));
      return hasValue(match);
    });
  }

  /**
   * Searches for a ThemeConfig by its name;
   */
  getThemeConfigFor(themeName: string): ThemeConfig {
    return this.gtcf(themeName);
  }
}
