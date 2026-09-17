package com.ronreynolds.android.util;

public interface StringSupplier {   // since Supplier<String> isn't available at API-23
    String get();
}
