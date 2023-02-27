import { FeatureID } from './feature-id';

/**
 * Search parameters for retrieving authorizations from the REST API
 */
export class AuthorizationSearchParams {
  objectUrl: string;
  ePersonUuid: string;
  featureId: FeatureID;

  constructor(objectUrl?: string, ePersonUuid?: string, featureId?: FeatureID) {
    this.objectUrl = objectUrl;
    this.ePersonUuid = ePersonUuid;
    this.featureId = featureId;
  }
}
