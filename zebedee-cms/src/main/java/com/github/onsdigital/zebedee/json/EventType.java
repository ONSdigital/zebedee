package com.github.onsdigital.zebedee.json;

/**
 * Enumeration to represent the events of a file.
 */
public enum EventType {
    CREATED,
    DELETED,
    EDITED,
    COMPLETED,
    REVIEWED,
    APPROVE_SUBMITTED,
    APPROVED,
    APPROVAL_FAILED,
    UNLOCKED,
    PUBLISHED,
    VERSIONED,
    MOVED,
    RENAMED,
    DELETE_MARKER_ADDED,
    DELETE_MARKER_REMOVED,
    VERSION_DELETED,
    VERSION_VERIFICATION_FAILED,
    VERSION_VERIFICATION_BYPASSED,
    CALENDAR_ENTRY_ATTACHED_ON_CREATION
}
