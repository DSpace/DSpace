import { Inject, Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import { getFirstCompletedRemoteData } from '../shared/operators';
import { ConfigurationProperty } from '../shared/configuration-property.model';
import { isNotEmpty } from '../../shared/empty.util';
import { DOCUMENT } from '@angular/common';
import { ConfigurationDataService } from '../data/configuration-data.service';
import { RemoteData } from '../data/remote-data';
import { map, switchMap, take } from 'rxjs/operators';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { CookieService } from '../services/cookie.service';
import { NativeWindowRef, NativeWindowService } from '../services/window.service';

export const CAPTCHA_COOKIE = '_GRECAPTCHA';
export const CAPTCHA_NAME = 'google-recaptcha';

/**
 * A GoogleRecaptchaService used to send action and get a token from REST
 */
@Injectable()
export class GoogleRecaptchaService {

  private renderer: Renderer2;

  /**
   * A Google Recaptcha version
   */
  private captchaVersionSubject$ = new BehaviorSubject<string>(null);

  /**
   * The Google Recaptcha Key
   */
  private captchaKeySubject$ = new BehaviorSubject<string>(null);

  /**
   * The Google Recaptcha mode
   */
  private captchaModeSubject$ = new BehaviorSubject<string>(null);

  captchaKey(): Observable<string> {
    return this.captchaKeySubject$.asObservable();
  }

  captchaMode(): Observable<string> {
    return this.captchaModeSubject$.asObservable();
  }

  captchaVersion(): Observable<string> {
    return this.captchaVersionSubject$.asObservable();
  }

  constructor(
    private cookieService: CookieService,
    @Inject(DOCUMENT) private _document: Document,
    @Inject(NativeWindowService) private _window: NativeWindowRef,
    rendererFactory: RendererFactory2,
    private configService: ConfigurationDataService,
  ) {
    if (this._window.nativeWindow) {
      this._window.nativeWindow.refreshCaptchaScript = this.refreshCaptchaScript;
    }
    this.renderer = rendererFactory.createRenderer(null, null);
    const registrationVerification$ = this.configService.findByPropertyName('registration.verification.enabled').pipe(
      take(1),
      getFirstCompletedRemoteData(),
      map((res: RemoteData<ConfigurationProperty>) => {
        return res.hasSucceeded && res.payload && isNotEmpty(res.payload.values) && res.payload.values[0].toLowerCase() === 'true';
      })
    );
    registrationVerification$.subscribe(registrationVerification => {
      if (registrationVerification) {
        this.loadRecaptchaProperties();
      }
    });
  }

  loadRecaptchaProperties() {
    const recaptchaKeyRD$ = this.configService.findByPropertyName('google.recaptcha.key.site').pipe(
      getFirstCompletedRemoteData(),
    );
    const recaptchaVersionRD$ = this.configService.findByPropertyName('google.recaptcha.version').pipe(
      getFirstCompletedRemoteData(),
    );
    const recaptchaModeRD$ = this.configService.findByPropertyName('google.recaptcha.mode').pipe(
      getFirstCompletedRemoteData(),
    );
    combineLatest([recaptchaVersionRD$, recaptchaModeRD$, recaptchaKeyRD$]).subscribe(([recaptchaVersionRD, recaptchaModeRD, recaptchaKeyRD]) => {

      if (
        this.cookieService.get('klaro-anonymous') && this.cookieService.get('klaro-anonymous')[CAPTCHA_NAME] &&
        recaptchaKeyRD.hasSucceeded && recaptchaVersionRD.hasSucceeded &&
        isNotEmpty(recaptchaVersionRD.payload?.values) && isNotEmpty(recaptchaKeyRD.payload?.values)
      ) {
        const key = recaptchaKeyRD.payload?.values[0];
        const version = recaptchaVersionRD.payload?.values[0];
        this.captchaKeySubject$.next(key);
        this.captchaVersionSubject$.next(version);

        let captchaUrl;
        switch (version) {
          case 'v3':
            if (recaptchaKeyRD.hasSucceeded && isNotEmpty(recaptchaKeyRD.payload?.values)) {
              captchaUrl = this.buildCaptchaUrl(key);
              this.captchaModeSubject$.next('invisible');
            }
            break;
          case 'v2':
            if (recaptchaModeRD.hasSucceeded && isNotEmpty(recaptchaModeRD.payload?.values)) {
              captchaUrl = this.buildCaptchaUrl();
              this.captchaModeSubject$.next(recaptchaModeRD.payload?.values[0]);
            }
            break;
          default:
          // TODO handle error
        }
        if (captchaUrl) {
          this.loadScript(captchaUrl);
        }
      }
    });
  }

  /**
   * Returns an observable of string
   * @param action action is the process type in which used to protect multiple spam REST calls
   */
  public getRecaptchaToken(action) {
    return this.captchaKey().pipe(
      switchMap((key) => grecaptcha.execute(key, {action: action}))
    );
  }

  /**
   * Returns an observable of string
   */
  public executeRecaptcha() {
    return of(grecaptcha.execute());
  }

  public getRecaptchaTokenResponse() {
    return grecaptcha.getResponse();
  }

  /**
   * Return the google captcha ur with google captchas api key
   *
   * @param key contains a secret key of a google captchas
   * @returns string which has google captcha url with google captchas key
   */
  buildCaptchaUrl(key?: string) {
    const apiUrl = 'https://www.google.com/recaptcha/api.js';
    return key ? `${apiUrl}?render=${key}` : apiUrl;
  }

  /**
   * Append the google captchas script to the document
   *
   * @param url contains a script url which will be loaded into page
   * @returns A promise
   */
  private loadScript(url) {
    return new Promise((resolve, reject) => {
      const script = this.renderer.createElement('script');
      script.type = 'text/javascript';
      script.src = url;
      script.text = ``;
      script.onload = resolve;
      script.onerror = reject;
      this.renderer.appendChild(this._document.head, script);
    });
  }

  refreshCaptchaScript = () => {
    this.loadRecaptchaProperties();
  };

}
