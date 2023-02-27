import { Component } from '@angular/core';
import { MetadataRepresentation } from '../../../core/shared/metadata-representation/metadata-representation.model';

@Component({
  selector: 'ds-metadata-representation-list-element',
  template: ''
})
/**
 * An abstract class for displaying a single MetadataRepresentation
 */
export class MetadataRepresentationListElementComponent {
  /**
   * The metadata representation of this component
   */
  metadataRepresentation: MetadataRepresentation;

  /**
   * Returns true if this component's value matches a basic regex "Is this an HTTP URL" test
   */
  isLink(): boolean {
    // Match any string that begins with http:// or https://
    const linkPattern = new RegExp(/^https?\/\/.*/);
    return linkPattern.test(this.metadataRepresentation.getValue());
  }

}
