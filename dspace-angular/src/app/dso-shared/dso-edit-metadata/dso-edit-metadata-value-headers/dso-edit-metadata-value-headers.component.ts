import { Component, Input } from '@angular/core';

@Component({
  selector: 'ds-dso-edit-metadata-value-headers',
  styleUrls: ['./dso-edit-metadata-value-headers.component.scss', '../dso-edit-metadata-shared/dso-edit-metadata-cells.scss'],
  templateUrl: './dso-edit-metadata-value-headers.component.html',
})
/**
 * Component displaying invisible headers for a list of metadata values using table roles for accessibility
 */
export class DsoEditMetadataValueHeadersComponent {
  /**
   * Type of DSO we're displaying values for
   * Determines i18n messages
   */
  @Input() dsoType: string;
}
