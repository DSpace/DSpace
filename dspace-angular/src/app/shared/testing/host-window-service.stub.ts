import {of as observableOf,  Observable } from 'rxjs';

// declare a stub service
export class HostWindowServiceStub {

  private width: number;

  constructor(width) {
    this.setWidth(width);
  }

  setWidth(width) {
    this.width = width;
  }

  isXs(): Observable<boolean> {
    return observableOf(this.width < 576);
  }

  isXsOrSm(): Observable<boolean> {
    return this.isXs();
  }
}
