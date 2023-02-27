import { FilterType } from '../../models/filter-type.model';

/**
 * Contains the mapping between a facet component and a FilterType
 */
const filterTypeMap = new Map();

/**
 * Sets the mapping for a facet component in relation to a filter type
 * @param {FilterType} type The type for which the matching component is mapped
 * @returns Decorator function that performs the actual mapping on initialization of the facet component
 */
export function renderFacetFor(type: FilterType) {
  return function decorator(objectElement: any) {
    if (!objectElement) {
      return;
    }
    filterTypeMap.set(type, objectElement);
  };
}

/**
 * Requests the matching facet component based on a given filter type
 * @param {FilterType} type The filter type for which the facet component is requested
 * @returns The facet component's constructor that matches the given filter type
 */
export function renderFilterType(type: FilterType) {
  return filterTypeMap.get(type);
}
