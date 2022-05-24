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

        if (currentState == null && newState.equals(ContentStatus.Reviewed)) {
            info().data("last edited by", lastEditedBy).data("user", user)
                    .log("Attempt to review a resource that hasn't been submitted for review");

            throw new BadRequestException("Cannot be reviewed without being submitted for review first");
        }

        // Updating from scratch to 'in progress' or 'complete' state so don't need to perform following checks
        if (currentState == null && (newState.equals(ContentStatus.InProgress) || newState.equals(ContentStatus.Complete))) {
            info().data("user", user)
                    .data("last edited by", lastEditedBy)
                    .data("current state", currentState)
                    .data("new state", newState)
                    .log("Updating resource state for first time");

            return newState;
        }

        // The same user can't review edits they've submitted for review
        if (!currentState.equals(ContentStatus.Reviewed) && newState.equals(ContentStatus.Reviewed) && lastEditedBy.equalsIgnoreCase(user)) {
            info().data("user", user)
                    .data("last edited by", lastEditedBy)
                    .data("current state", currentState)
                    .data("new state", newState)
                    .log("User attempting to review their own resource");

            throw new ForbiddenException("User " + user + "doesn't have permission to review a resource they completed");
        }

        // Any further updates made by the user who submitted the dataset should keep the dataset in the awaiting review state
        if (currentState.equals(ContentStatus.Complete) && lastEditedBy.equalsIgnoreCase(user)) {

            info().data("user", user)
                    .data("last edited by", lastEditedBy)
                    .data("current state", currentState)
                    .data("new state", newState)
                    .log("User making more updates to a resource whilst it is awaiting review");
            return ContentStatus.Complete;
        }

        // Any updates to a dataset awaiting review by a different user means it moves back to an in progress state
        if (currentState.equals(ContentStatus.Complete) && !newState.equals(ContentStatus.Reviewed) && !lastEditedBy.equalsIgnoreCase(user)) {

            info().data("user", user)
                    .data("last edited by", lastEditedBy)
                    .data("current state", currentState)
                    .data("new state", newState)
                    .log("A different user making updates to a resource whilst it is awaiting review");
            return ContentStatus.InProgress;
        }

        // Once reviewed any updates can be made to a dataset without the state changing
        if (currentState.equals(ContentStatus.Reviewed)) {

            info().data("user", user)
                    .data("last edited by", lastEditedBy)
                    .data("current state", currentState)
                    .data("new state", newState)
                    .log("Making updates to a review resource");
            return ContentStatus.Reviewed;
        }

        info().data("user", user)
                .data("last edited by", lastEditedBy)
                .data("current state", currentState)
                .data("new state", newState)
                .log("Updating resource state");
        return newState;
    }
}
