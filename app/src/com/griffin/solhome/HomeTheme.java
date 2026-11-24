package com.griffin.solhome;

enum HomeTheme {
    CLASSIC("classic", "Classic", null),
    PARK_RAT("park-rat", "Park Rat", "park-rat.jpg"),
    FIRST_CHAIR("first-chair", "First Chair", "first-chair.jpg"),
    PORCHLIGHT("porchlight", "Porchlight", null),
    RIVER_CITY("river-city", "River City", "river-city.jpg"),
    GTA_VI("gta-vi", "GTA VI", "gta-vi-jason-lucia.jpg");

    static final String PREFERENCE = "home_theme", LEGACY_PARK_RAT = "park_rat_home";
    static final String GTA_INSET = "gta-vi-tuner.jpg";
    final String id, label, artwork;

    HomeTheme(String id, String label, String artwork) {
        this.id = id;
        this.label = label;
        this.artwork = artwork;
    }

    static HomeTheme saved(String id, boolean legacyParkRat) {
        if (id == null) return legacyParkRat ? PARK_RAT : CLASSIC;
        for (HomeTheme theme : values()) if (theme.id.equals(id)) return theme;
        return CLASSIC;
    }
}
