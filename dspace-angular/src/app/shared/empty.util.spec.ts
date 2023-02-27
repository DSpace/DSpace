import { cold, hot } from 'jasmine-marbles';
import {
  ensureArrayHasValue,
  hasNoValue,
  hasValue,
  hasValueOperator,
  isEmpty,
  isNotEmpty,
  isNotEmptyOperator,
  isNotNull,
  isNotUndefined,
  isNull, isObjectEmpty,
  isUndefined
} from './empty.util';

describe('Empty Utils', () => {
  const strng = 'string';
  const fn: () => void = () => {
    console.info();
  };
  const object: any = { length: 0 };
  const emptyMap: Map<any, any> = new Map();
  const fullMap: Map<string, string> = new Map();
  fullMap.set('foo', 'bar');

  describe('isNull', () => {
    it('should return true for null', () => {
      expect(isNull(null)).toBe(true);
    });

    it('should return false for undefined', () => {
      expect(isNull(undefined)).toBe(false);
    });

    it('should return false for an empty String', () => {
      expect(isNull('')).toBe(false);
    });

    it('should return false for true', () => {
      expect(isNull(true)).toBe(false);
    });

    it('should return false for false', () => {
      expect(isNull(false)).toBe(false);
    });

    it('should return false for a String', () => {
      expect(isNull(strng)).toBe(false);
    });

    it('should return false for a Function', () => {
      expect(isNull(fn)).toBe(false);
    });

    it('should return false for 0', () => {
      expect(isNull(0)).toBe(false);
    });

    it('should return false for an empty Array', () => {
      expect(isNull([])).toBe(false);
    });

    it('should return false for an empty Object', () => {
      expect(isNull({})).toBe(false);
    });

  });

  describe('isNotNull', () => {

    it('should return false for null', () => {
      expect(isNotNull(null)).toBe(false);
    });

    it('should return true for undefined', () => {
      expect(isNotNull(undefined)).toBe(true);
    });

    it('should return true for an empty String', () => {
      expect(isNotNull('')).toBe(true);
    });

    it('should return true for false', () => {
      expect(isNotNull(false)).toBe(true);
    });

    it('should return true for true', () => {
      expect(isNotNull(true)).toBe(true);
    });

    it('should return true for a String', () => {
      expect(isNotNull(strng)).toBe(true);
    });

    it('should return true for a Function', () => {
      expect(isNotNull(fn)).toBe(true);
    });

    it('should return true for 0', () => {
      expect(isNotNull(0)).toBe(true);
    });

    it('should return true for an empty Array', () => {
      expect(isNotNull([])).toBe(true);
    });

    it('should return true for an empty Object', () => {
      expect(isNotNull({})).toBe(true);
    });

  });

  describe('isUndefined', () => {
    it('should return false for null', () => {
      expect(isUndefined(null)).toBe(false);
    });

    it('should return true for undefined', () => {
      expect(isUndefined(undefined)).toBe(true);
    });

    it('should return false for an empty String', () => {
      expect(isUndefined('')).toBe(false);
    });

    it('should return false for true', () => {
      expect(isUndefined(true)).toBe(false);
    });

    it('should return false for false', () => {
      expect(isUndefined(false)).toBe(false);
    });

    it('should return false for a String', () => {
      expect(isUndefined(strng)).toBe(false);
    });

    it('should return false for a Function', () => {
      expect(isUndefined(fn)).toBe(false);
    });

    it('should return false for 0', () => {
      expect(isUndefined(0)).toBe(false);
    });

    it('should return false for an empty Array', () => {
      expect(isUndefined([])).toBe(false);
    });

    it('should return false for an empty Object', () => {
      expect(isUndefined({})).toBe(false);
    });

  });

  describe('isNotUndefined', () => {

    it('should return true for null', () => {
      expect(isNotUndefined(null)).toBe(true);
    });

    it('should return false for undefined', () => {
      expect(isNotUndefined(undefined)).toBe(false);
    });

    it('should return true for an empty String', () => {
      expect(isNotUndefined('')).toBe(true);
    });

    it('should return true for false', () => {
      expect(isNotUndefined(false)).toBe(true);
    });

    it('should return true for true', () => {
      expect(isNotUndefined(true)).toBe(true);
    });

    it('should return true for a String', () => {
      expect(isNotUndefined(strng)).toBe(true);
    });

    it('should return true for a Function', () => {
      expect(isNotUndefined(fn)).toBe(true);
    });

    it('should return true for 0', () => {
      expect(isNotUndefined(0)).toBe(true);
    });

    it('should return true for an empty Array', () => {
      expect(isNotUndefined([])).toBe(true);
    });

    it('should return true for an empty Object', () => {
      expect(isNotUndefined({})).toBe(true);
    });

  });

  describe('hasNoValue', () => {
    it('should return true for null', () => {
      expect(hasNoValue(null)).toBe(true);
    });

    it('should return true for undefined', () => {
      expect(hasNoValue(undefined)).toBe(true);
    });

    it('should return false for an empty String', () => {
      expect(hasNoValue('')).toBe(false);
    });

    it('should return false for true', () => {
      expect(hasNoValue(true)).toBe(false);
    });

    it('should return false for false', () => {
      expect(hasNoValue(false)).toBe(false);
    });

    it('should return false for a String', () => {
      expect(hasNoValue(strng)).toBe(false);
    });

    it('should return false for a Function', () => {
      expect(hasNoValue(fn)).toBe(false);
    });

    it('should return false for 0', () => {
      expect(hasNoValue(0)).toBe(false);
    });

    it('should return false for an empty Array', () => {
      expect(hasNoValue([])).toBe(false);
    });

    it('should return false for an empty Object', () => {
      expect(hasNoValue({})).toBe(false);
    });

  });

  describe('hasValue', () => {

    it('should return false for null', () => {
      expect(hasValue(null)).toBe(false);
    });

    it('should return false for undefined', () => {
      expect(hasValue(undefined)).toBe(false);
    });

    it('should return true for an empty String', () => {
      expect(hasValue('')).toBe(true);
    });

    it('should return true for false', () => {
      expect(hasValue(false)).toBe(true);
    });

    it('should return true for true', () => {
      expect(hasValue(true)).toBe(true);
    });

    it('should return true for a String', () => {
      expect(hasValue(strng)).toBe(true);
    });

    it('should return true for a Function', () => {
      expect(hasValue(fn)).toBe(true);
    });

    it('should return true for 0', () => {
      expect(hasValue(0)).toBe(true);
    });

    it('should return true for an empty Array', () => {
      expect(hasValue([])).toBe(true);
    });

    it('should return true for an empty Object', () => {
      expect(hasValue({})).toBe(true);
    });

  });

  describe('hasValueOperator', () => {
    it('should only include items from the source observable for which hasValue is true, and omit all others', () => {
      const testData = {
        a: null,
        b: 'test',
        c: true,
        d: undefined,
        e: 1,
        f: {}
      };

      const source$ = hot('abcdef', testData);
      const expected$ = cold('-bc-ef', testData);
      const result$ = source$.pipe(hasValueOperator());

      expect(result$).toBeObservable(expected$);
    });
  });

  describe('isEmpty', () => {
    it('should return true for null', () => {
      expect(isEmpty(null)).toBe(true);
    });

    it('should return true for undefined', () => {
      expect(isEmpty(undefined)).toBe(true);
    });

    it('should return true for an empty String', () => {
      expect(isEmpty('')).toBe(true);
    });

    it('should return false for a whitespace String', () => {
      expect(isEmpty('  ')).toBe(false);
      expect(isEmpty('\n\t')).toBe(false);
    });

    it('should return false for true', () => {
      expect(isEmpty(true)).toBe(false);
    });

    it('should return false for false', () => {
      expect(isEmpty(false)).toBe(false);
    });

    it('should return false for a String', () => {
      expect(isEmpty(strng)).toBe(false);
    });

    it('should return false for a Function', () => {
      expect(isEmpty(fn)).toBe(false);
    });

    it('should return false for 0', () => {
      expect(isEmpty(0)).toBe(false);
    });

    it('should return true for an empty Array', () => {
      expect(isEmpty([])).toBe(true);
    });

    it('should return false for an empty Object', () => {
      expect(isEmpty({})).toBe(true);
    });

    it('should return true for an Object that has zero \'length\'', () => {
      expect(isEmpty(object)).toBe(true);
    });

    it('should return true for an Empty map', () => {
      expect(isEmpty(emptyMap)).toBe(true);
    });

    it('should return false for a Map that is not empty', () => {
      expect(isEmpty(fullMap)).toBe(false);
    });

  });

  describe('isNotEmpty', () => {
    it('should return false for null', () => {
      expect(isNotEmpty(null)).toBe(false);
    });

    it('should return false for undefined', () => {
      expect(isNotEmpty(undefined)).toBe(false);
    });

    it('should return false for an empty String', () => {
      expect(isNotEmpty('')).toBe(false);
    });

    it('should return true for a whitespace String', () => {
      expect(isNotEmpty('  ')).toBe(true);
      expect(isNotEmpty('\n\t')).toBe(true);
    });

    it('should return true for false', () => {
      expect(isNotEmpty(false)).toBe(true);
    });

    it('should return true for true', () => {
      expect(isNotEmpty(true)).toBe(true);
    });

    it('should return true for a String', () => {
      expect(isNotEmpty(strng)).toBe(true);
    });

    it('should return true for a Function', () => {
      expect(isNotEmpty(fn)).toBe(true);
    });

    it('should return true for 0', () => {
      expect(isNotEmpty(0)).toBe(true);
    });

    it('should return false for an empty Array', () => {
      expect(isNotEmpty([])).toBe(false);
    });

    it('should return true for an empty Object', () => {
      expect(isNotEmpty({})).toBe(false);
    });

    it('should return false for an Object that has zero length', () => {
      expect(isNotEmpty(object)).toBe(false);
    });

    it('should return false for an Empty map', () => {
      expect(isNotEmpty(emptyMap)).toBe(false);
    });

    it('should return true for a Map that is not empty', () => {
      expect(isNotEmpty(fullMap)).toBe(true);
    });

  });

  describe('isNotEmptyOperator', () => {
    it('should only include items from the source observable for which isNotEmpty is true, and omit all others', () => {
      const testData = {
        a: null,
        b: 'test',
        c: true,
        d: undefined,
        e: 1,
        f: {},
        g: '',
        h: ' '
      };

      const source$ = hot('abcdefgh', testData);
      const expected$ = cold('-bc-e--h', testData);
      const result$ = source$.pipe(isNotEmptyOperator());

      expect(result$).toBeObservable(expected$);
    });
  });

  describe('isObjectEmpty', () => {
    /*
    isObjectEmpty();                // true
    isObjectEmpty(null);            // true
    isObjectEmpty(undefined);       // true
    isObjectEmpty('');              // true
    isObjectEmpty([]);              // true
    isObjectEmpty({});              // true
    isObjectEmpty({name: null});    // true
    isObjectEmpty({ name: 'Adam Hawkins', surname : null});  // false
    */
    it('should be empty if no parameter passed', () => {
      expect(isObjectEmpty()).toBeTrue();
    });
    it('should be empty if null parameter passed', () => {
      expect(isObjectEmpty(null)).toBeTrue();
    });
    it('should be empty if undefined parameter passed', () => {
      expect(isObjectEmpty(undefined)).toBeTrue();
    });
    it('should be empty if empty string passed', () => {
      expect(isObjectEmpty('')).toBeTrue();
    });
    it('should be empty if empty array passed', () => {
      expect(isObjectEmpty([])).toBeTrue();
    });
    it('should be empty if empty object passed', () => {
      expect(isObjectEmpty({})).toBeTrue();
    });
    it('should be empty if single key with null value passed', () => {
      expect(isObjectEmpty({ name: null })).toBeTrue();
    });
    it('should NOT be empty if object with at least one non-null value passed', () => {
      expect(isObjectEmpty({ name: 'Adam Hawkins', surname : null })).toBeFalse();
    });
  });

  describe('ensureArrayHasValue', () => {
    it('should let all arrays pass unchanged, and turn everything else in to empty arrays', () => {
      const sourceData = {
        a: { a: 'b' },
        b: ['a', 'b', 'c'],
        c: null,
        d: [1],
        e: undefined,
        f: [],
        g: () => true,
        h: {},
        i: ''
      };

      const expectedData = Object.assign({}, sourceData, {
        a: [],
        c: [],
        e: [],
        g: [],
        h: [],
        i: []
      });

      const source$ = hot('abcdefghi', sourceData);
      const expected$ = cold('abcdefghi', expectedData);
      const result$ = source$.pipe(ensureArrayHasValue());

      expect(result$).toBeObservable(expected$);
    });
  });
});
