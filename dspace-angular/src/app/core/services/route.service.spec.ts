import { ActivatedRoute, convertToParamMap, NavigationEnd, Params, Router } from '@angular/router';
import { TestBed, waitForAsync } from '@angular/core/testing';

import { of as observableOf } from 'rxjs';
import { Store } from '@ngrx/store';
import { getTestScheduler, hot } from 'jasmine-marbles';

import { RouteService } from './route.service';
import { RouterMock } from '../../shared/mocks/router.mock';
import { TestScheduler } from 'rxjs/testing';
import { AddUrlToHistoryAction } from '../history/history.actions';

describe('RouteService', () => {
  let scheduler: TestScheduler;
  let service: RouteService;
  let serviceAsAny: any;
  const paramName1 = 'name';
  const paramValue1 = 'Test Name';
  const paramName2 = 'id';
  const paramValue2a = 'Test id';
  const paramValue2b = 'another id';
  const nonExistingParamName = 'non existing name';
  const nonExistingParamValue = 'non existing value';

  const paramObject: Params = {};

  const store: any = jasmine.createSpyObj('store', {
    dispatch: jasmine.createSpy('dispatch'),
    select: jasmine.createSpy('select')
  });

  const router = new RouterMock();
  router.setParams(convertToParamMap(paramObject));

  paramObject[paramName1] = paramValue1;
  paramObject[paramName2] = [paramValue2a, paramValue2b];

  beforeEach(waitForAsync(() => {
    return TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: observableOf(paramObject),
            params: observableOf(paramObject),
            queryParamMap: observableOf(convertToParamMap(paramObject))
          },
        },
        { provide: Router, useValue: router },
        { provide: Store, useValue: store },
      ]
    });
  }));

  beforeEach(() => {
    service = new RouteService(TestBed.inject(ActivatedRoute), TestBed.inject(Router), TestBed.inject(Store));
    serviceAsAny = service;
  });

  describe('hasQueryParam', () => {
    it('should return true when the parameter name exists', () => {
      service.hasQueryParam(paramName1).subscribe((status) => {
        expect(status).toBeTruthy();
      });
    });
    it('should return false when the parameter name does not exists', () => {
      service.hasQueryParam(nonExistingParamName).subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
  });

  describe('hasQueryParamWithValue', () => {
    it('should return true when the parameter name exists and contains the specified value', () => {
      service.hasQueryParamWithValue(paramName2, paramValue2a).subscribe((status) => {
        expect(status).toBeTruthy();
      });
    });
    it('should return false when the parameter name exists and does not contain the specified value', () => {
      service.hasQueryParamWithValue(paramName1, nonExistingParamValue).subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
    it('should return false when the parameter name does not exists', () => {
      service.hasQueryParamWithValue(nonExistingParamName, nonExistingParamValue).subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
  });

  describe('getQueryParameterValues', () => {
    it('should return a list of values when the parameter exists', () => {
      service.getQueryParameterValues(paramName2).subscribe((params) => {
        expect(params).toEqual([paramValue2a, paramValue2b]);
      });
    });

    it('should return an empty array when the parameter does not exists', () => {
      service.getQueryParameterValues(nonExistingParamName).subscribe((params) => {
        expect(params).toEqual([]);
      });
    });
  });

  describe('getQueryParameterValue', () => {
    it('should return a single value when the parameter exists', () => {
      service.getQueryParameterValue(paramName1).subscribe((params) => {
        expect(params).toEqual(paramValue1);
      });
    });

    it('should return only the first value when the parameter exists', () => {
      service.getQueryParameterValue(paramName2).subscribe((params) => {
        expect(params).toEqual(paramValue2a);
      });
    });

    it('should return undefined when the parameter exists', () => {
      service.getQueryParameterValue(nonExistingParamName).subscribe((params) => {
        expect(params).toBeNull();
      });
    });
  });

  describe('saveRouting', () => {

    it('should dispatch AddUrlToHistoryAction on NavigationEnd event', () => {
      scheduler = getTestScheduler();

      serviceAsAny.router.events = hot('a-b', {
        a: new NavigationEnd(0, 'url', 'url'),
        b: new NavigationEnd(1, 'newurl', 'newurl')
      });

      scheduler.schedule(() => service.saveRouting());
      scheduler.flush();

      expect(serviceAsAny.store.dispatch).toHaveBeenCalledWith(new AddUrlToHistoryAction('url'));
      expect(serviceAsAny.store.dispatch).toHaveBeenCalledWith(new AddUrlToHistoryAction('newurl'));
    });
  });

  describe('getHistory', () => {
    it('should dispatch AddUrlToHistoryAction on NavigationEnd event', () => {
      serviceAsAny.store = observableOf({
        core: {
          history: ['url', 'newurl']
        }
      });

      service.getHistory().subscribe((history) => {
        expect(history).toEqual(['url', 'newurl']);
      });
    });
  });

  describe('getCurrentUrl', () => {
    it('should return an observable with the current url', () => {
      serviceAsAny.store = observableOf({
        core: {
          history: ['url', 'newurl']
        }
      });

      service.getCurrentUrl().subscribe((history) => {
        expect(history).toEqual('newurl');
      });
    });
  });

  describe('getCurrentUrl', () => {
    it('should return an observable with the previous url', () => {
      serviceAsAny.store = observableOf({
        core: {
          history: ['url', 'newurl']
        }
      });

      service.getPreviousUrl().subscribe((history) => {
        expect(history).toEqual('url');
      });
    });
  });
});
