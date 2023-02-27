import { MissingTranslationHandler, MissingTranslationHandlerParams } from '@ngx-translate/core';

/**
 * Class to handle missing translations for the ngx-translate library
 */
export class MissingTranslationHelper implements MissingTranslationHandler {
  /**
   * Called when there is not translation for a specific key
   * Will return the 'default' parameter of the translate pipe, if there is one available
   * @param params
   */
  handle(params: MissingTranslationHandlerParams) {
    if (params.interpolateParams) {
      return (params.interpolateParams as any).default || params.key;
    }
    return params.key;
  }
}
