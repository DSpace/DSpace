import { of as observableOf } from 'rxjs';
export class RouterStub {
  url: string;
  routeReuseStrategy = {shouldReuseRoute: {}};
  //noinspection TypeScriptUnresolvedFunction
  navigate = jasmine.createSpy('navigate');
  parseUrl = jasmine.createSpy('parseUrl');
  events = observableOf({});
  navigateByUrl(url): void {
    this.url = url;
  }
  createUrlTree(commands, navigationExtras = {}) {
    return '/testing-url';
  }
  serializeUrl(commands, navExtras = {}) {
    return '/testing-url';
  }
}
