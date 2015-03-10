package com.github.onsdigital.zebedee.json;

import java.util.List;

import com.github.onsdigital.zebedee.Collection;

/**
 * This cd ..
 * 
 * @author david
 *
 */
public class CollectionDescription {
	/** The readable name of this {@link Collection}. */
	public String name;
	/**
	 * The date-time when this {@link Collection} should be published (if it has
	 * a publish date).
	 */
	public String publishDate;

	public List<String> inProgressUris;
	public List<String> approvedUris;
}
