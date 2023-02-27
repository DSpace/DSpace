import {of as observableOf,  Observable } from 'rxjs';

// declare a stub service
export class HostWindowServiceMock {

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

  isSm(): Observable<boolean> {
    return observableOf(this.width < 768);
  }
}
