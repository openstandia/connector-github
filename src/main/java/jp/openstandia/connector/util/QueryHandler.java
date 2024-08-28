package jp.openstandia.connector.util;

@FunctionalInterface
public interface QueryHandler<T> {
    boolean handle(T arg);
}