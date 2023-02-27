/**
 * The resource type for SystemWideAlert
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */

import { ResourceType } from '../core/shared/resource-type';

export const SYSTEMWIDEALERT = new ResourceType('systemwidealert');
