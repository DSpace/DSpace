/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.Hashtable;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

/**
 * Database entity representation of the previewcontent table.
 * It is not possible to create entity from FileInfo class (without modifications)
 * so we created PreviewContent (which serves as an entity for FileInfo)
 * with corresponding database table previewcontent.
 *
 * @author Michaela Paurikova (dspace at dataquest.sk)
 */
@Entity
@Table(name = "previewcontent")
public class PreviewContent implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "previewcontent_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "previewcontent_previewcontent_id_seq")
    @SequenceGenerator(name = "previewcontent_previewcontent_id_seq",
            sequenceName = "previewcontent_previewcontent_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "bitstream_id")
    private Bitstream bitstream;

    @Column(name = "name")
    public String name;

    @Column(name = "content")
    public String content;

    @Column(name = "isDirectory")
    public boolean isDirectory;

    @Column(name = "size")
    public String size;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "preview2preview",
            joinColumns = @JoinColumn(name = "parent_id"),
            inverseJoinColumns = @JoinColumn(name = "child_id")
    )
    @MapKeyColumn(name = "name")
    public Map<String, PreviewContent> sub = new Hashtable<>();

    /**
     * Protected constructor.
     */
    protected PreviewContent() {}

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.PreviewContentService#create(Context, PreviewContent)}
     */
    protected PreviewContent(PreviewContent previewContent) {
        this.bitstream = previewContent.getBitstream();
        this.name = previewContent.getName();
        this.content = previewContent.getContent();
        this.isDirectory = previewContent.isDirectory();
        this.size = previewContent.getSize();
        this.sub = previewContent.getSubPreviewContents();
    }

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.PreviewContentService#create(Context, Bitstream, String, String, boolean,
     * String, Map<String, PreviewContent>)}
     */
    protected PreviewContent(Bitstream bitstream, String name, String content, boolean isDirectory, String size,
                             Map<String, PreviewContent> subPreviewContents) {
        this.bitstream = bitstream;
        this.name = name;
        this.content = content;
        this.isDirectory = isDirectory;
        this.size = size;
        this.sub = subPreviewContents;
    }

    @Override
    public Integer getID() {
        return id;
    }

    public Bitstream getBitstream() {
        return bitstream;
    }

    public void setBitstream(Bitstream bitstream) {
        this.bitstream = bitstream;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Map<String, PreviewContent> getSubPreviewContents() {
        return sub;
    }
}
