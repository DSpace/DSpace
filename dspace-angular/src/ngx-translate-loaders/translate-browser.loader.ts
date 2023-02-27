import { TranslateLoader } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { TransferState } from '@angular/platform-browser';
import { NGX_TRANSLATE_STATE, NgxTranslateState } from './ngx-translate-state';
import { hasValue } from '../app/shared/empty.util';
import { map } from 'rxjs/operators';
import { of as observableOf, Observable } from 'rxjs';

/**
 * A TranslateLoader for ngx-translate to retrieve i18n messages from the TransferState, or download
 * them if they're not available there
 */
export class TranslateBrowserLoader implements TranslateLoader {
  constructor(
    protected transferState: TransferState,
    protected http: HttpClient,
    protected prefix?: string,
    protected suffix?: string
  ) {
  }

  /**
   * Return the i18n messages for a given language, first try to find them in the TransferState
   * retrieve them using HttpClient if they're not available there
   *
   * @param lang the language code
   */
  getTranslation(lang: string): Observable<any> {
    // Get the ngx-translate messages from the transfer state, to speed up the initial page load
    // client side
    const state = this.transferState.get<NgxTranslateState>(NGX_TRANSLATE_STATE, {});
    const messages = state[lang];
    if (hasValue(messages)) {
      return observableOf(messages);
    } else {
      // If they're not available on the transfer state (e.g. when running in dev mode), retrieve
      // them using HttpClient
      return this.http.get('' + this.prefix + lang + this.suffix, { responseType: 'text' }).pipe(
        map((json: any) => JSON.parse(json))
      );
    }
  }
}
