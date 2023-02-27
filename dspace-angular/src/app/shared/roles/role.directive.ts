import {
  ChangeDetectorRef,
  Directive,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  TemplateRef,
  ViewContainerRef
} from '@angular/core';

import { combineLatest, Observable, Subscription } from 'rxjs';
import { filter, first, map } from 'rxjs/operators';

import { hasValue } from '../empty.util';
import { RoleService } from '../../core/roles/role.service';
import { RoleType } from '../../core/roles/role-types';

@Directive({
  selector: '[dsShowOnlyForRole],[dsShowExceptForRole]'
})
/**
 * Structural Directive for showing or hiding a template based on current user role
 */
export class RoleDirective implements OnChanges, OnDestroy {

  /**
   * The role or list of roles that can show template
   */
  @Input() dsShowOnlyForRole: RoleType | RoleType[];

  /**
   * The role or list of roles that cannot show template
   */
  @Input() dsShowExceptForRole: RoleType | RoleType[];

  private subs: Subscription[] = [];

  constructor(
    private roleService: RoleService,
    private viewContainer: ViewContainerRef,
    private changeDetector: ChangeDetectorRef,
    private templateRef: TemplateRef<any>
  ) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    const onlyChanges = changes.dsShowOnlyForRole;
    const exceptChanges = changes.dsShowExceptForRole;
    this.hasRoles(this.dsShowOnlyForRole);
    if (changes.dsShowOnlyForRole) {
      this.validateOnly();
    } else if (changes.dsShowExceptForRole) {
      this.validateExcept();
    }
  }

  ngOnDestroy(): void {
    this.subs
      .filter((subscription) => hasValue(subscription))
      .forEach((subscription) => subscription.unsubscribe());
  }

  /**
   * Show template in view container
   */
  private showTemplateBlockInView(): void {
    this.viewContainer.clear();
    if (!this.templateRef) {
      return;
    }

    this.viewContainer.createEmbeddedView(this.templateRef);
    this.changeDetector.markForCheck();
  }

  /**
   * Validate the list of roles that can show template
   */
  private validateOnly(): void  {
    this.subs.push(this.hasRoles(this.dsShowOnlyForRole).pipe(filter((hasRole) => hasRole))
      .subscribe((hasRole) => {
        this.showTemplateBlockInView();
      }));
  }

  /**
   * Validate the list of roles that cannot show template
   */
  private validateExcept(): void  {
    this.subs.push(this.hasRoles(this.dsShowExceptForRole).pipe(filter((hasRole) => !hasRole))
      .subscribe((hasRole) => {
        this.showTemplateBlockInView();
      }));
  }

  /**
   * Check if current user role is included in the specified role list
   *
   * @param roles
   *    The role or the list of roles
   * @returns {Observable<boolean>}
   *    observable of true if current user role is included in the specified role list, observable of false otherwise
   */
  private hasRoles(roles: RoleType | RoleType[]): Observable<boolean> {
    const toValidate: RoleType[] = (Array.isArray(roles)) ? roles : [roles];
    const checks: Observable<boolean>[] = toValidate.map((role) => this.roleService.checkRole(role));

    return combineLatest(checks).pipe(
      map((permissions: boolean[]) => permissions.includes(true)),
      first()
    );
  }
}
