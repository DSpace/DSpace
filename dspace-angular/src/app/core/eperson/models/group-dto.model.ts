import { PaginatedList } from '../../data/paginated-list.model';
import { EPerson } from './eperson.model';
import { Group } from './group.model';

/**
 * This class serves as a Data Transfer Model that contains the Group, whether or not it's able to be deleted and its members
 */
export class GroupDtoModel {

  /**
   * The Group linked to this object
   */
  public group: Group;

  /**
   * Whether or not the linked Group is able to be deleted
   */
  public ableToDelete: boolean;

  /**
   * Whether or not the current user is able to edit the linked group
   */
  public ableToEdit: boolean;

  /**
   * List of subgroups of this group
   */
  public subgroups: PaginatedList<Group>;

  /**
   * List of members of this group
   */
  public epersons: PaginatedList<EPerson>;

}
