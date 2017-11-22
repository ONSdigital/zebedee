package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import com.google.common.collect.ImmutableList;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Abstract custom {@link Collector} - If you want to use {@link java.util.stream.Stream} to iterate over the custom
 * collection types in the code base i.e. {@link UserList}.
 */
public abstract class CustomListCollector<T, R> implements Collector<T, ImmutableList.Builder<T>, R> {

    @Override
    public Supplier<ImmutableList.Builder<T>> supplier() {
        return ImmutableList::builder;
    }

    @Override
    public BiConsumer<ImmutableList.Builder<T>, T> accumulator() {
        return (builder, item) -> {
            if (item != null) {
                builder.add(item);
            }
        };
    }

    @Override
    public BinaryOperator<ImmutableList.Builder<T>> combiner() {
        return (left, right) -> {
            left.addAll(right.build());
            return left;
        };
    }

    @Override
    public abstract Function<ImmutableList.Builder<T>, R> finisher();

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }
}
