import { Directive, Host } from '@angular/core';
import { NgForOf } from '@angular/common';

import { DSpaceObject } from '../core/shared/dspace-object.model';

@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: '[ngForTrackById]',
})
export class NgForTrackByIdDirective<T extends DSpaceObject> {

  constructor(@Host() private ngFor: NgForOf<T>) {
    this.ngFor.ngForTrackBy = (index: number, dso: T) => (dso) ? dso.id : undefined;
  }

}
