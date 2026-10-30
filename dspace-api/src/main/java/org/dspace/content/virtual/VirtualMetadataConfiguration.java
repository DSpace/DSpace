/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * This interface describes beans to be used for the {@link VirtualMetadataPopulator} implementation.
 * The config is located in core-services.xml whilst the actual code implementation is located in
 * {@link org.dspace.content.ItemServiceImpl}
 */
public interface VirtualMetadataConfiguration {
    /**
     * Represents a metadata value result with an optional placement indicator.
     * Used by VirtualMetadataConfiguration implementations to return metadata values
     * along with their position information for ordering purposes.
     */
    class ValueResult {
        /**
         * The string representation of the metadata value.
         * This field is immutable once set through the constructor.
         */
        private final String value;
        /**
         * The placement indicator for this value within a list of metadata values.
         * Used to maintain or establish ordering of metadata values.
         * Can be null if placement is not relevant or not yet determined.
         */
        private Integer place;

        /**
         * Compares this ValueResult with another object for equality.
         * Two ValueResult objects are considered equal if they have the same
         * value and place properties.
         *
         * @param o The object to compare with
         * @return true if the objects are equal, false otherwise
         */
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ValueResult that = (ValueResult) o;
            return Objects.equals(value, that.value) && Objects.equals(place, that.place);
        }

        /**
         * Returns a hash code value for this ValueResult.
         * The hash code is computed based on both the value and place properties.
         *
         * @return A hash code value for this object
         */
        @Override
        public int hashCode() {
            return Objects.hash(value, place);
        }

        /**
         * Creates a new ValueResult with the specified value and no placement information.
         * The place is initialized to null and can be set later using setPlace().
         *
         * @param value The string value of the metadata
         */
        public ValueResult(String value) {
            this.value = value;
            this.place = null;
        }

        /**
         * Sets the placement indicator for this value result.
         * This method is package-private and intended for internal use, particularly
         * when placement information needs to be assigned after initial construction.
         *
         * @param place The placement indicator to set
         */
        void setPlace(Integer place) {
            this.place = place;
        }

        /**
         * Returns the string value of this result.
         *
         * @return The metadata value as a string
         */
        public String getValue() {
            return value;
        }

        /**
         * Returns the placement indicator for this value result.
         *
         * @return The placement indicator, or null if not set
         */
        public Integer getPlace() {
            return place;
        }
    }

    /**
     * This method will return a list filled with String values which will be determine by the bean that's responsible
     * of handling the metadata fields when fully traversed through all the {@link Related} beans
     *
     * @param context The relevant DSpace context
     * @param item    The item that will be used to either retrieve metadata values from or to find
     *                the related item through its relationships
     * @return The list of the ValueResult values of all metadata values as constructed by the responsible bean
     * @throws SQLException If something goes wrong
     */
    List<ValueResult> getValues(Context context, Item item) throws SQLException;

    /**
     * Generic setter for the useForPlace property
     *
     * @param useForPlace The boolean value that the useForPlace property will be set to
     */
    void setUseForPlace(boolean useForPlace);

    /**
     * Generic getter for the useForPlace property
     *
     * @return The useForPlace to be used by this bean
     */
    boolean getUseForPlace();

    /**
     * Generic setter for the populateWithNameVariant
     * This property defines whether the value should be retrieved from the left/rightward on the Relationship (true)
     * or through the configuration and usual way (false)
     *
     * @param populateWithNameVariant The boolean value that the populateWithNameVariant property will be set to
     */
    void setPopulateWithNameVariant(boolean populateWithNameVariant);

    /**
     * Generic getter for the populateWithNameVariant property
     *
     * @return The populatewithNameVariant to be used by this bean
     */
    boolean getPopulateWithNameVariant();
}
