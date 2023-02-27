import { HALLink } from '../shared/hal-link.model';
import { UnCacheableObject } from '../shared/uncacheable-object.model';

/**
 * The response substate in the NgRx store
 */
export class ResponseState {
    timeCompleted: number;
    statusCode: number;
    errorMessage?: string;
    payloadLink?: HALLink;
    unCacheableObject?: UnCacheableObject;
}
