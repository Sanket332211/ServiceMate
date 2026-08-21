package com.example.carservice.entity;

/**
 * ServiceType
 *
 * Defines the fixed system-supported car service packages in ServiceMate Phase 4
 * along with their display labels and fixed estimated base prices.
 */
public enum ServiceType {
    GENERAL_SERVICE("General Service", 1499),
    OIL_CHANGE("Oil Change", 999),
    AC_SERVICE("AC Service & Inspection", 1299),
    BRAKE_SERVICE("Brake Service & Fluid", 1799),
    BATTERY_SERVICE("Battery Inspection & Care", 499);

    private final String displayName;
    private final int basePrice;

    ServiceType(String displayName, int basePrice) {
        this.displayName = displayName;
        this.basePrice = basePrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBasePrice() {
        return basePrice;
    }
}
