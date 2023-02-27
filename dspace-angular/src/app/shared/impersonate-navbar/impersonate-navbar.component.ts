import { Component, OnInit } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '../../app.reducer';
import { AuthService } from '../../core/auth/auth.service';
import { Observable } from 'rxjs';
import { isAuthenticated } from '../../core/auth/selectors';

@Component({
  selector: 'ds-impersonate-navbar',
  templateUrl: 'impersonate-navbar.component.html'
})
/**
 * Navbar component for actions to take concerning impersonating users
 */
export class ImpersonateNavbarComponent implements OnInit {
  /**
   * Whether or not the user is authenticated.
   * @type {Observable<string>}
   */
  isAuthenticated$: Observable<boolean>;

  /**
   * Is the user currently impersonating another user?
   */
  isImpersonating: boolean;

  constructor(private store: Store<AppState>,
              private authService: AuthService) {
  }

  ngOnInit(): void {
    this.isAuthenticated$ = this.store.pipe(select(isAuthenticated));
    this.isImpersonating = this.authService.isImpersonating();
  }

  /**
   * Stop impersonating the user
   */
  stopImpersonating() {
    this.authService.stopImpersonatingAndRefresh();
  }
}
