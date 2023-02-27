import { TestBed } from '@angular/core/testing';

import { BreadcrumbsService } from './breadcrumbs.service';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { Observable, of as observableOf, Subject } from 'rxjs';
import { BreadcrumbConfig } from './breadcrumb/breadcrumb-config.model';
import { BreadcrumbsProviderService } from '../core/breadcrumbs/breadcrumbsProviderService';
import { Breadcrumb } from './breadcrumb/breadcrumb.model';
import { cold } from 'jasmine-marbles';

class TestBreadcrumbsService implements BreadcrumbsProviderService<string> {
  getBreadcrumbs(key: string, url: string): Observable<Breadcrumb[]> {
    return observableOf([new Breadcrumb(key, url)]);
  }
}

describe('BreadcrumbsService', () => {
  let service: BreadcrumbsService;
  let routerEventsObs: Subject<any>;
  let routerMock: Router;
  let activatedRouteMock: Partial<ActivatedRoute>;
  let currentRootRoute: Partial<ActivatedRoute>;
  let breadcrumbProvider;
  let breadcrumbConfigA: BreadcrumbConfig<string>;
  let breadcrumbConfigB: BreadcrumbConfig<string>;

  /**
   * Init breadcrumb variables, see beforeEach
   */
  const initBreadcrumbs = () => {
    breadcrumbProvider = new TestBreadcrumbsService();
    breadcrumbConfigA = { provider: breadcrumbProvider, key: 'example.path', url: 'example.com' };
    breadcrumbConfigB = { provider: breadcrumbProvider, key: 'another.path', url: 'another.com' };
  };

  const changeActivatedRoute = (newRootRoute: any) => {
    // update the ActivatedRoute that the service will receive
    currentRootRoute = newRootRoute;

    // the pipeline of BreadcrumbsService#listenForRouteChanges needs a NavigationEnd event,
    // but the actual payload does not matter, since ActivatedRoute is mocked too.
    routerEventsObs.next(new NavigationEnd(0, '', ''));
  };

  beforeEach(() => {
    initBreadcrumbs();

    routerEventsObs = new Subject<any>();

    // BreadcrumbsService uses Router#events
    routerMock = jasmine.createSpyObj([], {
      events: routerEventsObs,
    });

    // BreadcrumbsService uses ActivatedRoute#root
    activatedRouteMock = {
      get root() {
        return currentRootRoute as ActivatedRoute;
      },
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: activatedRouteMock },
      ],
    });
    service = TestBed.inject(BreadcrumbsService);

    // this is done by AppComponent under regular circumstances
    service.listenForRouteChanges();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('breadcrumbs$', () => {
    it('should return a breadcrumb corresponding to the current route', () => {
      const route1 = {
        snapshot: {
          data: { breadcrumb: breadcrumbConfigA },
          routeConfig: { resolve: { breadcrumb: {} } }
        }
      };

      const expectation1 = [
        new Breadcrumb(breadcrumbConfigA.key, breadcrumbConfigA.url),
      ];

      changeActivatedRoute(route1);
      expect(service.breadcrumbs$).toBeObservable(cold('a', { a: expectation1 }));

      const route2 = {
        snapshot: {
          data: { breadcrumb: breadcrumbConfigA },
          routeConfig: { resolve: { breadcrumb: {} } }
        },
        firstChild: {
          snapshot: {
            // Example without resolver should be ignored
            data: { breadcrumb: breadcrumbConfigA },
          },
          firstChild: {
            snapshot: {
              data: { breadcrumb: breadcrumbConfigB },
              routeConfig: { resolve: { breadcrumb: {} } }
            }
          }
        }
      };

      const expectation2 = [
        new Breadcrumb(breadcrumbConfigA.key, breadcrumbConfigA.url),
        new Breadcrumb(breadcrumbConfigB.key, breadcrumbConfigB.url),
      ];

      changeActivatedRoute(route2);
      expect(service.breadcrumbs$).toBeObservable(cold('a', { a: expectation2 }));
    });
  });

  describe('showBreadcrumbs$', () => {
    describe('when the last part of the route has showBreadcrumbs in its data', () => {
      it('should return that value', () => {
        const route1 = {
          snapshot: {
            data: {
              breadcrumb: breadcrumbConfigA,
              showBreadcrumbs: false, // explicitly hide breadcrumbs
            },
            routeConfig: { resolve: { breadcrumb: {} } }
          }
        };

        changeActivatedRoute(route1);
        expect(service.showBreadcrumbs$).toBeObservable(cold('a', { a: false }));

        const route2 = {
          snapshot: {
            data: {
              breadcrumb: breadcrumbConfigA,
              showBreadcrumbs: true, // explicitly show breadcrumbs
            },
            routeConfig: { resolve: { breadcrumb: {} } }
          }
        };

        changeActivatedRoute(route2);
        expect(service.showBreadcrumbs$).toBeObservable(cold('a', { a: true }));
      });
    });

    describe('when the last part of the route has no breadcrumb in its data', () => {
      it('should return false', () => {
        const route1 = {
          snapshot: {
            data: {
              // no breadcrumbs set - always hide
            },
            routeConfig: { resolve: { breadcrumb: {} } }
          }
        };

        changeActivatedRoute(route1);
        expect(service.showBreadcrumbs$).toBeObservable(cold('a', { a: false }));
      });
    });
  });

});
