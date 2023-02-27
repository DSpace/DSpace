import { AbstractControl, ValidationErrors } from '@angular/forms';
import { Observable } from 'rxjs';
import { map} from 'rxjs/operators';

import { GroupDataService } from '../../../../core/eperson/group-data.service';
import { getFirstSucceededRemoteListPayload } from '../../../../core/shared/operators';
import { Group } from '../../../../core/eperson/models/group.model';

export class ValidateGroupExists {

  /**
   * This method will create the validator with the groupDataService requested from component
   * @param groupDataService the service with DI in the component that this validator is being utilized.
   * @return Observable<ValidationErrors | null>
   */
  static createValidator(groupDataService: GroupDataService) {
    return (control: AbstractControl): Promise<ValidationErrors | null> | Observable<ValidationErrors | null> => {
      return groupDataService.searchGroups(control.value, {
            currentPage: 1,
            elementsPerPage: 100
          })
        .pipe(
          getFirstSucceededRemoteListPayload(),
          map( (groups: Group[]) => {
            return groups.filter(group => group.name === control.value);
          }),
          map( (groups: Group[]) => {
            return groups.length > 0 ? { groupExists: true } : null;
          }),
        );
    };
  }
}
