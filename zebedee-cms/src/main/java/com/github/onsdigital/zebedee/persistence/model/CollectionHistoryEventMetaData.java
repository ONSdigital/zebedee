package com.github.onsdigital.zebedee.persistence.model;

import javax.persistence.*;

/**
 * Created by dave on 6/1/16.
 */
@Entity
@Table(name = "history_event_meta_data")
public class CollectionHistoryEventMetaData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "history_event_meta_data_seq")
    @SequenceGenerator(name = "history_event_meta_data_seq",
            sequenceName = "history_event_meta_data_history_event_meta_data_id_seq",
            allocationSize = 1)
    @Column(name = "history_event_meta_data_id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "event_collection_history_event_id",
            foreignKey = @ForeignKey(name = "event_collection_history_event_id")
    )
    private CollectionHistoryEvent event;

    @Column(name = "meta_data_key", nullable = false)
    private String key;

    @Column(name = "meta_data_value", nullable = false)
    private String value;

    public CollectionHistoryEventMetaData() {
    }

    public CollectionHistoryEventMetaData(String key, String value, CollectionHistoryEvent event) {
        this.key = key;
        this.value = value;
        this.event = event;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public CollectionHistoryEvent getEvent() {
        return event;
    }

    public int getId() {
        return id;
    }
}
