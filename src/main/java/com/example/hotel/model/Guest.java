package com.example.hotel.model;

import java.util.regex.Pattern;

public final class Guest {
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?[0-9]{7,15}");
    private final String name;
    private final String phone;

    public Guest(String name, String phone) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Guest name is required");
        }
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Phone must be 7–15 digits, optionally starting with +");
        }
        this.name = name.trim();
        this.phone = phone;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
}
