import { BehaviorSubject, Observable } from 'rxjs';
import { RoleType } from '../../core/roles/role-types';

export class RoleServiceMock {

  _isSubmitter = new BehaviorSubject(true);
  _isController = new BehaviorSubject(true);
  _isAdmin = new BehaviorSubject(true);

  setSubmitter(isSubmitter: boolean) {
    this._isSubmitter.next(isSubmitter);
  }

  setController(isController: boolean) {
    this._isController.next(isController);
  }

  setAdmin(isAdmin: boolean) {
    this._isAdmin.next(isAdmin);
  }

  isSubmitter(): Observable<boolean> {
    return this._isSubmitter;
  }

  isController(): Observable<boolean> {
    return this._isController;
  }

  isAdmin(): Observable<boolean> {
    return this._isAdmin;
  }

  checkRole(role: RoleType): Observable<boolean> {
    let check: Observable<boolean>;
    switch (role) {
      case RoleType.Submitter:
        check = this.isSubmitter();
        break;
      case RoleType.Controller:
        check = this.isController();
        break;
      case RoleType.Admin:
        check = this.isAdmin();
        break;
    }

    return check;
  }
}
