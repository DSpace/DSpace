import { cold, hot } from 'jasmine-marbles';
import { map } from 'rxjs/operators';
import { WidthCategory } from '../host-window.service';
import { ObjectGridComponent } from './object-grid.component';

describe('ObjectGridComponent', () => {
  const testObjects = [
    { one: 1 },
    { two: 2 },
    { three: 3 },
    { four: 4 },
    { five: 5 },
    { six: 6 },
    { seven: 7 },
    { eight: 8 },
    { nine: 9 },
    { ten: 10 }
  ];
  const mockRD = {
    payload: {
      page: testObjects
    }
  } as any;

  describe('the number of columns', () => {

    it('should be 3 for xl screens', () => {
      const hostWindowService = {
        widthCategory: hot('a', { a: WidthCategory.XL }),
      } as any;
      const comp = new ObjectGridComponent(hostWindowService);

      (comp as any)._objects$ = hot('b', { b: mockRD });

      comp.ngOnInit();

      const expected = cold('c', { c: 3 });

      const result = comp.columns$.pipe(
        map((columns) => columns.length)
      );

      expect(result).toBeObservable(expected);
    });

    it('should be 3 for lg screens', () => {
      const hostWindowService = {
        widthCategory: hot('a', { a: WidthCategory.LG }),
      } as any;
      const comp = new ObjectGridComponent(hostWindowService);

      (comp as any)._objects$ = hot('b', { b: mockRD });

      comp.ngOnInit();

      const expected = cold('c', { c: 3 });

      const result = comp.columns$.pipe(
        map((columns) => columns.length)
      );

      expect(result).toBeObservable(expected);
    });

    it('should be 2 for md screens', () => {
      const hostWindowService = {
        widthCategory: hot('a', { a: WidthCategory.MD }),
      } as any;
      const comp = new ObjectGridComponent(hostWindowService);

      (comp as any)._objects$ = hot('b', { b: mockRD });

      comp.ngOnInit();

      const expected = cold('c', { c: 2 });

      const result = comp.columns$.pipe(
        map((columns) => columns.length)
      );

      expect(result).toBeObservable(expected);
    });

    it('should be 2 for sm screens', () => {
      const hostWindowService = {
        widthCategory: hot('a', { a: WidthCategory.SM }),
      } as any;
      const comp = new ObjectGridComponent(hostWindowService);

      (comp as any)._objects$ = hot('b', { b: mockRD });

      comp.ngOnInit();

      const expected = cold('c', { c: 2 });

      const result = comp.columns$.pipe(
        map((columns) => columns.length)
      );

      expect(result).toBeObservable(expected);
    });

    it('should be 1 for xs screens', () => {
      const hostWindowService = {
        widthCategory: hot('a', { a: WidthCategory.XS }),
      } as any;
      const comp = new ObjectGridComponent(hostWindowService);

      (comp as any)._objects$ = hot('b', { b: mockRD });

      comp.ngOnInit();

      const expected = cold('c', { c: 1 });

      const result = comp.columns$.pipe(
        map((columns) => columns.length)
      );

      expect(result).toBeObservable(expected);
    });

  });

  describe('The ordering of the content', () => {
    it('should be left to right for XL screens', () => {
      const hostWindowService = {
        widthCategory: hot('a', { a: WidthCategory.XL }),
      } as any;
      const comp = new ObjectGridComponent(hostWindowService);

      (comp as any)._objects$ = hot('b', { b: mockRD });

      comp.ngOnInit();

      const expected = cold('c', {
        c: [
          [testObjects[0], testObjects[3], testObjects[6], testObjects[9]],
          [testObjects[1], testObjects[4], testObjects[7]],
          [testObjects[2], testObjects[5], testObjects[8]]
        ]
      });

      const result = comp.columns$;

      expect(result).toBeObservable(expected);
    });

    it('should be left to right for LG screens', () => {
      const hostWindowService = {
        widthCategory: hot('a', { a: WidthCategory.LG }),
      } as any;
      const comp = new ObjectGridComponent(hostWindowService);

      (comp as any)._objects$ = hot('b', { b: mockRD });

      comp.ngOnInit();

      const expected = cold('c', {
        c: [
          [testObjects[0], testObjects[3], testObjects[6], testObjects[9]],
          [testObjects[1], testObjects[4], testObjects[7]],
          [testObjects[2], testObjects[5], testObjects[8]]
        ]
      });

      const result = comp.columns$;

      expect(result).toBeObservable(expected);
    });

    it('should be left to right for MD screens', () => {
      const hostWindowService = {
        widthCategory: hot('a', { a: WidthCategory.MD }),
      } as any;
      const comp = new ObjectGridComponent(hostWindowService);

      (comp as any)._objects$ = hot('b', { b: mockRD });

      comp.ngOnInit();

      const expected = cold('c', {
        c: [
          [testObjects[0], testObjects[2], testObjects[4], testObjects[6], testObjects[8]],
          [testObjects[1], testObjects[3], testObjects[5], testObjects[7], testObjects[9]],
        ]
      });

      const result = comp.columns$;

      expect(result).toBeObservable(expected);
    });

    it('should be left to right for SM screens', () => {
      const hostWindowService = {
        widthCategory: hot('a', { a: WidthCategory.SM }),
      } as any;
      const comp = new ObjectGridComponent(hostWindowService);

      (comp as any)._objects$ = hot('b', { b: mockRD });

      comp.ngOnInit();

      const expected = cold('c', {
        c: [
          [testObjects[0], testObjects[2], testObjects[4], testObjects[6], testObjects[8]],
          [testObjects[1], testObjects[3], testObjects[5], testObjects[7], testObjects[9]],
        ]
      });

      const result = comp.columns$;

      expect(result).toBeObservable(expected);
    });

    it('should be top to bottom for XS screens', () => {
      const hostWindowService = {
        widthCategory: hot('a', { a: WidthCategory.XS }),
      } as any;
      const comp = new ObjectGridComponent(hostWindowService);

      (comp as any)._objects$ = hot('b', { b: mockRD });

      comp.ngOnInit();

      const expected = cold('c', { c: [testObjects] });

      const result = comp.columns$;

      expect(result).toBeObservable(expected);
    });
  });
});
