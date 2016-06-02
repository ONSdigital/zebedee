package com.github.onsdigital.zebedee.persistence.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Created by dave on 6/1/16.
 */
@Entity
@Table(name = "history_event_meta_data")
public class CollectionHistoryEventMetaData {

    public CollectionHistoryEventMetaData() {
    }

    public CollectionHistoryEventMetaData(String key, String value, CollectionHistoryEvent event) {
        this.key = key;
        this.value = value;
        this.event = event;
    }

    @Id
    @GeneratedValue
    @Column(name = "history_event_meta_data_id")
    private int id;

    @ManyToOne
    private CollectionHistoryEvent event;

    @Column(name = "meta_data_key")
    private String key;

    @Column(name = "meta_data_value")
    private String value;

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
