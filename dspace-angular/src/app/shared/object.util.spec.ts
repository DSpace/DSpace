import { deleteProperty, difference, hasOnlyEmptyProperties } from './object.util';

describe('Object Utils', () => {
  let object: any = {};
  let anotherObject: any = {};
  let objectExpected: any = {};

  describe('deleteProperty', () => {
    it('should return object without property \'a\'', () => {
      object = { a: 'a', b: 'b' };
      objectExpected = { b: 'b' };
      expect(deleteProperty(object, 'a')).toEqual(objectExpected);
    });

    it('should return same object', () => {
      object = { a: 'a', b: 'b' };
      expect(deleteProperty(object, 'c')).toEqual(object);
    });

  });

  describe('hasOnlyEmptyProperties', () => {

    it('should return true when object is empty', () => {
      object = {};
      expect(hasOnlyEmptyProperties(object)).toBe(true);
    });

    it('should return true when object has a null property', () => {
      object = { a: null };
      expect(hasOnlyEmptyProperties(object)).toBe(true);
    });

    it('should return true when object property has an empty array as value', () => {
      object = { a: [] };
      expect(hasOnlyEmptyProperties(object)).toBe(true);
    });

    it('should return true when object property has an empty object as value', () => {
      object = { a: {} };
      expect(hasOnlyEmptyProperties(object)).toBe(true);
    });

    it('should return false when object is not empty', () => {
      object = { a: 'a', b: 'b' };
      expect(hasOnlyEmptyProperties(object)).toBe(false);
    });

    it('should return false when object has at least a valued property', () => {
      object = { a: [], b: 'b' };
      expect(hasOnlyEmptyProperties(object)).toBe(false);
    });

  });

  describe('difference', () => {

    it('should return an empty object', () => {
      object = {};
      anotherObject = {};
      objectExpected = {};
      expect(difference(object, anotherObject)).toEqual(objectExpected);
    });

    it('should return object properties that are not included in the base object', () => {
      object = { a: 'a', b: 'b' };
      anotherObject = { a: 'a' };
      objectExpected = { b: 'b' };
      expect(difference(object, anotherObject)).toEqual(objectExpected);
    });

    it('should not return object properties that are included only in the base object', () => {
      object = { a: 'a' };
      anotherObject = { a: 'a', b: 'b' };
      objectExpected = {};
      expect(difference(object, anotherObject)).toEqual(objectExpected);
    });

    it('should not return empty object properties that are not included in the base object', () => {
      object = { a: 'a', b: {} };
      anotherObject = { a: 'a' };
      objectExpected = {};
      expect(difference(object, anotherObject)).toEqual(objectExpected);
    });

  });
});
