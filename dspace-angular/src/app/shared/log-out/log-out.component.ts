import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { Observable } from 'rxjs';
import { select, Store } from '@ngrx/store';

import { LogOutAction } from '../../core/auth/auth.actions';
import { getLogOutError, } from '../../core/auth/selectors';
import { AppState } from '../../app.reducer';
import { fadeOut } from '../animations/fade';

@Component({
  selector: 'ds-log-out',
  templateUrl: './log-out.component.html',
  styleUrls: ['./log-out.component.scss'],
  animations: [fadeOut]
})
export class LogOutComponent implements OnInit {
  /**
   * The error if authentication fails.
   * @type {Observable<string>}
   */
  public error: Observable<string>;

  /**
   * @constructor
   * @param {Store<State>} store
   * @param {Router} router
   */
  constructor(private router: Router,
              private store: Store<AppState>) {
  }

  /**
   * Lifecycle hook that is called after data-bound properties of a directive are initialized.
   */
  ngOnInit() {
    // set error
    this.error = this.store.pipe(select(getLogOutError));
  }

  /**
   * Go to the home page.
   */
  public home() {
    this.router.navigate(['/home']);
  }

  public logOut() {
    this.store.dispatch(new LogOutAction());
  }

}
