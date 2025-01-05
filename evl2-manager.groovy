/**
 *  EVL2 Complete Manager App
 *  Advanced management for EVL2/LCD1 controllers
 *  
 *  Copyright 2025 Wayne Pirtle
 *  Version: 1.0.0
 *  Build Date: 2025-01-05 09:30:00
 *
 *  Licensed under the Apache License, Version 2.0
 *
 *  Changes:
 *  1.0.0 - Initial release with comprehensive EVL2 support
 */

definition(
    name: "EVL2 Manager",
    namespace: "odwp",
    author: "Wayne Pirtle",
    description: "Complete management solution for EVL2/LCD1 controllers",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    version: "1.0.0"
)

import groovy.transform.Field

@Field static final String VERSION = "1.0.0"

// Z-Wave spec constants
@Field static final Map BUTTON_TYPES = [
    SCENE_MOMENTARY: 0x00,
    SCENE_TOGGLE: 0x01,
    BASIC_TOGGLE: 0x02,
    THERMOSTAT: 0x03,
    PRIVACY: 0x04,
    HOUSEKEEPING: 0x05,
    SCENE_BASIC: 0x06
]

@Field static final Map FONT_STYLES = [
    NORMAL: 0x00,
    CONDENSED: 0x01,
    INVERTED: 0x02
]

@Field static final Map TEXT_ALIGN = [
    LEFT: 0x00,
    CENTER: 0x01,
    RIGHT: 0x02
]

@Field static final Integer MAX_PAGES = 9
@Field static final Integer MAX_BUTTONS = 15
@Field static final Integer MAX_LABEL_LENGTH = 16
@Field static final Integer STATE_VERSION = 1
