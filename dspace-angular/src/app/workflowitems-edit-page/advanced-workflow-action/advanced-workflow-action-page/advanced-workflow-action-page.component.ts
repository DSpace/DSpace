import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/**
 * The Advanced Workflow page containing the correct {@link AdvancedWorkflowActionComponent}
 * based on the route parameters.
 */
@Component({
  selector: 'ds-advanced-workflow-action-page',
  templateUrl: './advanced-workflow-action-page.component.html',
  styleUrls: ['./advanced-workflow-action-page.component.scss']
})
export class AdvancedWorkflowActionPageComponent implements OnInit {

  public type: string;

  constructor(
    protected route: ActivatedRoute,
  ) {
  }

  ngOnInit(): void {
    this.type = this.route.snapshot.queryParams.workflow;
  }

}
