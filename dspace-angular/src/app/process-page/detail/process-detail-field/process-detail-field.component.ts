import { Component, Input } from '@angular/core';

@Component({
  selector: 'ds-process-detail-field',
  templateUrl: './process-detail-field.component.html',
})
/**
 * A component displaying a single detail about a DSpace Process
 */
export class ProcessDetailFieldComponent {
  /**
   * I18n message for the header
   */
  @Input() title: string;
}
