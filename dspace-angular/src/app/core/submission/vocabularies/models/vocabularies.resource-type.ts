import { ResourceType } from '../../../shared/resource-type';

/**
 * The resource type for vocabulary models
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */

export const VOCABULARY = new ResourceType('vocabulary');
export const VOCABULARY_ENTRY = new ResourceType('vocabularyEntry');
export const VOCABULARY_ENTRY_DETAIL = new ResourceType('vocabularyEntryDetail');
