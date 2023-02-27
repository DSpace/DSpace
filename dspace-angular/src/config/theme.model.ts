/* eslint-disable max-classes-per-file */
import { Config } from './config.interface';
import { hasValue, hasNoValue, isNotEmpty } from '../app/shared/empty.util';
import { DSpaceObject } from '../app/core/shared/dspace-object.model';
import { getDSORoute } from '../app/app-routing-paths';
import { HandleObject } from '../app/core/shared/handle-object.model';
import { Injector } from '@angular/core';
import { HandleService } from '../app/shared/handle.service';

export interface NamedThemeConfig extends Config {
  name: string;

  /**
   * Specify another theme to build upon: whenever a themed component is not found in the current theme,
   * its ancestor theme(s) will be checked recursively before falling back to the default theme.
   */
  extends?: string;

  /**
   * A list of HTML tags that should be added to the HEAD section of the document, whenever this theme is active.
   */
  headTags?: HeadTagConfig[];
}

/**
 * Interface that represents a single theme-specific HTML tag in the HEAD section of the page.
 */
export interface HeadTagConfig extends Config {
  /**
   * The name of the HTML tag
   */
  tagName: string;

  /**
   * The attributes on the HTML tag
   */
  attributes?: {
    [key: string]: string;
  };
}

export interface RegExThemeConfig extends NamedThemeConfig {
  regex: string;
}

export interface HandleThemeConfig extends NamedThemeConfig {
  handle: string;
}

export interface UUIDThemeConfig extends NamedThemeConfig {
  uuid: string;
}

export class Theme {
  constructor(public config: NamedThemeConfig) {
  }

  matches(url: string, dso: DSpaceObject): boolean {
    return true;
  }
}

export class RegExTheme extends Theme {
  regex: RegExp;

  constructor(public config: RegExThemeConfig) {
    super(config);
    this.regex = new RegExp(this.config.regex);
  }

  matches(url: string, dso: DSpaceObject): boolean {
    let match;
    const route = getDSORoute(dso);

    if (isNotEmpty(route)) {
      match = route.match(this.regex);
    }

    if (hasNoValue(match)) {
      match = url.match(this.regex);
    }

    return hasValue(match);
  }
}

export class HandleTheme extends Theme {

  private normalizedHandle;

  constructor(public config: HandleThemeConfig,
              protected handleService: HandleService
              ) {
    super(config);
    this.normalizedHandle = this.handleService.normalizeHandle(this.config.handle);

  }

  matches<T extends DSpaceObject & HandleObject>(url: string, dso: T): boolean {
    return hasValue(dso) && hasValue(dso.handle)
      && this.handleService.normalizeHandle(dso.handle) === this.normalizedHandle;
  }
}

export class UUIDTheme extends Theme {
  constructor(public config: UUIDThemeConfig) {
    super(config);
  }

  matches(url: string, dso: DSpaceObject): boolean {
    return hasValue(dso) && dso.uuid === this.config.uuid;
  }
}

export const themeFactory = (config: ThemeConfig, injector: Injector): Theme => {
  if (hasValue((config as RegExThemeConfig).regex)) {
    return new RegExTheme(config as RegExThemeConfig);
  } else if (hasValue((config as HandleThemeConfig).handle)) {
    return new HandleTheme(config as HandleThemeConfig, injector.get(HandleService));
  } else if (hasValue((config as UUIDThemeConfig).uuid)) {
    return new UUIDTheme(config as UUIDThemeConfig);
  } else {
    return new Theme(config as NamedThemeConfig);
  }
};

export type ThemeConfig
  = NamedThemeConfig
  | RegExThemeConfig
  | HandleThemeConfig
  | UUIDThemeConfig;
