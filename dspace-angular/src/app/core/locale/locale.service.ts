import { Inject, Injectable } from '@angular/core';

import { TranslateService } from '@ngx-translate/core';

import { isEmpty, isNotEmpty } from '../../shared/empty.util';
import { CookieService } from '../services/cookie.service';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { combineLatest, Observable, of as observableOf } from 'rxjs';
import { map, mergeMap, take } from 'rxjs/operators';
import { NativeWindowRef, NativeWindowService } from '../services/window.service';
import { RouteService } from '../services/route.service';
import { DOCUMENT } from '@angular/common';

export const LANG_COOKIE = 'dsLanguage';

/**
 * This enum defines the possible origin of the languages
 */
export enum LANG_ORIGIN {
  UI,
  EPERSON,
  BROWSER
}

/**
 * Service to provide localization handler
 */
@Injectable()
export class LocaleService {

  /**
   * Eperson language metadata
   */
  EPERSON_LANG_METADATA = 'eperson.language';

  constructor(
    @Inject(NativeWindowService) protected _window: NativeWindowRef,
    protected cookie: CookieService,
    protected translate: TranslateService,
    protected authService: AuthService,
    protected routeService: RouteService,
    @Inject(DOCUMENT) protected document: any
  ) {
  }

  /**
   * Get the language currently used
   *
   * @returns {string} The language code
   */
  getCurrentLanguageCode(): string {
    // Attempt to get the language from a cookie
    let lang = this.getLanguageCodeFromCookie();
    if (isEmpty(lang)) {
      // Cookie not found
      // Attempt to get the browser language from the user
      if (this.translate.getLangs().includes(this.translate.getBrowserLang())) {
        lang = this.translate.getBrowserLang();
      } else {
        lang = environment.defaultLanguage;
      }
    }
    return lang;
  }

  /**
   * Get the languages list of the user in Accept-Language format
   *
   * @returns {Observable<string[]>}
   */
  getLanguageCodeList(): Observable<string[]> {
    const obs$ = combineLatest([
      this.authService.isAuthenticated(),
      this.authService.isAuthenticationLoaded()
    ]);

    return obs$.pipe(
      take(1),
      mergeMap(([isAuthenticated, isLoaded]) => {
        // TODO to enabled again when https://github.com/DSpace/dspace-angular/issues/739 will be resolved
        const epersonLang$: Observable<string[]> = observableOf([]);
/*        if (isAuthenticated && isLoaded) {
          epersonLang$ = this.authService.getAuthenticatedUserFromStore().pipe(
            take(1),
            map((eperson) => {
              const languages: string[] = [];
              const ePersonLang = eperson.firstMetadataValue(this.EPERSON_LANG_METADATA);
              if (ePersonLang) {
                languages.push(...this.setQuality(
                  [ePersonLang],
                  LANG_ORIGIN.EPERSON,
                  !isEmpty(this.translate.currentLang)));
              }
              return languages;
            })
          );
        }*/
        return epersonLang$.pipe(
          map((epersonLang: string[]) => {
            const languages: string[] = [];
            if (this.translate.currentLang) {
              languages.push(...this.setQuality(
                [this.translate.currentLang],
                LANG_ORIGIN.UI,
                false));
            }
            if (isNotEmpty(epersonLang)) {
              languages.push(...epersonLang);
            }
            if (navigator.languages) {
              languages.push(...this.setQuality(
                Object.assign([], navigator.languages),
                LANG_ORIGIN.BROWSER,
                !isEmpty(this.translate.currentLang))
              );
            }
            return languages;
          })
        );
      })
    );
  }

  /**
   * Retrieve the language from a cookie
   */
  getLanguageCodeFromCookie(): string {
    return this.cookie.get(LANG_COOKIE);
  }

  /**
   * Set the language currently used
   *
   * @param lang
   *  The language to save
   */
  saveLanguageCodeToCookie(lang: string): void {
    this.cookie.set(LANG_COOKIE, lang);
  }

  /**
   * Set the language currently used
   *
   * @param lang
   *  The language to set, if it's not provided retrieve default one
   */
  setCurrentLanguageCode(lang?: string): void {
    if (isEmpty(lang)) {
      lang = this.getCurrentLanguageCode();
    }
    this.translate.use(lang);
    this.saveLanguageCodeToCookie(lang);
    this.document.documentElement.lang = lang;
  }

  /**
   * Set the quality factor for all element of input array.
   * Returns a new array that contains the languages list with the quality value.
   * The quality factor indicate the relative degree of preference for the language
   * @param languages the languages list
   * @param origin origin of language list (UI, EPERSON, BROWSER)
   * @param hasOther true if contains other language, false otherwise
   */
  setQuality(languages: string[], origin: LANG_ORIGIN, hasOther: boolean): string[] {
    const langWithPrior = [];
    let idx = 0;
    const v = languages.length > 10 ? languages.length : 10;
    let divisor: number;
    switch (origin) {
      case LANG_ORIGIN.EPERSON:
        divisor = 2; break;
      case LANG_ORIGIN.BROWSER:
        divisor = (hasOther ? 10 : 1); break;
      default:
        divisor = 1;
    }
    languages.forEach( (lang) => {
        let value = lang + ';q=';
        let quality = (v - idx++) / v;
        quality = ((languages.length > 10) ? quality.toFixed(2) : quality) as number;
        value += quality / divisor;
        langWithPrior.push(value);
    });
    return langWithPrior;
  }

  /**
   * Refresh route navigated
   */
  public refreshAfterChangeLanguage() {
    this.routeService.getCurrentUrl().pipe(take(1)).subscribe((currentURL) => {
      // Hard redirect to the reload page with a unique number behind it
      // so that all state is definitely lost
      this._window.nativeWindow.location.href = `reload/${new Date().getTime()}?redirect=` + encodeURIComponent(currentURL);
    });

  }

}
