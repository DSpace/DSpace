import { ResponsiveColumnSizes } from './responsive-column-sizes';

describe('ResponsiveColumnSizes', () => {
  const xs = 2;
  const sm = 3;
  const md = 4;
  const lg = 6;
  const xl = 8;
  const column = new ResponsiveColumnSizes(xs, sm, md, lg, xl);

  describe('buildClasses', () => {
    let classes: string;

    beforeEach(() => {
      classes = column.buildClasses();
    });

    it('should return the correct bootstrap classes', () => {
      expect(classes).toEqual(`col-${xs} col-sm-${sm} col-md-${md} col-lg-${lg} col-xl-${xl}`);
    });
  });
});
