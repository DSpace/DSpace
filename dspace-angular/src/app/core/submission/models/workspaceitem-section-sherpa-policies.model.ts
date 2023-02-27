import { SherpaPoliciesDetailsObject } from './sherpa-policies-details.model';

/**
 * An interface to represent the submission's item accesses condition.
 */
export interface WorkspaceitemSectionSherpaPoliciesObject {

  /**
   * The access condition id
   */
  id: string;

  /**
   * The sherpa policies retrievalTime
   */
  retrievalTime: string;

  /**
   * The sherpa policies details
   */
  sherpaResponse: SherpaPoliciesDetailsObject;
}
