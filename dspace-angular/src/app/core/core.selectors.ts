import { createFeatureSelector } from '@ngrx/store';
import { CoreState } from './core-state.model';

/**
 * Base selector to select the core state from the store
 */
export const coreSelector = createFeatureSelector<CoreState>('core');
