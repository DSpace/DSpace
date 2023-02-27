import { RequestEntry } from './request-entry.model';

/**
 * The request sub-state of the NgRx store
 */
export interface RequestState {
    [uuid: string]: RequestEntry;
}
