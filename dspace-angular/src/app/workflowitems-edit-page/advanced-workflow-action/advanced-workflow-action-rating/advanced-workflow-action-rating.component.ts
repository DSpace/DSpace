import { Component, OnInit } from '@angular/core';
import {
  rendersAdvancedWorkflowTaskOption
} from '../../../shared/mydspace-actions/claimed-task/switcher/claimed-task-actions-decorator';
import { AdvancedWorkflowActionComponent } from '../advanced-workflow-action/advanced-workflow-action.component';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { WorkflowAction } from '../../../core/tasks/models/workflow-action-object.model';
import { RatingAdvancedWorkflowInfo } from '../../../core/tasks/models/rating-advanced-workflow-info.model';

export const ADVANCED_WORKFLOW_TASK_OPTION_RATING = 'submit_score';
export const ADVANCED_WORKFLOW_ACTION_RATING = 'scorereviewaction';

/**
 * The page on which reviewers can rate submitted items.
 */
@rendersAdvancedWorkflowTaskOption(ADVANCED_WORKFLOW_ACTION_RATING)
@Component({
  selector: 'ds-advanced-workflow-action-rating-reviewer',
  templateUrl: './advanced-workflow-action-rating.component.html',
  styleUrls: ['./advanced-workflow-action-rating.component.scss'],
  preserveWhitespaces: false,
})
export class AdvancedWorkflowActionRatingComponent extends AdvancedWorkflowActionComponent implements OnInit {

  ratingForm: FormGroup;

  ngOnInit(): void {
    super.ngOnInit();
    this.ratingForm = new FormGroup({
      review: new FormControl(''),
      rating: new FormControl(0, Validators.min(1)),
    });
  }

  /**
   * Only run **performAction()** when the form has been correctly filled in
   */
  performAction(): void {
    this.ratingForm.updateValueAndValidity();
    if (this.ratingForm.valid) {
      super.performAction();
    } else {
      this.ratingForm.markAllAsTouched();
    }
  }

  /**
   * Returns the task option, the score and the review if one was provided
   */
  createBody(): any {
    const body = {
      [ADVANCED_WORKFLOW_TASK_OPTION_RATING]: true,
      score: this.ratingForm.get('rating').value,
    };
    if (this.ratingForm.get('review').value !== '') {
      const review: string = this.ratingForm.get('review').value;
      Object.assign(body, { review: review });
    }
    return body;
  }

  getType(): string {
    return ADVANCED_WORKFLOW_ACTION_RATING;
  }

  getAdvancedInfo(workflowAction: WorkflowAction | null): RatingAdvancedWorkflowInfo | null {
    return workflowAction ? (workflowAction.advancedInfo[0] as RatingAdvancedWorkflowInfo) : null;
  }

  /**
   * Returns whether the field is valid or not.
   *
   * @param formControlName The input field
   */
  isInvalid(formControlName: string): boolean {
    return this.ratingForm.get(formControlName).touched && !this.ratingForm.get(formControlName).valid;
  }

}
