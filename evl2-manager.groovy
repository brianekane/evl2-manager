/**
 *  EVL2 Complete Manager App
 *  Advanced management for EVL2/LCD1 controllers
 *  
 *  Copyright 2025 Brian Kane
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


// ... previous code remains the same ...

preferences {
    page(name: "mainPage")
    page(name: "pageSetupPage")
    page(name: "buttonConfigPage")
    page(name: "displaySettingsPage")
    page(name: "advancedSettingsPage")
}

void logDebug(String msg) {
    if (settings.enableDebug) log.debug "${app.name}: ${msg}"
}

void logInfo(String msg) {
    if (settings.enableDesc) log.info "${app.name}: ${msg}"
}

void logError(String msg) { log.error "${app.name}: ${msg}" }

def mainPage() {
    dynamicPage(name: "mainPage", title: "EVL2 Controller Manager", install: true, uninstall: true) {
        section("Select Controller") {
            input "controller", "capability.pushableButton", 
                title: "Select EVL2 Controller", 
                required: true
                
            input "enableDebug", "bool",
                title: "Enable debug logging",
                defaultValue: true
                
            input "enableDesc", "bool",
                title: "Enable description logging",
                defaultValue: true
        }
        
        if (controller) {
            section("Navigation") {
                input "currentPage", "number",
                    title: "Active Page",
                    range: "1..${settings.totalPages ?: 1}",
                    defaultValue: 1,
                    required: true
                
                input "returnHome", "button",
                    title: "Return to Home Page"
            }
            
            section("Configuration") {
                href "pageSetupPage", 
                    title: "Page Management",
                    description: "Set up pages and navigation"
                
                href "buttonConfigPage",
                    title: "Button Configuration",
                    description: "Configure buttons for current page"
                
                href "displaySettingsPage",
                    title: "Display Settings",
                    description: "Configure screen appearance"
                
                href "advancedSettingsPage",
                    title: "Advanced Settings",
                    description: "Special features and Z-Wave settings"
            }
        }
    }
}


def pageSetupPage() {
    dynamicPage(name: "pageSetupPage", title: "Page Management", nextPage: "mainPage") {
        section("General Page Setup") {
            input "totalPages", "number",
                title: "Total Number of Pages",
                range: "1..${MAX_PAGES}",
                defaultValue: 1,
                required: true
                
            input "homePage", "number",
                title: "Home Page Number",
                range: "1..${settings.totalPages ?: 1}",
                defaultValue: 1,
                required: true
                
            input "homeTimeout", "number",
                title: "Return to Home After (seconds, 0=never)",
                defaultValue: 0,
                required: true
        }
        
        // Individual Page Configuration
        for (def i = 1; i <= (settings.totalPages ?: 1); i++) {
            section("Page ${i} Setup", hideable: true) {
                input "page${i}Name", "text",
                    title: "Page Name",
                    defaultValue: "Page ${i}",
                    required: false
                    
                input "page${i}Timeout", "number",
                    title: "Page Timeout (seconds, 0=never)",
                    defaultValue: 0,
                    required: false
                    
                input "page${i}TimeoutAction", "enum",
                    title: "On Timeout",
                    options: [
                        "none": "Do Nothing",
                        "home": "Return Home",
                        "page": "Go to Specific Page"
                    ],
                    defaultValue: "none",
                    required: true
                    
                if (settings["page${i}TimeoutAction"] == "page") {
                    input "page${i}TimeoutPage", "number",
                        title: "Go to Page",
                        range: "1..${settings.totalPages}",
                        required: true
                }
                
                paragraph "Page ${i} Quick Actions:"
                input "previewPage${i}", "button", 
                    title: "Preview Page ${i}"
                input "testPage${i}", "button",
                    title: "Test Navigation to Page ${i}"
            }
        }
    }
}

// Handler for page preview/test buttons
def appButtonHandler(btn) {
    switch(btn) {
        case ~/previewPage\d+/:
            def pageNum = btn.substring(10) as Integer
            previewPage(pageNum)
            break
            
        case ~/testPage\d+/:
            def pageNum = btn.substring(8) as Integer
            testPageNavigation(pageNum)
            break
            
        case "returnHome":
            switchToPage(settings.homePage ?: 1)
            break
    }
}

private void previewPage(Integer pageNum) {
    logDebug "Previewing page ${pageNum} configuration"
    def config = getPageConfig(pageNum)
    logInfo "Page ${pageNum}: ${config}"
}

private void testPageNavigation(Integer pageNum) {
    logDebug "Testing navigation to page ${pageNum}"
    switchToPage(pageNum)
}

private Map getPageConfig(Integer pageNum) {
    return [
        name: settings["page${pageNum}Name"],
        timeout: settings["page${pageNum}Timeout"],
        timeoutAction: settings["page${pageNum}TimeoutAction"],
        timeoutPage: settings["page${pageNum}TimeoutPage"]
    ]
}
def buttonConfigPage() {
    def currentPageNum = settings.currentPage ?: 1
    
    dynamicPage(name: "buttonConfigPage", title: "Button Configuration - Page ${currentPageNum}", nextPage: "mainPage") {
        section("Button Configuration") {
            paragraph "Configuring buttons for Page ${currentPageNum}: ${settings["page${currentPageNum}Name"]}"
            
            for (def i = 1; i <= MAX_BUTTONS; i++) {
                section("Button ${i} Settings", hideable: true, hidden: true) {
                    input "page${currentPageNum}Btn${i}Enable", "bool",
                        title: "Configure Button ${i}",
                        defaultValue: false
                        
                    if (settings["page${currentPageNum}Btn${i}Enable"]) {
                        input "page${currentPageNum}Btn${i}Label", "text",
                            title: "Button Label",
                            maxLength: MAX_LABEL_LENGTH,
                            required: false
                            
                        input "page${currentPageNum}Btn${i}Type", "enum",
                            title: "Button Function",
                            options: [
                                "scene_momentary": "Scene - Momentary",
                                "scene_toggle": "Scene - Toggle",
                                "basic_toggle": "Basic Set Toggle",
                                "privacy": "Privacy/DND Mode",
                                "housekeeping": "Housekeeping Mode",
                                "scene_basic": "Scene/Basic Toggle",
                                "page_jump": "Switch to Page",
                                "home": "Return Home"
                            ],
                            defaultValue: "scene_momentary",
                            required: true
                            
                        if (settings["page${currentPageNum}Btn${i}Type"] in ["scene_momentary", "scene_toggle", "scene_basic"]) {
                            input "page${currentPageNum}Btn${i}SceneId", "number",
                                title: "Scene ID (1-255)",
                                range: "1..255",
                                required: true
                        }
                        
                        if (settings["page${currentPageNum}Btn${i}Type"] == "page_jump") {
                            input "page${currentPageNum}Btn${i}TargetPage", "number",
                                title: "Target Page",
                                range: "1..${settings.totalPages}",
                                required: true
                        }
                        
                        // Font and display options
                        input "page${currentPageNum}Btn${i}Font", "enum",
                            title: "Font Style",
                            options: [
                                "normal": "Normal",
                                "condensed": "Condensed",
                                "inverted": "Inverted"
                            ],
                            defaultValue: "normal"
                            
                        input "page${currentPageNum}Btn${i}Align", "enum",
                            title: "Text Alignment",
                            options: [
                                "left": "Left",
                                "center": "Center",
                                "right": "Right"
                            ],
                            defaultValue: "center"
                            
                        input "page${currentPageNum}Btn${i}DoubleLine", "bool",
                            title: "Enable Double Line Text",
                            defaultValue: false
                            
                        if (settings["page${currentPageNum}Btn${i}DoubleLine"]) {
                            paragraph "Use \\r for line break in label"
                        }
                    }
                }
            }
        }
        
        section("Quick Actions") {
            input "previewButtons", "button",
                title: "Preview All Button Settings"
                
            input "updateLabels", "button",
                title: "Update Button Labels"
                
            input "clearPage", "button",
                title: "Clear All Page Settings"
        }
        
        section {
            paragraph "Note: Changes are saved when returning to main page"
        }
    }
}

private void handleButtonConfigActions(btn) {
    switch(btn) {
        case "previewButtons":
            previewButtonConfig()
            break
            
        case "updateLabels":
            updateButtonLabels()
            break
            
        case "clearPage":
            clearPageConfig()
            break
    }
}

private void previewButtonConfig() {
    def pageNum = settings.currentPage ?: 1
    logDebug "Previewing button configuration for page ${pageNum}"
    
    def config = [:]
    for (def i = 1; i <= MAX_BUTTONS; i++) {
        if (settings["page${pageNum}Btn${i}Enable"]) {
            config["button${i}"] = [
                label: settings["page${pageNum}Btn${i}Label"],
                type: settings["page${pageNum}Btn${i}Type"],
                sceneId: settings["page${pageNum}Btn${i}SceneId"],
                font: settings["page${pageNum}Btn${i}Font"],
                align: settings["page${pageNum}Btn${i}Align"],
                doubleLine: settings["page${pageNum}Btn${i}DoubleLine"]
            ]
        }
    }
    
    logInfo "Page ${pageNum} button configuration: ${config}"
}
def displaySettingsPage() {
    dynamicPage(name: "displaySettingsPage", title: "Display Settings", nextPage: "mainPage") {
        section("Screen Settings") {
            input "backlightLevel", "number",
                title: "Backlight Level (1-20)",
                range: "1..20",
                defaultValue: 15
                
            input "backlightOffLevel", "number",
                title: "Backlight Off Level (0-19)",
                range: "0..19",
                defaultValue: 0
                
            input "displayContrast", "number",
                title: "Display Contrast (5-20)",
                range: "5..20",
                defaultValue: 14
                
            input "displayTimeout", "number",
                title: "Display Timeout (seconds)",
                defaultValue: 15
        }
        
        section("RGB Backlight Control") {
            input "enableRGB", "bool",
                title: "Enable RGB Control",
                defaultValue: false
                
            if (settings.enableRGB) {
                input "redLevel", "number",
                    title: "Red Level (0-100)",
                    range: "0..100",
                    defaultValue: 100
                    
                input "greenLevel", "number",
                    title: "Green Level (0-100)",
                    range: "0..100",
                    defaultValue: 100
                    
                input "blueLevel", "number",
                    title: "Blue Level (0-100)",
                    range: "0..100",
                    defaultValue: 100
            }
        }
        
        section("Screen Orientation") {
            input "screenRotated", "bool",
                title: "Rotate Screen 180°",
                defaultValue: false
        }
        
        section("Text Display") {
            input "defaultFont", "enum",
                title: "Default Font Style",
                options: [
                    "normal": "Normal",
                    "condensed": "Condensed"
                ],
                defaultValue: "normal"
                
            input "defaultAlignment", "enum",
                title: "Default Text Alignment",
                options: [
                    "left": "Left",
                    "center": "Center",
                    "right": "Right"
                ],
                defaultValue: "center"
        }
        
        section("Quick Actions") {
            input "updateDisplay", "button",
                title: "Update Display Settings"
                
            input "testBacklight", "button",
                title: "Test Backlight Levels"
                
            if (settings.enableRGB) {
                input "testRGB", "button",
                    title: "Test RGB Settings"
            }
        }
    }
}

// Display configuration methods
private void updateDisplaySettings() {
    logDebug "Updating display settings..."
    
    try {
        // Basic display settings
        sendDisplayCommand(createDisplayCommand([
            backlightLevel: settings.backlightLevel,
            backlightOffLevel: settings.backlightOffLevel,
            contrast: settings.displayContrast,
            timeout: settings.displayTimeout,
            orientation: settings.screenRotated ? 1 : 0
        ]))
        
        // RGB settings if enabled
        if (settings.enableRGB) {
            sendRGBCommand(createRGBCommand([
                red: settings.redLevel,
                green: settings.greenLevel,
                blue: settings.blueLevel
            ]))
        }
        
        logInfo "Display settings updated successfully"
        
    } catch (e) {
        logError "Error updating display settings: ${e.message}"
    }
}

private Map createDisplayCommand(Map params) {
    // Convert settings to Z-Wave configuration parameters
    return [
        cmd: "configuration",
        param: [
            [number: 21, value: params.backlightLevel],
            [number: 22, value: params.backlightOffLevel],
            [number: 25, value: params.contrast],
            [number: 20, value: params.timeout],
            [number: 26, value: params.orientation]
        ]
    ]
}

private Map createRGBCommand(Map params) {
    return [
        cmd: "configuration",
        param: [
            [number: 29, value: params.red],
            [number: 30, value: params.blue],
            [number: 31, value: params.green]
        ]
    ]
}

def handleDisplayButton(String btn) {
    switch(btn) {
        case "updateDisplay":
            updateDisplaySettings()
            break
            
        case "testBacklight":
            testBacklightLevels()
            break
            
        case "testRGB":
            testRGBSettings()
            break
    }
}

private void testBacklightLevels() {
    logDebug "Testing backlight levels..."
    
    // Schedule a sequence of backlight tests
    def levels = [20, 15, 10, 5, 1]
    levels.eachWithIndex { level, index ->
        runIn(index * 2, 'setBacklightLevel', [data: [level: level]])
    }
    
    // Return to configured level after test
    runIn(levels.size() * 2, 'updateDisplaySettings')
}

private void setBacklightLevel(data) {
    logDebug "Setting test backlight level: ${data.level}"
    sendDisplayCommand(createDisplayCommand([backlightLevel: data.level]))
}

private void testRGBSettings() {
    if (!settings.enableRGB) return
    
    logDebug "Testing RGB settings..."
    
    // Test sequence: Red, Green, Blue, White
    def colors = [
        [red: 100, green: 0, blue: 0],
        [red: 0, green: 100, blue: 0],
        [red: 0, green: 0, blue: 100],
        [red: 100, green: 100, blue: 100]
    ]
    
    colors.eachWithIndex { color, index ->
        runIn(index * 2, 'setRGBColor', [data: color])
    }
    
    // Return to configured colors after test
    runIn(colors.size() * 2, 'updateDisplaySettings')
}

private void setRGBColor(data) {
    logDebug "Setting test RGB color: ${data}"
    sendRGBCommand(createRGBCommand(data))
}

def advancedSettingsPage() {
    dynamicPage(name: "advancedSettingsPage", title: "Advanced Settings", nextPage: "mainPage") {
        section("Z-Wave Configuration") {
            input "zwaveConfig", "enum",
                title: "Z-Wave Association Mode",
                options: [
                    "group": "Use Association Groups (recommended)",
                    "direct": "Direct Scene Control"
                ],
                defaultValue: "group"
                
            input "powerLevel", "enum",
                title: "Transmission Power Level",
                options: [
                    "normal": "Normal Power",
                    "minus1": "-1dB",
                    "minus2": "-2dB",
                    "minus3": "-3dB",
                    "minus4": "-4dB",
                    "minus5": "-5dB"
                ],
                defaultValue: "normal"
        }
        
        section("Temperature Display") {
            input "enableTemp", "bool",
                title: "Enable Temperature Display",
                defaultValue: false
                
            if (settings.enableTemp) {
                input "tempSensor", "capability.temperatureMeasurement",
                    title: "Temperature Sensor",
                    required: false
                    
                input "tempUpdateInterval", "number",
                    title: "Update Interval (seconds)",
                    defaultValue: 60,
                    required: true
                    
                input "tempFormat", "enum",
                    title: "Temperature Format",
                    options: [
                        "F": "Fahrenheit",
                        "C": "Celsius"
                    ],
                    defaultValue: "F"
            }
        }
        
        section("Button Press Handling") {
            input "buttonHoldTime", "number",
                title: "Hold Time for Long Press (ms)",
                defaultValue: 1000
                
            input "buttonRepeatDelay", "number",
                title: "Repeat Delay (ms)",
                defaultValue: 500
        }
        
        section("Scene Control") {
            input "sceneTimeout", "number",
                title: "Scene Activation Timeout (seconds)",
                defaultValue: 0
                
            input "dimmerRampRate", "number",
                title: "Dimming Duration (seconds)",
                defaultValue: 0
        }
    }
}

// Z-Wave Command Handling
private void sendZWaveCommand(Map cmd) {
    logDebug "Sending Z-Wave command: ${cmd}"
    
    try {
        def zwaveCmd
        switch(cmd.cmd) {
            case "configuration":
                zwaveCmd = zwave.configurationV1.configurationSet(
                    parameterNumber: cmd.param.number,
                    size: 1,
                    configurationValue: [cmd.param.value]
                )
                break
                
            case "association":
                zwaveCmd = zwave.associationV2.associationSet(
                    groupingIdentifier: cmd.group,
                    nodeId: cmd.nodes
                )
                break
                
            case "scene":
                zwaveCmd = zwave.sceneActivationV1.sceneActivationSet(
                    sceneId: cmd.scene,
                    dimmingDuration: cmd.duration ?: 0
                )
                break
                
            case "basic":
                zwaveCmd = zwave.basicV1.basicSet(
                    value: cmd.value
                )
                break
                
            case "indicator":
                zwaveCmd = zwave.indicatorV1.indicatorSet(
                    value: cmd.value
                )
                break
                
            default:
                logError "Unknown command type: ${cmd.cmd}"
                return
        }
        
        if (zwaveCmd) {
            def encapCmd = zwaveSecureEncap(zwaveCmd)
            sendHubCommand(new hubitat.device.HubAction(encapCmd, hubitat.device.Protocol.ZWAVE))
        }
        
    } catch (e) {
        logError "Error sending Z-Wave command: ${e.message}"
    }
}

private String zwaveSecureEncap(physicalgraph.zwave.Command cmd) {
    logDebug "Encapsulating command: ${cmd}"
    def secureCmd = zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd)
    return secureCmd.format()
}

// Scene Control Methods
private void handleSceneActivation(buttonConfig) {
    if (!buttonConfig.sceneId) return
    
    def sceneCmd = [
        cmd: "scene",
        scene: buttonConfig.sceneId,
        duration: settings.dimmerRampRate
    ]
    
    sendZWaveCommand(sceneCmd)
    
    if (settings.sceneTimeout > 0) {
        runIn(settings.sceneTimeout, "handleSceneTimeout", [data: [sceneId: buttonConfig.sceneId]])
    }
}

private void handleSceneTimeout(data) {
    logDebug "Scene timeout for scene ${data.sceneId}"
    // Add scene timeout handling if needed
}

// Button Event Processing
private void processButtonEvent(evt) {
    def buttonNumber = evt.value as Integer
    def buttonConfig = getButtonConfig(buttonNumber)
    
    if (!buttonConfig?.enabled) return
    
    switch(buttonConfig.type) {
        case "scene_momentary":
            handleSceneActivation(buttonConfig)
            break
            
        case "scene_toggle":
            handleSceneToggle(buttonConfig)
            break
            
        case "basic_toggle":
            handleBasicToggle(buttonConfig)
            break
            
        case "privacy":
            handlePrivacyMode(buttonConfig)
            break
            
        case "housekeeping":
            handleHousekeepingMode(buttonConfig)
            break
            
        case "scene_basic":
            handleSceneBasicToggle(buttonConfig)
            break
    }
}

// Core Functionality Methods
def installed() {
    logDebug "Installed version ${VERSION}"
    initialize()
}

def updated() {
    logDebug "Updated to version ${VERSION}"
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    logDebug "Initializing..."
    
    // Initialize state if needed
    if (!state.version || state.version < STATE_VERSION) {
        initializeState()
    }
    
    // Subscribe to events
    subscribe(controller, "pushed", "buttonEventHandler")
    if (settings.enableTemp && settings.tempSensor) {
        subscribe(tempSensor, "temperature", "temperatureEventHandler")
        scheduleTemperatureUpdates()
    }
    
    // Configure display settings
    updateDisplaySettings()
    
    // Initialize button configurations
    initializeButtons()
    
    // Set initial page
    switchToPage(settings.currentPage ?: 1)
}

private void initializeState() {
    logDebug "Initializing state storage..."
    state.clear()
    state.version = STATE_VERSION
    state.pages = [:]
    state.buttonStates = [:]
    state.currentPage = settings.currentPage ?: 1
    state.lastUpdate = now()
}

private void initializeButtons() {
    logDebug "Initializing button configurations..."
    
    def pageNum = state.currentPage
    if (!state.pages["page${pageNum}"]) {
        state.pages["page${pageNum}"] = [:]
    }
    
    def pageConfig = state.pages["page${pageNum}"]
    pageConfig.buttons = [:]
    
    // Configure each button
    for (def i = 1; i <= MAX_BUTTONS; i++) {
        if (settings["page${pageNum}Btn${i}Enable"]) {
            pageConfig.buttons["button${i}"] = [
                enabled: true,
                label: settings["page${pageNum}Btn${i}Label"],
                type: settings["page${pageNum}Btn${i}Type"],
                sceneId: settings["page${pageNum}Btn${i}SceneId"],
                font: settings["page${pageNum}Btn${i}Font"],
                align: settings["page${pageNum}Btn${i}Align"],
                doubleLine: settings["page${pageNum}Btn${i}DoubleLine"]
            ]
        }
    }
    
    // Save configuration
    state.pages["page${pageNum}"] = pageConfig
}

def buttonEventHandler(evt) {
    def buttonNumber = evt.value as Integer
    logDebug "Button ${buttonNumber} event on page ${state.currentPage}"
    
    // Get button configuration
    def buttonConfig = getButtonConfig(buttonNumber)
    if (!buttonConfig?.enabled) {
        logDebug "Button ${buttonNumber} not configured"
        return
    }
    
    // Process button press
    processButtonPress(buttonNumber, buttonConfig)
}

private void processButtonPress(buttonNumber, config) {
    logDebug "Processing button ${buttonNumber} press with config: ${config}"
    
    try {
        switch(config.type) {
            case "scene_momentary":
                handleSceneActivation([sceneId: config.sceneId])
                break
                
            case "scene_toggle":
                handleSceneToggle(buttonNumber, config)
                break
                
            case "basic_toggle":
                handleBasicToggle(buttonNumber)
                break
                
            case "privacy":
                handlePrivacyMode()
                break
                
            case "housekeeping":
                handleHousekeepingMode()
                break
                
            case "scene_basic":
                handleSceneBasicToggle(buttonNumber, config)
                break
                
            case "page_jump":
                switchToPage(config.targetPage)
                break
                
            case "home":
                switchToPage(settings.homePage ?: 1)
                break
        }
        
        // Update button state if needed
        updateButtonState(buttonNumber, config)
        
    } catch (e) {
        logError "Error processing button press: ${e.message}"
    }
}

private void switchToPage(pageNumber) {
    if (!pageNumber || pageNumber < 1 || pageNumber > (settings.totalPages ?: 1)) {
        logError "Invalid page number: ${pageNumber}"
        return
    }
    
    try {
        logDebug "Switching to page ${pageNumber}"
        
        // Cancel any existing timeouts
        unschedule("handlePageTimeout")
        
        // Update state
        state.currentPage = pageNumber
        
        // Update display
        updatePageDisplay(pageNumber)
        
        // Set new timeout if configured
        def timeout = settings["page${pageNumber}Timeout"] ?: settings.globalTimeout ?: 0
        if (timeout > 0) {
            runIn(timeout, "handlePageTimeout")
        }
        
    } catch (e) {
        logError "Error switching to page ${pageNumber}: ${e.message}"
    }
}

// Button Mode Handling Methods
private void handleSceneToggle(buttonNumber, config) {
    logDebug "Handling scene toggle for button ${buttonNumber}"
    
    def currentState = getButtonState(buttonNumber)
    def newState = !currentState
    
    def sceneCmd = [
        cmd: "scene",
        scene: config.sceneId,
        duration: settings.dimmerRampRate ?: 0
    ]
    
    if (newState) {
        sendZWaveCommand(sceneCmd)
    } else {
        // For OFF state, either send scene off or basic set
        if (config.offSceneId) {
            sceneCmd.scene = config.offSceneId
            sendZWaveCommand(sceneCmd)
        } else {
            sendZWaveCommand([cmd: "basic", value: 0])
        }
    }
    
    updateButtonState(buttonNumber, newState)
    updateButtonDisplay(buttonNumber, newState)
}

private void handleBasicToggle(buttonNumber) {
    logDebug "Handling basic toggle for button ${buttonNumber}"
    
    def currentState = getButtonState(buttonNumber)
    def newState = !currentState
    
    sendZWaveCommand([
        cmd: "basic",
        value: newState ? 0xFF : 0x00
    ])
    
    updateButtonState(buttonNumber, newState)
    updateButtonDisplay(buttonNumber, newState)
}

private void handlePrivacyMode() {
    logDebug "Handling privacy mode toggle"
    
    def currentState = state.privacyMode ?: false
    def newState = !currentState
    
    sendZWaveCommand([
        cmd: "indicator",
        value: newState ? 0x01 : 0x00
    ])
    
    state.privacyMode = newState
    updatePrivacyDisplay(newState)
}

private void handleHousekeepingMode() {
    logDebug "Handling housekeeping mode toggle"
    
    def currentState = state.housekeepingMode ?: false
    def newState = !currentState
    
    sendZWaveCommand([
        cmd: "indicator",
        value: newState ? 0x02 : 0x00
    ])
    
    state.housekeepingMode = newState
    updateHousekeepingDisplay(newState)
}

private void handleSceneBasicToggle(buttonNumber, config) {
    logDebug "Handling scene/basic toggle for button ${buttonNumber}"
    
    def currentState = getButtonState(buttonNumber)
    def newState = !currentState
    
    if (newState) {
        // Send scene activation for ON
        sendZWaveCommand([
            cmd: "scene",
            scene: config.sceneId,
            duration: settings.dimmerRampRate ?: 0
        ])
    } else {
        // Send basic set for OFF
        sendZWaveCommand([
            cmd: "basic",
            value: 0x00
        ])
    }
    
    updateButtonState(buttonNumber, newState)
    updateButtonDisplay(buttonNumber, newState)
}

// Display Update Methods
private void updatePageDisplay(pageNumber) {
    logDebug "Updating display for page ${pageNumber}"
    
    def pageConfig = state.pages["page${pageNumber}"]
    if (!pageConfig?.buttons) return
    
    def commands = []
    
    // Update each button's display
    pageConfig.buttons.each { buttonId, buttonConfig ->
        if (buttonConfig.enabled) {
            def buttonNum = buttonId.substring(6) as Integer
            commands.add(createLabelCommand(buttonNum, buttonConfig))
        }
    }
    
    // Execute commands with proper timing
    executeCommandQueue(commands)
}

private void updateButtonDisplay(buttonNumber, boolean state) {
    def buttonConfig = getButtonConfig(buttonNumber)
    if (!buttonConfig?.enabled) return
    
    def label = buttonConfig.label ?: ""
    
    // Handle inverted display for toggle states
    if (buttonConfig.font == "inverted") {
        buttonConfig.font = state ? "inverted" : "normal"
    }
    
    sendZWaveCommand(createLabelCommand(buttonNumber, buttonConfig))
}

private void updatePrivacyDisplay(boolean state) {
    // Update privacy indicator on all controllers in group
    def cmd = [
        cmd: "indicator",
        value: state ? 0x01 : 0x00
    ]
    
    sendZWaveCommand(cmd)
    
    // Update any buttons configured for privacy display
    updateModeButtons("privacy", state)
}

private void updateHousekeepingDisplay(boolean state) {
    // Update housekeeping indicator on all controllers in group
    def cmd = [
        cmd: "indicator",
        value: state ? 0x02 : 0x00
    ]
    
    sendZWaveCommand(cmd)
    
    // Update any buttons configured for housekeeping display
    updateModeButtons("housekeeping", state)
}

private void updateModeButtons(String mode, boolean state) {
    def currentPage = state.currentPage
    def pageConfig = state.pages["page${currentPage}"]
    
    pageConfig?.buttons.each { buttonId, buttonConfig ->
        if (buttonConfig.type == mode) {
            def buttonNum = buttonId.substring(6) as Integer
            updateButtonDisplay(buttonNum, state)
        }
    }
}

// Temperature Handling Methods
def temperatureEventHandler(evt) {
    logDebug "Temperature event: ${evt.value}"
    if (settings.enableTemp) {
        updateTemperatureDisplay(evt.value)
    }
}

private void updateTemperatureDisplay(value) {
    // Convert temperature if needed
    def displayTemp = settings.tempFormat == "C" ? value : celsiusToFahrenheit(value)
    def formattedTemp = sprintf("%.1f°${settings.tempFormat}", displayTemp)
    
    // Update temperature display on controller
    // Implementation depends on your controller's specific requirements
    logDebug "Updating temperature display: ${formattedTemp}"
}

private void scheduleTemperatureUpdates() {
    def interval = settings.tempUpdateInterval ?: 60
    schedule("0/${interval} * * * * ?", "updateTemperature")
}

// Utility Methods
private Map getButtonConfig(buttonNumber) {
    def pageNum = state.currentPage
    return state.pages["page${pageNum}"]?.buttons?.get("button${buttonNumber}")
}

private boolean getButtonState(buttonNumber) {
    return state.buttonStates["${state.currentPage}_${buttonNumber}"] ?: false
}

private void updateButtonState(buttonNumber, boolean newState) {
    state.buttonStates["${state.currentPage}_${buttonNumber}"] = newState
}

private void executeCommandQueue(List commands) {
    if (!commands) return
    
    def index = 0
    commands.each { cmd ->
        runInMillis(index * 100, 'executeCommand', [data: [cmd: cmd]])
        index++
    }
}

private void executeCommand(data) {
    try {
        sendZWaveCommand(data.cmd)
    } catch (e) {
        logError "Error executing command: ${e.message}"
    }
}

private Map createLabelCommand(buttonNumber, config) {
    def label = config.label ?: ""
    if (config.doubleLine) {
        label = formatDoubleLineLabel(label)
    }
    
    return [
        cmd: "screen_md",
        lineNumber: buttonNumber - 1,
        text: label,
        font: FONT_STYLES[config.font?.toUpperCase() ?: "NORMAL"],
        align: TEXT_ALIGN[config.align?.toUpperCase() ?: "CENTER"]
    ]
}

private String formatDoubleLineLabel(String label) {
    if (label.contains("\\r")) {
        return label.replace("\\r", "\r")
    }
    
    // Auto-split if no explicit line break
    if (label.length() > 8) {
        def midpoint = label.length() / 2
        def spaceIndex = label.indexOf(" ", midpoint as Integer)
        if (spaceIndex > 0) {
            return label.substring(0, spaceIndex) + "\r" + label.substring(spaceIndex + 1)
        }
    }
    
    return label
}

private void handlePageTimeout() {
    def currentPage = state.currentPage
    def timeoutAction = settings["page${currentPage}TimeoutAction"]
    
    switch(timeoutAction) {
        case "home":
            switchToPage(settings.homePage ?: 1)
            break
            
        case "specific":
            def targetPage = settings["page${currentPage}TimeoutPage"]
            if (targetPage) {
                switchToPage(targetPage)
            }
            break
    }
}

// Error Handling and Recovery
private void recoverFromError() {
    logDebug "Attempting to recover from error state"
    
    try {
        // Reset display
        updateDisplaySettings()
        
        // Restore current page
        updatePageDisplay(state.currentPage)
        
        // Restore mode indicators
        if (state.privacyMode) updatePrivacyDisplay(true)
        if (state.housekeepingMode) updateHousekeepingDisplay(true)
        
    } catch (e) {
        logError "Error during recovery: ${e.message}"
    }
}

// Lifecycle Management
def uninstalled() {
    logDebug "Uninstalling..."
    unschedule()
    unsubscribe()
}

// Helper Methods
private celsiusToFahrenheit(value) {
    return value * 9/5 + 32
}

private fahrenheitToCelsius(value) {
    return (value - 32) * 5/9
}

// Debug Helpers
private void logDebug(msg) {
    if (settings.enableDebug) {
        log.debug "${app.name}: ${msg}"
    }
}

private void logInfo(msg) {
    if (settings.enableDesc) {
        log.info "${app.name}: ${msg}"
    }
}

private void logError(msg) {
    log.error "${app.name}: ${msg}"
}

// Version Info
def getVersionInfo() {
    return [
        version: VERSION,
        type: "Hubitat App",
        name: app.name,
        namespace: "brianekane",
        author: "Brian Kane",
        updated: "2025-01-05"
    ]
}
