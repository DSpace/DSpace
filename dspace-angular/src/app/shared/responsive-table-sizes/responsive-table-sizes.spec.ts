import { ResponsiveColumnSizes } from './responsive-column-sizes';
import { ResponsiveTableSizes } from './responsive-table-sizes';

describe('ResponsiveColumnSizes', () => {
  const column0 = new ResponsiveColumnSizes(2, 3, 4, 6, 8);
  const column1 = new ResponsiveColumnSizes(8, 7, 4, 2, 1);
  const column2 = new ResponsiveColumnSizes(1, 1, 4, 2, 1);
  const column3 = new ResponsiveColumnSizes(1, 1, 4, 2, 2);
  const table = new ResponsiveTableSizes([column0, column1, column2, column3]);

  describe('combineColumns', () => {
    describe('when start value is out of bounds', () => {
      let combined: ResponsiveColumnSizes;

      beforeEach(() => {
        combined = table.combineColumns(-1, 2);
      });

      it('should return undefined', () => {
        expect(combined).toBeUndefined();
      });
    });

    describe('when end value is out of bounds', () => {
      let combined: ResponsiveColumnSizes;

      beforeEach(() => {
        combined = table.combineColumns(0, 5);
      });

      it('should return undefined', () => {
        expect(combined).toBeUndefined();
      });
    });

    describe('when start value is greater than end value', () => {
      let combined: ResponsiveColumnSizes;

      beforeEach(() => {
        combined = table.combineColumns(2, 0);
      });

      it('should return undefined', () => {
        expect(combined).toBeUndefined();
      });
    });

    describe('when start value is equal to end value', () => {
      let combined: ResponsiveColumnSizes;

      beforeEach(() => {
        combined = table.combineColumns(0, 0);
      });

      it('should return undefined', () => {
        expect(combined).toBeUndefined();
      });
    });

    describe('when provided with valid values', () => {
      let combined: ResponsiveColumnSizes;

      beforeEach(() => {
        combined = table.combineColumns(0, 2);
      });

      it('should combine the sizes of each column within the range into one', () => {
        expect(combined.xs).toEqual(column0.xs + column1.xs + column2.xs);
        expect(combined.sm).toEqual(column0.sm + column1.sm + column2.sm);
        expect(combined.md).toEqual(column0.md + column1.md + column2.md);
        expect(combined.lg).toEqual(column0.lg + column1.lg + column2.lg);
        expect(combined.xl).toEqual(column0.xl + column1.xl + column2.xl);
      });
    });
  });
});
