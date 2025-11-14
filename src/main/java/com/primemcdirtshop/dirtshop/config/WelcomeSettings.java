package com.primemcdirtshop.dirtshop.config;

import java.util.List;

public record WelcomeSettings(boolean enabled, String title, String subtitle, List<String> messages) {
    public WelcomeSettings {
        if (messages == null) {
            messages = List.of();
        }
    }
}
