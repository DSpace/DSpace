import { ViewMode } from '../../../../core/shared/view-mode.model';
import { Context } from '../../../../core/shared/context.model';
import { hasNoValue, hasValue, isNotEmpty } from '../../../empty.util';
import { GenericConstructor } from '../../../../core/shared/generic-constructor';
import { ListableObject } from '../listable-object.model';
import { environment } from '../../../../../environments/environment';
import { ThemeConfig } from '../../../../../config/theme.model';
import { InjectionToken } from '@angular/core';

export const DEFAULT_VIEW_MODE = ViewMode.ListElement;
export const DEFAULT_CONTEXT = Context.Any;
export const DEFAULT_THEME = '*';

/**
 * A class used to compare two matches and their relevancy to determine which of the two gains priority over the other
 *
 * "level" represents the index of the first default value that was used to find the match with:
 * ViewMode being index 0, Context index 1 and theme index 2. Examples:
 * - If a default value was used for context, but not view-mode and theme, the "level" will be 1
 * - If a default value was used for view-mode and context, but not for theme, the "level" will be 0
 * - If no default value was used for any of the fields, the "level" will be 3
 *
 * "relevancy" represents the amount of values that didn't require a default value to fall back on. Examples:
 * - If a default value was used for theme, but not view-mode and context, the "relevancy" will be 2
 * - If a default value was used for view-mode and context, but not for theme, the "relevancy" will be 1
 * - If a default value was used for all fields, the "relevancy" will be 0
 * - If no default value was used for any of the fields, the "relevancy" will be 3
 *
 * To determine which of two MatchRelevancies is the most relevant, we compare "level" and "relevancy" in that order.
 * If any of the two is higher than the other, that match is most relevant. Examples:
 * - { level: 1, relevancy: 1 } is more relevant than { level: 0, relevancy: 2 }
 * - { level: 1, relevancy: 1 } is less relevant than { level: 1, relevancy: 2 }
 * - { level: 1, relevancy: 1 } is more relevant than { level: 1, relevancy: 0 }
 * - { level: 1, relevancy: 1 } is less relevant than { level: 2, relevancy: 0 }
 * - { level: 1, relevancy: 1 } is more relevant than null
 */
class MatchRelevancy {
  constructor(public match: any,
              public level: number,
              public relevancy: number) {
  }

  isMoreRelevantThan(otherMatch: MatchRelevancy): boolean {
    if (hasNoValue(otherMatch)) {
      return true;
    }
    if (otherMatch.level > this.level) {
      return false;
    }
    if (otherMatch.level === this.level && otherMatch.relevancy > this.relevancy) {
      return false;
    }
    return true;
  }

  isLessRelevantThan(otherMatch: MatchRelevancy): boolean {
    return !this.isMoreRelevantThan(otherMatch);
  }
}

/**
 * Factory to allow us to inject getThemeConfigFor so we can mock it in tests
 */
export const GET_THEME_CONFIG_FOR_FACTORY = new InjectionToken<(str) => ThemeConfig>('getThemeConfigFor', {
  providedIn: 'root',
  factory: () => getThemeConfigFor
});

const map = new Map();

/**
 * Decorator used for rendering a listable object
 * @param objectType The object type or entity type the component represents
 * @param viewMode The view mode the component represents
 * @param context The optional context the component represents
 * @param theme The optional theme for the component
 */
export function listableObjectComponent(objectType: string | GenericConstructor<ListableObject>, viewMode: ViewMode, context: Context = DEFAULT_CONTEXT, theme = DEFAULT_THEME) {
  return function decorator(component: any) {
    if (hasNoValue(objectType)) {
      return;
    }
    if (hasNoValue(map.get(objectType))) {
      map.set(objectType, new Map());
    }
    if (hasNoValue(map.get(objectType).get(viewMode))) {
      map.get(objectType).set(viewMode, new Map());
    }
    if (hasNoValue(map.get(objectType).get(viewMode).get(context))) {
      map.get(objectType).get(viewMode).set(context, new Map());
    }
    map.get(objectType).get(viewMode).get(context).set(theme, component);
  };
}

/**
 * Getter to retrieve the matching listable object component
 *
 * Looping over the provided types, it'll attempt to find the best match depending on the {@link MatchRelevancy} returned by getMatch()
 * The most relevant match between types is kept and eventually returned
 *
 * @param types The types of which one should match the listable component
 * @param viewMode The view mode that should match the components
 * @param context The context that should match the components
 * @param theme The theme that should match the components
 */
export function getListableObjectComponent(types: (string | GenericConstructor<ListableObject>)[], viewMode: ViewMode, context: Context = DEFAULT_CONTEXT, theme: string = DEFAULT_THEME) {
  let currentBestMatch: MatchRelevancy = null;
  for (const type of types) {
    const typeMap = map.get(type);
    if (hasValue(typeMap)) {
      const match = getMatch(typeMap, [viewMode, context, theme], [DEFAULT_VIEW_MODE, DEFAULT_CONTEXT, DEFAULT_THEME]);
      if (hasNoValue(currentBestMatch) || currentBestMatch.isLessRelevantThan(match)) {
        currentBestMatch = match;
      }
    }
  }
  return hasValue(currentBestMatch) ? currentBestMatch.match : null;
}

/**
 * Find an object within a nested map, matching the provided keys as best as possible, falling back on defaults wherever
 * needed.
 *
 * Starting off with a Map, it loops over the provided keys, going deeper into the map until it finds a value
 * If at some point, no value is found, it'll attempt to use the default value for that index instead
 * If the default value exists, the index is stored in the "level"
 * If no default value exists, 1 is added to "relevancy"
 * See {@link MatchRelevancy} what these represent
 *
 * @param typeMap         a multi-dimensional map
 * @param keys            the keys of the multi-dimensional map to loop over. Each key represents a level within the map
 * @param defaults        the default values to use for each level, in case no value is found for the key at that index
 * @returns matchAndLevel a {@link MatchRelevancy} object containing the match and its level of relevancy
 */
function getMatch(typeMap: Map<any, any>, keys: any[], defaults: any[]): MatchRelevancy {
  let currentMap = typeMap;
  let level = -1;
  let relevancy = 0;
  for (let i = 0; i < keys.length; i++) {
    // If we're currently checking the theme, resolve it first to take extended themes into account
    let currentMatch = defaults[i] === DEFAULT_THEME ? resolveTheme(currentMap, keys[i]) : currentMap.get(keys[i]);
    if (hasNoValue(currentMatch)) {
      currentMatch = currentMap.get(defaults[i]);
      if (level === -1) {
        level = i;
      }
    } else {
      relevancy++;
    }
    if (hasValue(currentMatch)) {
      if (currentMatch instanceof Map) {
        currentMap = currentMatch as Map<any, any>;
      } else {
        return new MatchRelevancy(currentMatch, level > -1 ? level : i + 1, relevancy);
      }
    } else {
      return null;
    }
  }
  return null;
}

/**
 * Searches for a ThemeConfig by its name;
 */
export const getThemeConfigFor = (themeName: string): ThemeConfig => {
  return environment.themes.find(theme => theme.name === themeName);
};

/**
 * Find a match in the given map for the given theme name, taking theme extension into account
 *
 * @param contextMap A map of theme names to components
 * @param themeName The name of the theme to check
 * @param checkedThemeNames The list of theme names that are already checked
 */
export const resolveTheme = (contextMap: Map<any, any>, themeName: string, checkedThemeNames: string[] = []): any => {
  const match = contextMap.get(themeName);
  if (hasValue(match)) {
    return match;
  } else {
    const cfg = getThemeConfigFor(themeName);
    if (hasValue(cfg) && isNotEmpty(cfg.extends)) {
      const nextTheme = cfg.extends;
      const nextCheckedThemeNames = [...checkedThemeNames, themeName];
      if (checkedThemeNames.includes(nextTheme)) {
        throw new Error('Theme extension cycle detected: ' + [...nextCheckedThemeNames, nextTheme].join(' -> '));
      } else {
        return resolveTheme(contextMap, nextTheme, nextCheckedThemeNames);
      }
    }
  }
};
