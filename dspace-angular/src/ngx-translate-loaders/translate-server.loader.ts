import { TranslateLoader } from '@ngx-translate/core';
import { Observable, of as observableOf } from 'rxjs';
import { readFileSync } from 'fs';
import { TransferState } from '@angular/platform-browser';
import { NGX_TRANSLATE_STATE, NgxTranslateState } from './ngx-translate-state';

/**
 * A TranslateLoader for ngx-translate to parse json5 files server-side, and store them in the
 * TransferState
 */
export class TranslateServerLoader implements TranslateLoader {

  constructor(
    protected transferState: TransferState,
    protected prefix: string = 'dist/assets/i18n/',
    protected suffix: string = '.json'
  ) {
  }

  /**
   * Return the i18n messages for a given language, and store them in the TransferState
   *
   * @param lang the language code
   */
  public getTranslation(lang: string): Observable<any> {
    // Retrieve the file for the given language, and parse it
    const messages = JSON.parse(readFileSync(`${this.prefix}${lang}${this.suffix}`, 'utf8'));
    // Store the parsed messages in the transfer state so they'll be available immediately when the
    // app loads on the client
    this.storeInTransferState(lang, messages);
    // Return the parsed messages to translate things server side
    return observableOf(messages);
  }

  /**
   * Store the i18n messages for the given language code in the transfer state, so they can be
   * retrieved client side
   *
   * @param lang the language code
   * @param messages the i18n messages
   * @protected
   */
  protected storeInTransferState(lang: string, messages) {
    const prevState = this.transferState.get<NgxTranslateState>(NGX_TRANSLATE_STATE, {});
    const nextState = Object.assign({}, prevState, {
      [lang]: messages
    });
    this.transferState.set(NGX_TRANSLATE_STATE, nextState);
  }
}
