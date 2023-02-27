import { ResourceType } from '../../../../core/shared/resource-type';

/**
 * The resource type for FacetValues
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const FACET_VALUES = new ResourceType('discovery-facetvalues');
