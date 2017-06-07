package com.github.onsdigital.zebedee.user.model;

import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import com.github.onsdigital.zebedee.util.CustomListCollector;
import com.google.common.collect.ImmutableList;

import java.util.function.Function;

/**
 * Created by dave on 30/05/2017.
 */
public class UserListCollector extends CustomListCollector<User, UserList> {

    @Override
    public Function<ImmutableList.Builder<User>, UserList> finisher() {
        return (builder) -> {
            UserList userList = new UserList();
            userList.addAll(builder.build());
            return userList;
        };
    }
}
