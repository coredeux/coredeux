package com.coredeux.core.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Java reflection helper for resolving a component's first generic type
 * argument for a framework contract.
 */
public final class CoredeuxGenericTypeResolver {

    private CoredeuxGenericTypeResolver() {
    }

    public static Class<?> resolveFirstGeneric(Class<?> implementationType, Class<?> contractType) {
        if (implementationType == null || contractType == null) {
            return null;
        }
        Class<?> fromInterfaces = resolveFromTypes(implementationType.getGenericInterfaces(), contractType);
        if (fromInterfaces != null) {
            return fromInterfaces;
        }
        Type superclass = implementationType.getGenericSuperclass();
        if (superclass instanceof ParameterizedType parameterizedSuperclass) {
            Class<?> resolved = resolveFromType(parameterizedSuperclass, contractType);
            if (resolved != null) {
                return resolved;
            }
        }
        Class<?> parent = implementationType.getSuperclass();
        if (parent == null || Object.class.equals(parent)) {
            return null;
        }
        return resolveFirstGeneric(parent, contractType);
    }

    private static Class<?> resolveFromTypes(Type[] types, Class<?> contractType) {
        for (Type type : types) {
            if (type instanceof ParameterizedType parameterizedType) {
                Class<?> resolved = resolveFromType(parameterizedType, contractType);
                if (resolved != null) {
                    return resolved;
                }
            } else if (type instanceof Class<?> rawType) {
                Class<?> resolved = resolveFirstGeneric(rawType, contractType);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return null;
    }

    private static Class<?> resolveFromType(ParameterizedType type, Class<?> contractType) {
        Type rawType = type.getRawType();
        if (rawType instanceof Class<?> rawClass && contractType.equals(rawClass)) {
            Type argument = type.getActualTypeArguments()[0];
            if (argument instanceof Class<?> argumentClass) {
                return argumentClass;
            }
            if (argument instanceof ParameterizedType parameterizedArgument
                    && parameterizedArgument.getRawType() instanceof Class<?> rawArgumentClass) {
                return rawArgumentClass;
            }
        }
        if (rawType instanceof Class<?> rawClass) {
            return resolveFirstGeneric(rawClass, contractType);
        }
        return null;
    }
}
