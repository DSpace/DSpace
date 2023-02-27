import { Component, Input } from '@angular/core';

import { Metadata } from '../../../../core/submission/models/sherpa-policies-details.model';

/**
 * This component represents a section that contains the matadata informations.
 */
@Component({
  selector: 'ds-metadata-information',
  templateUrl: './metadata-information.component.html',
  styleUrls: ['./metadata-information.component.scss']
})
export class MetadataInformationComponent {
  /**
   * Metadata to show information from
   */
  @Input() metadata: Metadata;

}
