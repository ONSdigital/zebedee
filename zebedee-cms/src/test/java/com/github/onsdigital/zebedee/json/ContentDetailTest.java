package com.github.onsdigital.zebedee.json;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ContentDetailTest {

    @Test
    public void overlayDetailsShouldAddNewItems() {

        // Given a content detail instance with a child
        ContentDetail detail = new ContentDetail("base content", "/", "home");
        ContentDetail child = new ContentDetail("child content", "/child", "article");
        detail.children = new ArrayList<>();
        detail.children.add(child);

        // When we call the overlayDetails method with a new item
        ContentDetail descendant = new ContentDetail("descendant content", "/child/descendant", "bulletin");
        List<ContentDetail> toOverlay = new ArrayList<>();
        toOverlay.add(descendant);
        detail.overlayDetails(toOverlay);

        // Then the item is added into the tree
        assertTrue(detail.containsDescendant(descendant));
    }

    @Test
    public void overlayDetailsShouldAddNewDirectories() {

        // Given a content detail instance
        ContentDetail detail = new ContentDetail("base content", "/", "home");

        // When we call the overlayDetails method with a Content detail instace with new directories to create.
        ContentDetail descendant = new ContentDetail("descendant content", "/childdir1/childdir2/descendant", "bulletin");
        List<ContentDetail> toOverlay = new ArrayList<>();
        toOverlay.add(descendant);
        detail.overlayDetails(toOverlay);

        // Then the directories are created as part of the overlay.
        assertTrue(detail.containsDescendant(descendant));

        ContentDetail childDir1 = detail.getChildWithName("childdir1");
        assertNotNull(childDir1);

        ContentDetail childDir2 = childDir1.getChildWithName("childdir2");
        assertNotNull(childDir2);
    }

    @Test
    public void overlayDetailsShouldIgnoreExistingItems() {

        // Given a content detail instance with an existing descendant
        ContentDetail detail = new ContentDetail("base content", "/", "home");
        ContentDetail child = new ContentDetail("child content", "/child", "article");
        detail.children = new ArrayList<>();
        detail.children.add(child);
        ContentDetail descendant = new ContentDetail("descendant content", "/child/descendant", "bulletin");
        child.children = new ArrayList<>();
        child.children.add(descendant);

        // When we call the overlayDetails method with an existing item
        List<ContentDetail> toOverlay = new ArrayList<>();
        toOverlay.add(descendant);
        detail.overlayDetails(toOverlay);

        // Then nothing is added.
        assertTrue(child.children.size() == 1);
    }

    @Test
    public void containsChildShouldReturnTrueIfChildExists() {

        // Given a content detail instance with a child
        ContentDetail detail = new ContentDetail("base content", "/", "home");
        ContentDetail child = new ContentDetail("child content", "/child", "article");
        detail.children = new ArrayList<>();
        detail.children.add(child);

        // When we call the contains child method with the child instance.
        boolean containChild = detail.containsChild(child);

        // Then the result is true
        assertTrue(containChild);
    }

    @Test
    public void containsChildShouldReturnFalseIfChildIsNotFound() {

        // Given a content detail instance with a child
        ContentDetail detail = new ContentDetail("base content", "/", "home");
        ContentDetail child = new ContentDetail("child content", "/child", "article");
        detail.children = new ArrayList<>();

        // When we call the contains child method with the child instance.
        boolean containChild = detail.containsChild(child);

        // Then the result is true
        assertFalse(containChild);
    }

    @Test
    public void containsChildShouldReturnFalseIfChildrenIsNull() {

        // Given a content detail instance with a child
        ContentDetail detail = new ContentDetail("base content", "/", "home");
        ContentDetail child = new ContentDetail("child content", "/child", "article");

        // When we call the contains child method with the child instance.
        boolean containChild = detail.containsChild(child);

        // Then the result is true
        assertFalse(containChild);
    }

    @Test
    public void containsDescendantShouldReturnTrueIfFound() {

        // Given a content detail instance with a descendant
        ContentDetail detail = new ContentDetail("base content", "/", "home");
        ContentDetail child = new ContentDetail("child content", "/child", "article");
        ContentDetail descendant = new ContentDetail("descendant content", "/child/descendant", "bulletin");
        detail.children = new ArrayList<>();
        detail.children.add(child);
        child.children = new ArrayList<>();
        child.children.add(descendant);


        // When we call the containsDescendant method with the descendant instance.
        boolean containsDescendant = detail.containsDescendant(descendant);

        // Then the result is true
        assertTrue(containsDescendant);
    }

    @Test
    public void containsDescendantShouldReturnFalseIfNotFound() {

        // Given a content detail instance without adding a descendant.
        ContentDetail detail = new ContentDetail("base content", "/", "home");
        ContentDetail child = new ContentDetail("child content", "/child", "article");
        ContentDetail descendant = new ContentDetail("descendant content", "/child/descendant", "bulletin");
        detail.children = new ArrayList<>();
        detail.children.add(child);
        child.children = new ArrayList<>();

        // When we call the containsDescendant method with the descendant instance.
        boolean containsDescendant = detail.containsDescendant(descendant);

        // Then the result is false
        assertFalse(containsDescendant);
    }

    @Test
    public void cloneShouldCreateCopyIncludingChildNodes() {

        // Given a content detail instance with a child
        ContentDetail detail = new ContentDetail("base content", "/", "home");
        ContentDetail child = new ContentDetail("child content", "/child", "article");
        detail.children = new ArrayList<>();
        detail.children.add(child);

        // When we clone it and add an overlay to the clone.
        ContentDetail clone = detail.clone();

        ContentDetail descendant = new ContentDetail("descendant content", "/child/descendant", "bulletin");
        List<ContentDetail> toOverlay = new ArrayList<>();
        toOverlay.add(descendant);
        clone.overlayDetails(toOverlay);

        // Then the item is added into the tree
        assertTrue(clone.containsDescendant(descendant));
        assertFalse(detail.containsDescendant(descendant));
    }
}
