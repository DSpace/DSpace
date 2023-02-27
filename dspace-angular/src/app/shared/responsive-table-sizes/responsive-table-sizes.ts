import { ResponsiveColumnSizes } from './responsive-column-sizes';
import { hasValue } from '../empty.util';

/**
 * A helper class storing the sizes in which to render a table
 * It stores a list of columns, which in turn store their own bootstrap column sizes
 */
export class ResponsiveTableSizes {
  /**
   * A list of all the columns and their responsive sizes within this table
   */
  columns: ResponsiveColumnSizes[];

  constructor(columns: ResponsiveColumnSizes[]) {
    this.columns = columns;
  }

  /**
   * Combine the values of multiple columns into a single ResponsiveColumnSizes
   * Useful when a row element stretches over multiple columns
   * @param start Index of the first column
   * @param end   Index of the last column (inclusive)
   */
  combineColumns(start: number, end: number): ResponsiveColumnSizes {
    if (start < end && hasValue(this.columns[start]) && hasValue(this.columns[end])) {
      let xs = this.columns[start].xs;
      let sm = this.columns[start].sm;
      let md = this.columns[start].md;
      let lg = this.columns[start].lg;
      let xl = this.columns[start].xl;
      for (let i = start + 1; i < end + 1; i++) {
        xs += this.columns[i].xs;
        sm += this.columns[i].sm;
        md += this.columns[i].md;
        lg += this.columns[i].lg;
        xl += this.columns[i].xl;
      }
      return new ResponsiveColumnSizes(xs, sm, md, lg, xl);
    }
    return undefined;
  }
}
