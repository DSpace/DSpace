import { Observable, of } from 'rxjs';

export class ObjectSelectServiceStub {

  ids: string[] = [];

  constructor(ids?: string[]) {
    if (ids) {
      this.ids = ids;
    }
  }

  getSelected(id: string): Observable<boolean> {
    if (this.ids.indexOf(id) > -1) {
      return of(true);
    } else {
      return of(false);
    }
  }

  getAllSelected(): Observable<string[]> {
    return of(this.ids);
  }

  switch(id: string) {
    const index = this.ids.indexOf(id);
    if (index > -1) {
      this.ids.splice(index, 1);
    } else {
      this.ids.push(id);
    }
  }

  reset() {
    this.ids = [];
  }
}
