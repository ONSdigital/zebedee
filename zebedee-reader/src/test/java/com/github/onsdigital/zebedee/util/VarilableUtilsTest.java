package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.util.VariableUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by bren on 31/07/15.
 */
public class VarilableUtilsTest {

    @Test
    public void testGetSystemProperty() {
        final String TEST_VALUE = "testvalue";
        final String TEST_KEY = "testkey";
        System.setProperty(TEST_KEY,TEST_VALUE);
        String result = VariableUtils.getVariableValue(TEST_KEY);
        assertEquals(TEST_VALUE, result);
    }

    @Test
    public void testGetEnvVariable() {
        /*Setting environment varilable within java is not possible ( would require running a process on java which makes code non-portable ).
         That is why there is not easy way to test this, Is there a default environment variable that will always be there ?*/
    }

    @Test
    public void testGetVariable() {
        String variableValue = VariableUtils.getVariableValue("user.dir");
        assertNotNull(variableValue);
    }
}
