import { EPerson } from './eperson.model';

/**
 * This class serves as a Data Transfer Model that contains the EPerson and whether or not it's able to be deleted
 */
export class EpersonDtoModel {

    /**
     * The EPerson linked to this object
     */
    public eperson: EPerson;
    /**
     * Whether or not the linked EPerson is able to be deleted
     */
    public ableToDelete: boolean;
    /**
     * Whether or not this EPerson is member of group on page it is being used on
     */
    public memberOfGroup: boolean;

}
