import { FeatureID } from '../../../core/data/feature-authorization/feature-id';

/**
 *  Represents an item operation used on the edit item page with a key, an operation URL to which will be navigated
 *  when performing the action and an option to disable the operation.
 */
export class ItemOperation {

  operationKey: string;
  operationUrl: string;
  disabled: boolean;
  authorized: boolean;
  featureID: FeatureID;

  constructor(operationKey: string, operationUrl: string, featureID?: FeatureID, disabled = false, authorized = true) {
    this.operationKey = operationKey;
    this.operationUrl = operationUrl;
    this.featureID = featureID;
    this.authorized = authorized;
    this.setDisabled(disabled);
  }

  /**
   * Set whether this operation should be disabled
   * @param disabled
   */
  setDisabled(disabled: boolean): void {
    this.disabled = disabled;
  }

}
