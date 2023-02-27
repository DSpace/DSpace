/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class DragService {
  private _overrideDragOverPage = false;

  public overrideDragOverPage() {
    this._overrideDragOverPage = true;
  }

  public allowDragOverPage() {
    this._overrideDragOverPage = false;
  }

  public isAllowedDragOverPage(): boolean {
    return !this._overrideDragOverPage;
  }
}
