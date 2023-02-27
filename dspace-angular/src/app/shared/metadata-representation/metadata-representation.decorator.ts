import { MetadataRepresentationType } from '../../core/shared/metadata-representation/metadata-representation.model';
import { hasNoValue, hasValue } from '../empty.util';
import { Context } from '../../core/shared/context.model';
import { InjectionToken } from '@angular/core';
import { GenericConstructor } from '../../core/shared/generic-constructor';
import {
  resolveTheme,
  DEFAULT_THEME, DEFAULT_CONTEXT
} from '../object-collection/shared/listable-object/listable-object.decorator';

export const METADATA_REPRESENTATION_COMPONENT_FACTORY = new InjectionToken<(entityType: string, mdRepresentationType: MetadataRepresentationType, context: Context, theme: string) => GenericConstructor<any>>('getMetadataRepresentationComponent', {
  providedIn: 'root',
  factory: () => getMetadataRepresentationComponent
});

export const map = new Map();

export const DEFAULT_ENTITY_TYPE = 'Publication';
export const DEFAULT_REPRESENTATION_TYPE = MetadataRepresentationType.PlainText;

/**
 * Decorator function to store metadata representation mapping
 * @param entityType The entity type the component represents
 * @param mdRepresentationType The metadata representation type the component represents
 * @param context The optional context the component represents
 * @param theme The optional theme for the component
 */
export function metadataRepresentationComponent(entityType: string, mdRepresentationType: MetadataRepresentationType, context: Context = DEFAULT_CONTEXT, theme = DEFAULT_THEME) {
  return function decorator(component: any) {
    if (hasNoValue(map.get(entityType))) {
      map.set(entityType, new Map());
    }
    if (hasNoValue(map.get(entityType).get(mdRepresentationType))) {
      map.get(entityType).set(mdRepresentationType, new Map());
    }

    if (hasNoValue(map.get(entityType).get(mdRepresentationType).get(context))) {
      map.get(entityType).get(mdRepresentationType).set(context, new Map());
    }

    if (hasValue(map.get(entityType).get(mdRepresentationType).get(context).get(theme))) {
      throw new Error(`There can't be more than one component to render Entity of type "${entityType}" in MetadataRepresentation "${mdRepresentationType}" with context "${context}"`);
    }
    map.get(entityType).get(mdRepresentationType).get(context).set(theme, component);
  };
}

/**
 * Getter to retrieve a matching component by entity type, metadata representation and context
 * @param entityType The entity type to match
 * @param mdRepresentationType The metadata representation to match
 * @param context The context to match
 * @param theme the theme to match
 */
export function getMetadataRepresentationComponent(entityType: string, mdRepresentationType: MetadataRepresentationType, context: Context = DEFAULT_CONTEXT, theme = DEFAULT_THEME) {
  const mapForEntity = map.get(entityType);
  if (hasValue(mapForEntity)) {
    const entityAndMDRepMap = mapForEntity.get(mdRepresentationType);
    if (hasValue(entityAndMDRepMap)) {
      const contextMap = entityAndMDRepMap.get(context);
      if (hasValue(contextMap)) {
        const match = resolveTheme(contextMap, theme);
        if (hasValue(match)) {
          return match;
        }
        if (hasValue(contextMap.get(DEFAULT_THEME))) {
          return contextMap.get(DEFAULT_THEME);
        }
      }
      if (hasValue(entityAndMDRepMap.get(DEFAULT_CONTEXT)) &&
        hasValue(entityAndMDRepMap.get(DEFAULT_CONTEXT).get(DEFAULT_THEME))) {
        return entityAndMDRepMap.get(DEFAULT_CONTEXT).get(DEFAULT_THEME);
      }
    }
    if (hasValue(mapForEntity.get(DEFAULT_REPRESENTATION_TYPE)) &&
      hasValue(mapForEntity.get(DEFAULT_REPRESENTATION_TYPE).get(DEFAULT_CONTEXT)) &&
      hasValue(mapForEntity.get(DEFAULT_REPRESENTATION_TYPE).get(DEFAULT_CONTEXT).get(DEFAULT_THEME))) {
      return mapForEntity.get(DEFAULT_REPRESENTATION_TYPE).get(DEFAULT_CONTEXT).get(DEFAULT_THEME);
    }
  }
  return map.get(DEFAULT_ENTITY_TYPE).get(DEFAULT_REPRESENTATION_TYPE).get(DEFAULT_CONTEXT).get(DEFAULT_THEME);
}
