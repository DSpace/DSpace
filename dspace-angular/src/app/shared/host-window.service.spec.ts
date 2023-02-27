import { Store } from '@ngrx/store';
import { cold, hot } from 'jasmine-marbles';
import { of as observableOf } from 'rxjs';
import { AppState } from '../app.reducer';

import { HostWindowService, WidthCategory } from './host-window.service';
import { CSSVariableServiceStub } from './testing/css-variable-service.stub';

describe('HostWindowService', () => {
  let service: HostWindowService;
  let store: Store<AppState>;

  enum GridBreakpoint {
    SM_MIN = 576,
    MD_MIN = 768,
    LG_MIN = 992,
    XL_MIN = 1200
  }

  describe('', () => {
    beforeEach(() => {
      const _initialState = { hostWindow: { width: 1600, height: 770 } };
      store = new Store<AppState>(observableOf(_initialState), undefined, undefined);
      service = new HostWindowService(store, new CSSVariableServiceStub() as any);
    });

    it('isXs() should return false with width = 1600', () => {
      service.isXs().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });

    it('isSm() should return false with width = 1600', () => {
      service.isSm().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });

    it('isMd() should return false with width = 1600', () => {
      service.isMd().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
    it('isLg() should return false with width = 1600', () => {
      service.isLg().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
    it('isXl() should return true with width = 1600', () => {
      service.isXl().subscribe((status) => {
        expect(status).toBeTruthy();
      });
    });
  });

  describe('', () => {
    beforeEach(() => {
      const _initialState = { hostWindow: { width: 1100, height: 770 } };
      store = new Store<AppState>(observableOf(_initialState), undefined, undefined);
      service = new HostWindowService(store, new CSSVariableServiceStub() as any);
    });

    it('isXs() should return false with width = 1100', () => {
      service.isXs().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });

    it('isSm() should return false with width = 1100', () => {
      service.isSm().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });

    it('isMd() should return false with width = 1100', () => {
      service.isMd().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
    it('isLg() should return true with width = 1100', () => {
      service.isLg().subscribe((status) => {
        expect(status).toBeTruthy();
      });
    });
    it('isXl() should return false with width = 1100', () => {
      service.isXl().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
  });

  describe('', () => {
    beforeEach(() => {
      const _initialState = { hostWindow: { width: 800, height: 770 } };
      store = new Store<AppState>(observableOf(_initialState), undefined, undefined);
      service = new HostWindowService(store, new CSSVariableServiceStub() as any);
    });

    it('isXs() should return false with width = 800', () => {
      service.isXs().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });

    it('isSm() should return false with width = 800', () => {
      service.isSm().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });

    it('isMd() should return true with width = 800', () => {
      service.isMd().subscribe((status) => {
        expect(status).toBeTruthy();
      });
    });
    it('isLg() should return false with width = 800', () => {
      service.isLg().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
    it('isXl() should return false with width = 800', () => {
      service.isXl().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
  });

  describe('', () => {
    beforeEach(() => {
      const _initialState = { hostWindow: { width: 600, height: 770 } };
      store = new Store<AppState>(observableOf(_initialState), undefined, undefined);
      service = new HostWindowService(store, new CSSVariableServiceStub() as any);
    });

    it('isXs() should return false with width = 600', () => {
      service.isXs().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });

    it('isSm() should return true with width = 600', () => {
      service.isSm().subscribe((status) => {
        expect(status).toBeTruthy();
      });
    });

    it('isMd() should return false with width = 600', () => {
      service.isMd().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
    it('isLg() should return false with width = 600', () => {
      service.isLg().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
    it('isXl() should return false with width = 600', () => {
      service.isXl().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
  });

  describe('', () => {
    beforeEach(() => {
      const _initialState = { hostWindow: { width: 400, height: 770 } };
      store = new Store<AppState>(observableOf(_initialState), undefined, undefined);
      service = new HostWindowService(store, new CSSVariableServiceStub() as any);
    });

    it('isXs() should return true with width = 400', () => {
      service.isXs().subscribe((status) => {
        expect(status).toBeTruthy();
      });
    });

    it('isSm() should return false with width = 400', () => {
      service.isSm().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });

    it('isMd() should return false with width = 400', () => {
      service.isMd().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
    it('isLg() should return false with width = 400', () => {
      service.isLg().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
    it('isXl() should return false with width = 400', () => {
      service.isXl().subscribe((status) => {
        expect(status).toBeFalsy();
      });
    });
  });

  describe('widthCategory', () => {
    beforeEach(() => {
      service = new HostWindowService({} as Store<AppState>, new CSSVariableServiceStub() as any);
    });

    it('should call getWithObs to get the current width', () => {
      spyOn(service as any, 'getWidthObs').and
        .returnValue(hot('a-', { a: GridBreakpoint.SM_MIN - 1 }));

      const result = service.widthCategory;

      expect((service as any).getWidthObs).toHaveBeenCalled();
    });

    it('should return XS if width < SM_MIN', () => {
      spyOn(service as any, 'getWidthObs').and
        .returnValue(hot('a-', { a: GridBreakpoint.SM_MIN - 1 }));

      const result = service.widthCategory;

      const expected = cold('b-', { b: WidthCategory.XS });
      expect(result).toBeObservable(expected);
    });

    it('should return SM if SM_MIN <= width < MD_MIN', () => {
      spyOn(service as any, 'getWidthObs').and
        .returnValue(hot('a-', {
          a: GridBreakpoint.SM_MIN + Math.floor((GridBreakpoint.MD_MIN - GridBreakpoint.SM_MIN) / 2)
        }));

      const result = service.widthCategory;

      const expected = cold('b-', { b: WidthCategory.SM });
      expect(result).toBeObservable(expected);
    });

    it('should return MD if MD_MIN <= width < LG_MIN', () => {
      spyOn(service as any, 'getWidthObs').and
        .returnValue(hot('a-', {
          a: GridBreakpoint.MD_MIN + Math.floor((GridBreakpoint.LG_MIN - GridBreakpoint.MD_MIN) / 2)
        }));

      const result = service.widthCategory;

      const expected = cold('b-', { b: WidthCategory.MD });
      expect(result).toBeObservable(expected);
    });

    it('should return LG if LG_MIN <= width < XL_MIN', () => {
      spyOn(service as any, 'getWidthObs').and
        .returnValue(hot('a-', {
          a: GridBreakpoint.LG_MIN + Math.floor((GridBreakpoint.XL_MIN - GridBreakpoint.LG_MIN) / 2)
        }));

      const result = service.widthCategory;

      const expected = cold('b-', { b: WidthCategory.LG });
      expect(result).toBeObservable(expected);
    });

    it('should return XL if width >= XL_MIN', () => {
      spyOn(service as any, 'getWidthObs').and
        .returnValue(hot('a-', { a: GridBreakpoint.XL_MIN + 1 }));

      const result = service.widthCategory;

      const expected = cold('b-', { b: WidthCategory.XL });
      expect(result).toBeObservable(expected);
    });

  });

});
