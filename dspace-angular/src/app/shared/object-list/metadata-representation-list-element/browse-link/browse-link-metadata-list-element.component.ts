import { MetadataRepresentationType } from '../../../../core/shared/metadata-representation/metadata-representation.model';
import { Component } from '@angular/core';
import { MetadataRepresentationListElementComponent } from '../metadata-representation-list-element.component';
import { metadataRepresentationComponent } from '../../../metadata-representation/metadata-representation.decorator';
//@metadataRepresentationComponent('Publication', MetadataRepresentationType.PlainText)
// For now, authority controlled fields are rendered the same way as plain text fields
//@metadataRepresentationComponent('Publication', MetadataRepresentationType.AuthorityControlled)
@metadataRepresentationComponent('Publication', MetadataRepresentationType.BrowseLink)
@Component({
  selector: 'ds-browse-link-metadata-list-element',
  templateUrl: './browse-link-metadata-list-element.component.html'
})
/**
 * A component for displaying MetadataRepresentation objects in the form of plain text
 * It will simply use the value retrieved from MetadataRepresentation.getValue() to display as plain text
 */
export class BrowseLinkMetadataListElementComponent extends MetadataRepresentationListElementComponent {
  /**
   * Get the appropriate query parameters for this browse link, depending on whether the browse definition
   * expects 'startsWith' (eg browse by date) or 'value' (eg browse by title)
   */
  getQueryParams() {
    let queryParams = {startsWith: this.metadataRepresentation.getValue()};
    if (this.metadataRepresentation.browseDefinition.metadataBrowse) {
      return {value: this.metadataRepresentation.getValue()};
    }
    return queryParams;
  }
}
