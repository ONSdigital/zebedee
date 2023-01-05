package com.github.onsdigital.zebedee.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ForbiddenException;
import com.github.onsdigital.zebedee.json.ContentStatus;

public class ContentStatusUtilsTest {
    private static final String ALICE = "alice";
    private static final String BOB = "bob";

    @Test(expected = NullPointerException.class)
    public void testUpdatedStateInCollectionNullNewState() throws Exception {
        ContentStatus currentStatus = null;
        ContentStatus newStatus = null;
        String user = ALICE;
        String lastEditedBy = null;

        ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user);
    }

    @Test
    public void testUpdatedStateInCollectionInitialToInProgress() throws Exception {
        ContentStatus currentStatus = null;
        ContentStatus newStatus = ContentStatus.InProgress;
        String user = ALICE;
        String lastEditedBy = null;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionInitialToComplete() throws Exception {
        ContentStatus currentStatus = null;
        ContentStatus newStatus = ContentStatus.Complete;
        String user = ALICE;
        String lastEditedBy = null;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test(expected = BadRequestException.class)
    public void testUpdatedStateInCollectionInitialToReviewed() throws Exception {
        ContentStatus currentStatus = null;
        ContentStatus newStatus = ContentStatus.Reviewed;
        String user = ALICE;
        String lastEditedBy = null;

        // Can't go straight to reviewed: expect an exception
        ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user);
    }

    @Test
    public void testUpdatedStateInCollectionInProgressToInProgressSameUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.InProgress;
        ContentStatus newStatus = ContentStatus.InProgress;
        String user = ALICE;
        String lastEditedBy = ALICE;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionInProgressToInProgressDifferentUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.InProgress;
        ContentStatus newStatus = ContentStatus.InProgress;
        String user = ALICE;
        String lastEditedBy = BOB;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionInProgressToCompleteSameUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.InProgress;
        ContentStatus newStatus = ContentStatus.Complete;
        String user = ALICE;
        String lastEditedBy = ALICE;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionInProgressToCompleteDifferentUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.InProgress;
        ContentStatus newStatus = ContentStatus.Complete;
        String user = ALICE;
        String lastEditedBy = BOB;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test(expected = ForbiddenException.class)
    public void testUpdatedStateInCollectionInProgressToReviewedSameUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.InProgress;
        ContentStatus newStatus = ContentStatus.Reviewed;
        String user = ALICE;
        String lastEditedBy = ALICE;

        // Can't review their own resource: expect an exception
        ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user);
    }

    @Test(expected = ForbiddenException.class)
    public void testUpdatedStateInCollectionInProgressToReviewedDifferentUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.InProgress;
        ContentStatus newStatus = ContentStatus.Reviewed;
        String user = ALICE;
        String lastEditedBy = BOB;

       // Can't go straight to Reviewed without being Complete: expect an exception
        ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user);
    }

    @Test
    public void testUpdatedStateInCollectionReviewedToInProgressSameUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.Reviewed;
        ContentStatus newStatus = ContentStatus.InProgress;
        String user = ALICE;
        String lastEditedBy = ALICE;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionReviewedToInProgressDifferentUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.Reviewed;
        ContentStatus newStatus = ContentStatus.InProgress;
        String user = ALICE;
        String lastEditedBy = BOB;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionReviewedToCompleteSameUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.Reviewed;
        ContentStatus newStatus = ContentStatus.Complete;
        String user = ALICE;
        String lastEditedBy = ALICE;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionReviewedToCompleteDifferentUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.Reviewed;
        ContentStatus newStatus = ContentStatus.Complete;
        String user = ALICE;
        String lastEditedBy = BOB;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionReviewedToReviewedSameUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.Reviewed;
        ContentStatus newStatus = ContentStatus.Reviewed;
        String user = ALICE;
        String lastEditedBy = ALICE;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionReviewedToReviewedDifferentUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.Reviewed;
        ContentStatus newStatus = ContentStatus.Reviewed;
        String user = ALICE;
        String lastEditedBy = BOB;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionCompleteToInProgressSameUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.Complete;
        ContentStatus newStatus = ContentStatus.InProgress;
        String user = ALICE;
        String lastEditedBy = ALICE;

        // Updating a resource whilst awaiting review, keep them in review
        ContentStatus expectedNewStatus = ContentStatus.Complete;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionCompleteToInProgressDifferentUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.Complete;
        ContentStatus newStatus = ContentStatus.InProgress;
        String user = ALICE;
        String lastEditedBy = BOB;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test(expected = ForbiddenException.class)
    public void testUpdatedStateInCollectionCompleteToReviewedSameUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.Complete;
        ContentStatus newStatus = ContentStatus.Reviewed;
        String user = ALICE;
        String lastEditedBy = ALICE;

        // Can't review their own resource: expect an exception
        ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user);
    }

    @Test
    public void testUpdatedStateInCollectionCompleteToReviewedDifferentUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.Complete;
        ContentStatus newStatus = ContentStatus.Reviewed;
        String user = ALICE;
        String lastEditedBy = BOB;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionCompleteToCompleteSameUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.Complete;
        ContentStatus newStatus = ContentStatus.Complete;
        String user = ALICE;
        String lastEditedBy = ALICE;

        // Should accept the new status
        ContentStatus expectedNewStatus = newStatus;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

    @Test
    public void testUpdatedStateInCollectionCompleteToCompleteDifferentUser() throws Exception {
        ContentStatus currentStatus = ContentStatus.Complete;
        ContentStatus newStatus = ContentStatus.Complete;
        String user = ALICE;
        String lastEditedBy = BOB;

        // Should go back to In Progress
        ContentStatus expectedNewStatus = ContentStatus.InProgress;

        assertEquals(expectedNewStatus,
                ContentStatusUtils.updatedStateInCollection(currentStatus, newStatus, lastEditedBy, user));
    }

}
