package com.github.onsdigital.zebedee.model.collection.audit;

/**
 * Constants representing the different types of collection audit events.
 */
public enum AuditAction {

    COLLECTION_CREATED,

    COLLECTION_APPROVED,

    COLLECTION_DELETED,

    COLLECTION_UNLOCKED,

    COLLECTION_PUBLISHED,

    COLLECTION_EDIT_CHANGED_NAME,

    COLLECTION_EDIT_RESCHEDULED,

    COLLECTION_EDIT_ADDED_TEAM,

    COLLECTION_EDIT_REMOVED_TEAM,

    PAGE,

    FILE
}
