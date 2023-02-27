import { makeStateKey } from '@angular/platform-browser';

/**
 * Represents ngx-translate messages in different languages in the TransferState
 */
export class NgxTranslateState {
  [lang: string]: {
    [key: string]: string
  }
}

/**
 * The key to store the NgxTranslateState as part of the TransferState
 */
export const NGX_TRANSLATE_STATE = makeStateKey<NgxTranslateState>('NGX_TRANSLATE_STATE');
