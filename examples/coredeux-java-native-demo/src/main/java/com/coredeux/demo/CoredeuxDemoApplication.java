package com.coredeux.demo;

import com.coredeux.examples.nativejava.CoredeuxNativeDemoServer;
import com.coredeux.examples.nativejava.CoredeuxNativeRuntime;

/**
 * Native entrypoint for the demo application.
 */
public final class CoredeuxDemoApplication {

    private CoredeuxDemoApplication() {
    }

    public static void main(String[] args) throws Exception {
        int port = intValue(env("COREDEUX_NATIVE_PORT"), 8080);
        CoredeuxNativeRuntime runtime = CoredeuxNativeRuntime.create();
        CoredeuxNativeDemoServer server = CoredeuxNativeDemoServer.start(runtime, port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
            runtime.close();
        }, "coredeux-native-demo-shutdown"));

        System.out.println("Coredeux native demo started on http://localhost:" + server.port());
        Thread.currentThread().join();
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value;
    }

    private static int intValue(String value, int defaultValue) {
        try {
            return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
