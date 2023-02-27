import { Config } from './config.interface';

/**
 * An interface to represent a language in the configuration. A LangConfig has a code which should be the official
 * language code for the language (e.g. ‘fr’), a label which should be the name of the language in that language
 * (e.g. ‘Français’), and a boolean to determine whether or not it should be listed in the language select.
 */
export interface LangConfig extends Config {
      code: string;
      label: string;
      active: boolean;
}
