import { Component, Input } from '@angular/core';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { hasValue } from '../../empty.util';
import { Item } from 'src/app/core/shared/item.model';
import { IdentifierData } from './identifier-data.model';
import { IdentifierDataService } from '../../../core/data/identifier-data.service';

@Component({
  selector: 'ds-identifier-data',
  templateUrl: './identifier-data.component.html'
})
/**
 * Component rendering an identifier, eg. DOI or handle
 */
export class IdentifierDataComponent {

  @Input() item: Item;
  identifiers$: Observable<IdentifierData>;

  /**
   * Initialize instance variables
   *
   * @param {IdentifierDataService} identifierDataService
   */
  constructor(private identifierDataService: IdentifierDataService) { }

  ngOnInit(): void {
    if (this.item == null) {
      // Do not show the identifier if the feature is inactive or if the item is null.
      return;
    }
    if (this.item.identifiers == null) {
      // In case the identifier has not been loaded, do it individually.
      this.item.identifiers = this.identifierDataService.getIdentifierDataFor(this.item);
    }
    this.identifiers$ = this.item.identifiers.pipe(
      map((identifierRD) => {
        if (identifierRD.statusCode !== 401 && hasValue(identifierRD.payload)) {
          return identifierRD.payload;
        } else {
          return null;
        }
      }),
    );
  }
}
