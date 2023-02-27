/**
 * A helper class storing the sizes in which to render a single column
 * The values in this class are expected to be between 1 and 12
 * There are used to be added to bootstrap classes such as col-xs-{this.xs}
 */
export class ResponsiveColumnSizes {
  /**
   * The extra small bootstrap size
   */
  xs: number;

  /**
   * The small bootstrap size
   */
  sm: number;

  /**
   * The medium bootstrap size
   */
  md: number;

  /**
   * The large bootstrap size
   */
  lg: number;

  /**
   * The extra large bootstrap size
   */
  xl: number;

  constructor(xs: number, sm: number, md: number, lg: number, xl: number) {
    this.xs = xs;
    this.sm = sm;
    this.md = md;
    this.lg = lg;
    this.xl = xl;
  }

  /**
   * Build the bootstrap responsive column classes matching the values of this object
   */
  buildClasses(): string {
    return `col-${this.xs} col-sm-${this.sm} col-md-${this.md} col-lg-${this.lg} col-xl-${this.xl}`;
  }
}
