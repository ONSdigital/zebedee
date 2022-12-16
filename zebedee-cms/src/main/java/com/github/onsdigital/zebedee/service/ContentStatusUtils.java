package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ForbiddenException;
import com.github.onsdigital.zebedee.json.ContentStatus;

import java.util.Objects;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

public class ContentStatusUtils {

    private ContentStatusUtils() {}

    public static ContentStatus updatedStateInCollection(ContentStatus currentState, ContentStatus newState, String lastEditedBy, String user) throws ForbiddenException, BadRequestException {

        Objects.requireNonNull(newState);
        
        if (currentState == null) {
            switch (newState) {
            case Reviewed:
                info().data("last edited by", lastEditedBy)
                        .data("user", user)
                        .log("Attempt to review a resource that hasn't been submitted for review");

                throw new BadRequestException("Cannot be reviewed without being submitted for review first");
            case InProgress:
            case Complete:
                // Updating from scratch to 'in progress' or 'complete' state so don't need to perform following checks
                info().data("user", user)
                        .data("last edited by", lastEditedBy)
                        .data("current state", currentState)
                        .data("new state", newState)
                        .log("Updating resource state for first time");

                return newState;
            }
        }

        // Can't skip the Complete state
        if (currentState.equals(ContentStatus.InProgress) && newState.equals(ContentStatus.Reviewed)) {
            info().data("user", user)
                    .data("last edited by", lastEditedBy)
                    .data("current state", currentState)
                    .data("new state", newState)
                    .log("User attempting to review resource in progress (should be Complete first)");

            throw new ForbiddenException("Can't approve a resource in progress");
        }

        if (currentState.equals(ContentStatus.Complete)) {
            if (lastEditedBy.equalsIgnoreCase(user)) {
                if (newState.equals(ContentStatus.Reviewed)) {
                    // The same user can't review edits they've submitted for review
                    info().data("user", user)
                            .data("last edited by", lastEditedBy)
                            .data("current state", currentState)
                            .data("new state", newState)
                            .log("User attempting to review their own resource");

                    throw new ForbiddenException("User " + user + "doesn't have permission to review a resource they completed");
                } else {
                    // Any further updates made by the user who submitted the dataset should keep the dataset in the awaiting review state
                    info().data("user", user)
                            .data("last edited by", lastEditedBy)
                            .data("current state", currentState)
                            .data("new state", newState)
                            .log("User making more updates to a resource whilst it is awaiting review");
                    return ContentStatus.Complete;
                }
            } else if (newState.equals(ContentStatus.InProgress) || newState.equals(ContentStatus.Complete)) {
                // Any updates to a dataset awaiting review by a different user means it moves back to an in progress state
                info().data("user", user)
                        .data("last edited by", lastEditedBy)
                        .data("current state", currentState)
                        .data("new state", newState)
                        .log("A different user making updates to a resource whilst it is awaiting review");
                return ContentStatus.InProgress;
            }
        }

        info().data("user", user)
                .data("last edited by", lastEditedBy)
                .data("current state", currentState)
                .data("new state", newState)
                .log("Updating resource state");
        return newState;
    }
}
