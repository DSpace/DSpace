import {
  hasCompleted,
  hasFailed,
  hasSucceeded,
  isError,
  isErrorStale,
  isLoading,
  isRequestPending,
  isResponsePending,
  isStale,
  isSuccess,
  isSuccessStale,
  RequestEntryState
} from './request-entry-state.model';

/**
 * A class to represent the state of a remote resource
 */
export class RemoteData<T> {
  constructor(
    public timeCompleted: number,
    public msToLive: number,
    public lastUpdated: number,
    public state: RequestEntryState,
    public errorMessage?: string,
    public payload?: T,
    public statusCode?: number,
  ) {
  }

  /**
   * Returns true if this.state is RequestPending, false otherwise
   */
  get isRequestPending(): boolean {
    return isRequestPending(this.state);
  }

  /**
   * Returns true if this.state is ResponsePending, false otherwise
   */
  get isResponsePending(): boolean {
    return isResponsePending(this.state);
  }

  /**
   * Returns true if this.state is Error, false otherwise
   */
  get isError(): boolean {
    return isError(this.state);
  }

  /**
   * Returns true if this.state is Success, false otherwise
   *
   */
  get isSuccess(): boolean {
    return isSuccess(this.state);
  }

  /**
   * Returns true if this.state is ErrorStale, false otherwise
   */
  get isErrorStale(): boolean {
    return isErrorStale(this.state);
  }

  /**
   * Returns true if this.state is SuccessStale, false otherwise
   */
  get isSuccessStale(): boolean {
    return isSuccessStale(this.state);
  }

  /**
   * Returns true if this.state is RequestPending or ResponsePending,
   * false otherwise
   */
  get isLoading(): boolean {
    return isLoading(this.state);
  }

  /**
   * If this.isLoading is true, this method returns undefined, we can't know yet.
   * If it isn't this method will return true if this.state is Error or ErrorStale,
   * false otherwise
   */
  get hasFailed(): boolean {
    return hasFailed(this.state);
  }

  /**
   * If this.isLoading is true, this method returns undefined, we can't know yet.
   * If it isn't this method will return true if this.state is Success or SuccessStale,
   * false otherwise
   */
  get hasSucceeded(): boolean {
    return hasSucceeded(this.state);
  }

  /**
   * Returns true if this.state is not loading, false otherwise
   */
  get hasCompleted(): boolean {
    return hasCompleted(this.state);
  }

  /**
   * Returns true if this.state is SuccessStale or ErrorStale, false otherwise
   */
  get isStale(): boolean {
    return isStale(this.state);
  }

  get hasNoContent(): boolean {
    return this.statusCode === 204;
  }

}
