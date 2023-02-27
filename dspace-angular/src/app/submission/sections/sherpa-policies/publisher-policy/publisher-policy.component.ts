import { Component, Input } from '@angular/core';

import { Policy } from '../../../../core/submission/models/sherpa-policies-details.model';
import { AlertType } from '../../../../shared/alert/aletr-type';

/**
 * This component represents a section that contains the publisher policy informations.
 */
@Component({
  selector: 'ds-publisher-policy',
  templateUrl: './publisher-policy.component.html',
  styleUrls: ['./publisher-policy.component.scss']
})
export class PublisherPolicyComponent {

  /**
   * Policy to show information from
   */
  @Input() policy: Policy;


  /**
   * The AlertType enumeration
   * @type {AlertType}
   */
  public AlertTypeEnum = AlertType;

}
