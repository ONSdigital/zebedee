package com.github.onsdigital.zebedee.service;

/**
 * Many of the legacy services are accessed via {@link com.github.onsdigital.zebedee.Zebedee} static methods. This
 * makes testing components that use these services difficult as there is additional (messy) work
 * required to set up the test.
 * <br/><br/>
 * Using this interface the caller can create an inline class variable implementation which encapsulates the
 * invocation of the static methods to obtain / return the service they require.
 * <br><br>Example:<br/><br/>
 * <i>private ServiceSupplier&lt;UsersService&gt; usersServiceSupplier = () -> Root.zebedee.getUsersService();
 * </i><br/><br/>
 * Now when the class needs the access to the user service you simply call:
 * <br/><br/><b><i> usersServiceSupplier.getService()</i></b><br/><br/>
 * The advantage this offers is that the class variable can be replaced with mock or stub when running a unit test,
 * giving control of the behaviour to the tester.
 */
@FunctionalInterface
public interface ServiceSupplier<T> {

    /**
     * @return the service.
     */
    T getService();
}
