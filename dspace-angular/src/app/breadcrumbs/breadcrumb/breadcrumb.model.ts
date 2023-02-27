/**
 * Class representing a single breadcrumb
 */
export class Breadcrumb {
  constructor(
    /**
     * The display value of the breadcrumb
     */
    public text: string,
    /**
     * The optional url of the breadcrumb
     */
    public url?: string) {
  }
}
