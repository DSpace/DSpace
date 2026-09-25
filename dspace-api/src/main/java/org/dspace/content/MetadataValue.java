/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.dspace.content.authority.Choices;
import org.dspace.core.Context;
import org.dspace.core.HibernateProxyHelper;
import org.dspace.core.ReloadableEntity;
import org.hibernate.Length;

/**
 * Database access class representing a Dublin Core metadata value.
 * It represents a value of a given <code>MetadataField</code> on an Item.
 * (The Item can have many values of the same field.)  It contains element, qualifier, value and language.
 * the field (which names the schema, element, and qualifier), language,
 * and a value.
 *
 * @author Martin Hald
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 * @see org.dspace.content.MetadataSchema
 * @see org.dspace.content.MetadataField
 */
@Entity
@Table(name = "metadatavalue")
@SecondaryTable(
    name = "relationship",
    pkJoinColumns = @PrimaryKeyJoinColumn(name = "metadata_value_id", referencedColumnName = "metadata_value_id")
)
public class MetadataValue implements ReloadableEntity<Integer> {
    /**
     * The reference to the metadata field
     */
    @Id
    @Column(name = "metadata_value_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "metadatavalue_seq")
    @SequenceGenerator(name = "metadatavalue_seq", sequenceName = "metadatavalue_seq", allocationSize = 1)
    private final Integer id;

    /**
     * The primary key for the metadata value
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "metadata_field_id")
    private MetadataField metadataField = null;

    /**
     * The value of the field
     */
    @Column(name = "text_value", length = Length.LONG32)
    private String value;

    /**
     * The language of the field, may be <code>null</code>
     */
    @Column(name = "text_lang", length = 24)
    private String language;

    /**
     * The position of the record.
     */
    @Column(name = "place")
    private int place = 1;

    /**
     * Authority key, if any
     */
    @Column(name = "authority", length = 100)
    private String authority = null;

    /**
     * Authority confidence value -- see Choices class for values
     */
    @Column(name = "confidence")
    private int confidence = -1;

    /**
     * Security level value
     */
    @Nullable
    @Column(name = "security_level")
    private Integer securityLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dspace_object_id")
    protected DSpaceObject dSpaceObject;

    /**
     * The left item (usually the owner of the metadata value) of the authority-backed
     * {@code relationship} row joined through the {@link SecondaryTable}.
     * <p>
     * Both {@link #leftItem} and {@link #rightItem} are written together when the value
     * is minted. They are only persisted into the secondary table on the value's own
     * {@code INSERT}: the authority-backed relationship lives and dies with the
     * metadata value it belongs to.
     * </p>
     */
    @Column(table = "relationship", name = "left_id", nullable = false)
    private UUID leftItem;

    /**
     * The right item (usually the resolved target) of the authority-backed
     * {@code relationship} row joined through the {@link SecondaryTable}.
     */
    @Column(table = "relationship", name = "right_id", nullable = false)
    private UUID rightItem;

    /*
     * NOTE: the generated {@code relationship.id} of the joined row is intentionally NOT mapped.
     *
     * It was previously mapped read-only, so Hibernate would read the DB-generated value back
     * after each insert:
     *
     *     @Column(table = "relationship", name = "id", insertable = false, updatable = false)
     *     @Generated
     *     private Integer relationshipRowId;
     *
     * That read-only mapping is not needed here:
     * - {@code relationship.id} is populated by the database default
     *   ({@code nextval('relationship_id_seq')}); the secondary row insert omits the column and the
     *   value is never written by, nor read into, the domain model.
     * - Keeping the mapping forced Hibernate to run an extra read-back SELECT after *every*
     *   metadatavalue insert, whether or not a relationship row exists. That is one extra
     *   round-trip per metadata value with no functional benefit.
     * - The id is also not required for lifecycle management: the joined row is inserted, updated
     *   and deleted together with the metadata value (Hibernate wraps the optional secondary table
     *   in an upsert that deletes the row when both sides are cleared), and the
     *   {@code relationship_metadata_value_id_fk ... ON DELETE CASCADE} is the final safeguard.
     *
     * If the generated id is ever needed again, restoring the mapping above is enough, but the
     * read-back cost per inserted metadata value must be taken into account.
     */

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.MetadataValueService#create(Context, DSpaceObject, MetadataField)}
     */
    protected MetadataValue() {
        id = 0;
    }

    /**
     * Get the field ID the metadata value represents.
     *
     * @return metadata value ID
     */
    @Override
    public Integer getID() {
        return id;
    }

    /**
     * Get the dspaceObject
     *
     * @return dspaceObject
     */
    public DSpaceObject getDSpaceObject() {
        return dSpaceObject;
    }

    /**
     * Set the dspaceObject ID.
     *
     * @param dso new dspaceObject ID
     */
    public void setDSpaceObject(DSpaceObject dso) {
        this.dSpaceObject = dso;
    }

    /**
     * Get the language (e.g. "en").
     *
     * @return language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Set the language (e.g. "en").
     *
     * @param language new language
     */
    public void setLanguage(String language) {
        if (Strings.CS.equals(language, Item.ANY)) {
            language = null;
        }
        this.language = language;
    }

    /**
     * Get the place ordering.
     *
     * @return place ordering
     */
    public int getPlace() {
        return place;
    }

    /**
     * Set the place ordering.
     *
     * @param place new place (relative order in series of values)
     */
    public void setPlace(int place) {
        this.place = place;
    }

    public MetadataField getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(MetadataField metadataField) {
        this.metadataField = metadataField;
    }

    /**
     * @return {@code MetadataField#getID()}
     */
    @Transient
    protected Integer getMetadataFieldId() {
        return getMetadataField().getID();
    }

    /**
     * Get the metadata value.
     *
     * @return metadata value
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the metadata value
     *
     * @param value new metadata value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get the metadata authority.
     * <p>
     * For a resolved internal reference the {@code authority} column is elided (nulled) after
     * the backing {@code relationship} row is minted — the target UUID is then stored only once,
     * in {@code right_id}. This getter reconstitutes it transparently: it returns the raw column
     * when present, otherwise the {@code right_id} UUID when the relationship endpoints are set,
     * otherwise {@code null}.
     * </p>
     *
     * @return metadata authority
     */
    public String getAuthority() {
        if (authority != null) {
            return authority;
        }
        return rightItem != null ? rightItem.toString() : null;
    }

    /**
     * Return the literal {@code authority} column value, without deriving from {@code right_id}.
     * <p>
     * <strong>Internal use only</strong>: only the reconcile/mint machinery
     * ({@code AuthorityBackedRelationshipService}) may read the raw column, so it can tell an
     * already-elided value (raw {@code null}, endpoints set) apart from one whose authority
     * genuinely names nothing. All other readers must use {@link #getAuthority()}.
     * </p>
     *
     * @return the raw {@code authority} column, possibly {@code null}
     */
    public String getRawAuthority() {
        return authority;
    }

    /**
     * Set the metadata authority.
     * <p>
     * Blanking the authority on a value that currently backs an internal relationship is a
     * librarian "clear": both relationship endpoints are nulled (so Hibernate deletes the
     * secondary {@code relationship} row) and the confidence is reset to unset, while the human
     * readable display value is kept. Retarget (a non-blank new value) and external authority
     * keys (which have no {@code right_id}) are unaffected.
     * </p>
     *
     * @param value new metadata authority
     */
    public void setAuthority(String value) {
        boolean clearing = StringUtils.isBlank(value);
        if (clearing && rightItem != null) {
            // Librarian clear: unlink the relationship (both endpoints null -> row deleted) but
            // keep the display text. Confidence must not stay accepted for a now-detached value.
            this.leftItem = null;
            this.rightItem = null;
            this.confidence = Choices.CF_UNSET;
        }
        this.authority = clearing ? null : value;
        if (dSpaceObject != null) {
            // An authority change can change whether this value is backed by a
            // relationship, so make sure the owning object is reconciled on update.
            dSpaceObject.setMetadataModified();
        }
    }

    /**
     * Elide (null out) only the {@code authority} column, leaving the relationship endpoints
     * intact so {@link #getAuthority()} can reconstitute the UUID from {@code right_id}.
     * <p>
     * <strong>Internal use only</strong> and deliberately does <strong>not</strong> mark the
     * owning object modified: it is a field-level operation invoked by the mint machinery
     * ({@code AuthorityBackedRelationshipService}) once a relationship row has been stamped, and
     * must not re-open the dirty window (which would trigger a redundant reconcile pass). The
     * endpoints must already be set before this is called. It is public only because that service
     * lives in a sibling package; treat it as package-private in intent.
     * </p>
     */
    public void elideAuthorityColumn() {
        this.authority = null;
    }

    /**
     * Get the metadata confidence
     *
     * @return metadata confidence
     */
    public int getConfidence() {
        return confidence;
    }

    /**
     * Set the metadata confidence
     *
     * @param value new metadata confidence
     */
    public void setConfidence(int value) {
        this.confidence = value;
    }


    /**
     * Return <code>true</code> if <code>other</code> is the same MetadataValue
     * as this object, <code>false</code> otherwise
     *
     * @param obj object to compare to
     * @return <code>true</code> if object passed in represents the same
     * MetadataValue as this object
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
        if (!getClass().equals(objClass)) {
            return false;
        }
        final MetadataValue other = (MetadataValue) obj;
        if (!this.id.equals(other.id)) {
            return false;
        }
        if (!this.getID().equals(other.getID())) {
            return false;
        }
        return this.getDSpaceObject().getID().equals(other.getDSpaceObject().getID());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + this.id;
        hash = 47 * hash + this.getID();
        hash = 47 * hash + this.getDSpaceObject().getID().hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "MetadataValue [id=" + id + ", metadataField=" + metadataField + ", value=" + value + ", language="
            + language + ", place=" + place + ", authority=" + authority + ", confidence=" + confidence
            + ", securityLevel=" + securityLevel + "]";
    }

    public String getSchema() {
        return getMetadataField().getMetadataSchema().getName();
    }

    public String getElement() {
        return getMetadataField().getElement();
    }

    public String getQualifier() {
        return getMetadataField().getQualifier();
    }

    public Integer getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(Integer securityLevel) {
        this.securityLevel = securityLevel;
    }

    /**
     * Get the left item UUID of the authority-backed relationship owned by this value.
     *
     * @return the left item (owner) UUID, or {@code null} if this value owns no relationship
     */
    @Nullable
    public UUID getLeftItem() {
        return leftItem;
    }

    /**
     * Set the left item UUID of the authority-backed relationship owned by this value.
     *
     * @param leftItem the owner item UUID
     * @return this, to allow fluent usage
     */
    public MetadataValue setLeftItem(UUID leftItem) {
        this.leftItem = leftItem;
        return this;
    }

    /**
     * Get the right item UUID of the authority-backed relationship owned by this value.
     *
     * @return the right item (target) UUID, or {@code null} if this value owns no relationship
     */
    @Nullable
    public UUID getRightItem() {
        return rightItem;
    }

    /**
     * Set the right item UUID of the authority-backed relationship owned by this value.
     *
     * @param rightItem the target item UUID
     * @return this, to allow fluent usage
     */
    public MetadataValue setRightItem(UUID rightItem) {
        this.rightItem = rightItem;
        return this;
    }

    /**
     * Return {@code true} when this value carries the two items of an authority-backed relationship.
     *
     * @return {@code true} if both the left and right item UUIDs are set
     */
    @Transient
    public boolean isRelationshipBacked() {
        return leftItem != null && rightItem != null;
    }
}
