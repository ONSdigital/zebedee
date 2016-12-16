package com.github.onsdigital.zebedee.persistence.model;

import javax.persistence.*;

/**
 * Entity object for a deleted content event.
 */
@Entity
@Table(name = "deleted_file")
public class DeletedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deleted_file_sequence_generator")
    @SequenceGenerator(name = "deleted_file_sequence_generator",
            sequenceName = "deleted_file_id_sequence",
            allocationSize = 1)
    @Column(name = "deleted_file_id")
    private long id;

    @Column(name = "uri")
    private String uri;

    @ManyToOne
    @JoinColumn(name = "deleted_content_event_id",
            foreignKey = @ForeignKey(name = "deleted_content_event_id")
    )
    private DeletedContentEvent deletedContentEvent;

    public DeletedFile() {
    }

    public DeletedFile(String uri, DeletedContentEvent deletedContentEvent) {
        this.uri = uri;
        this.deletedContentEvent = deletedContentEvent;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public DeletedContentEvent getDeletedContentEvent() {
        return deletedContentEvent;
    }

    public void setDeletedContentEvent(DeletedContentEvent deletedContentEvent) {
        this.deletedContentEvent = deletedContentEvent;
    }
}

