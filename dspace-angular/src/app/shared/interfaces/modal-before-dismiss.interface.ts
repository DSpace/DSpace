
/**
 * If a component implementing this interface is used to create a modal (i.e. it is passed to {@link NgbModal#open}),
 * and that modal is dismissed, then {@link #beforeDismiss} will be called.
 *
 * This behavior is implemented for the whole app, by setting a default value for {@link NgbModalConfig#beforeDismiss}
 * in {@link AppComponent}.
 *
 * Docs: https://ng-bootstrap.github.io/#/components/modal/api
 */
export interface ModalBeforeDismiss {

  /**
   * Callback right before the modal will be dismissed.
   * If this function returns:
   * - false
   * - a promise resolved with false
   * - a promise that is rejected
   * then the modal won't be dismissed.
   * Docs: https://ng-bootstrap.github.io/#/components/modal/api#NgbModalOptions
   */
  beforeDismiss(): boolean | Promise<boolean>;

}
