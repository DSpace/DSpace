/**
 * A class to represent the data retrieved by after processing a task
 */
export class ProcessTaskResponse {
  constructor(
    private isSuccessful: boolean,
    public statusCode?: number,
    public errorMessage?: string
  ) {
  }

  get hasSucceeded(): boolean {
    return this.isSuccessful;
  }
}
