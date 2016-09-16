package com.github.onsdigital.zebedee.json;

/**
 * Enum to define the various states of the approval process.
 */
public enum ApprovalStatus {
    NOT_STARTED, // The default state of the approval before it has started
    IN_PROGRESS, // The approval has started
    COMPLETE, // The approval has completed successfully
    ERROR, // There was an error processing the approval
}
