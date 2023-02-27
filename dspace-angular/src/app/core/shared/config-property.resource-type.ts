import { ResourceType } from './resource-type';

/**
 * The resource type for ConfigurationProperty
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const CONFIG_PROPERTY = new ResourceType('property');
