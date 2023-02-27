import { isNumeric } from './numeric.util';

describe('Numeric Utils', () => {
  describe('isNumeric', () => {
    it('should return true for Number values', () => {
      expect(isNumeric(0)).toBeTrue();
      expect(isNumeric(123456)).toBeTrue();
      expect(isNumeric(-123456)).toBeTrue();
      expect(isNumeric(0.1234)).toBeTrue();
      expect(isNumeric(-0.1234)).toBeTrue();
      expect(isNumeric(1234e56)).toBeTrue();
      expect(isNumeric(-1234e-56)).toBeTrue();
      expect(isNumeric(0x123456)).toBeTrue();
      expect(isNumeric(-0x123456)).toBeTrue();
    });

    it('should return true for numeric String values', () => {
      expect(isNumeric('0')).toBeTrue();
      expect(isNumeric('123456')).toBeTrue();
      expect(isNumeric('-123456')).toBeTrue();
      expect(isNumeric('0.1234')).toBeTrue();
      expect(isNumeric('-0.1234')).toBeTrue();
      expect(isNumeric('1234e56')).toBeTrue();
      expect(isNumeric('-1234e-56')).toBeTrue();
      expect(isNumeric('0x123456')).toBeTrue();

      // expect(isNumeric('-0x123456')).toBeTrue();  // not recognized as numeric, known issue
    });

    it('should return false for non-numeric String values', () => {
      expect(isNumeric('just a regular string')).toBeFalse();
      expect(isNumeric('')).toBeFalse();
      expect(isNumeric(' ')).toBeFalse();
      expect(isNumeric('\n')).toBeFalse();
      expect(isNumeric('\t')).toBeFalse();
      expect(isNumeric('null')).toBeFalse();
      expect(isNumeric('undefined')).toBeFalse();
    });

    it('should return false for any other kind of value', () => {
      expect(isNumeric([1,2,3])).toBeFalse();
      expect(isNumeric({ a:1, b:2, c:3 })).toBeFalse();
      expect(isNumeric(() => { /* empty */ })).toBeFalse();
      expect(isNumeric(null)).toBeFalse();
      expect(isNumeric(undefined)).toBeFalse();
      expect(isNumeric(true)).toBeFalse();
      expect(isNumeric(false)).toBeFalse();
      expect(isNumeric(NaN)).toBeFalse();
      expect(isNumeric(Infinity)).toBeFalse();
      expect(isNumeric(-Infinity)).toBeFalse();
    });
  });
});
