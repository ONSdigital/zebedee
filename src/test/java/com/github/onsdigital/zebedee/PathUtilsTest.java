package com.github.onsdigital.zebedee;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PathUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void shouldCleanString() {

		// Given
		String string = "name.é+!@#$%^&*(){}][/=?+-_\\|;:`~!'\",<>";

		// When
		String result = PathUtils.toFilename(string);

		// Then
		assertEquals("name.é$_", result);
	}

	@Test
	public void shouldAbbreviateString() {

		// Given
		String string = RandomStringUtils.random(500, "aoeuidhtns");

		// When
		String result = PathUtils.toFilename(string);

		// Then
		assertEquals(PathUtils.MAX_LENGTH, result.length());
		assertEquals(StringUtils.substring(string, 0, 127),
				StringUtils.substring(result, 0, 127));
		assertEquals(StringUtils.substring(string, -127),
				StringUtils.substring(result, -127));
	}

}
