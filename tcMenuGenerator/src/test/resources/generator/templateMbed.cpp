/*
    The code in this file uses open source libraries provided by thecoderscorner

    DO NOT EDIT THIS FILE, IT WILL BE GENERATED EVERY TIME YOU USE THE UI DESIGNER
    INSTEAD EITHER PUT CODE IN YOUR SKETCH OR CREATE ANOTHER SOURCE FILE.

    All the variables you may need access to are marked extern in this file for easy
    use elsewhere.
 */

#include <tcMenu.h>
#include "project_menu.h"
#include <Fonts/sans24p7b.h>

// Global variable declarations
const  ConnectorLocalInfo applicationInfo = { "tester", "uuid1" };
TcMenuRemoteServer remoteServer(applicationInfo);
ArduinoAnalogDevice analogDevice(42);
const int anotherVar;
const int allowedPluginVar;

// Global Menu Item declarations
ScrollChoiceMenuItem menuMySubSub1CustomChoice(18, fnMySubSub1CustomChoiceRtCall, 0, 6, NULL);
extern const char* myChoiceRam;
RENDERING_CALLBACK_NAME_INVOKE(fnMySubSub1RamChoiceRtCall, enumItemRenderFn, "Ram Choice", 31, onRamChoice)
ScrollChoiceMenuItem menuMySubSub1RamChoice(17, fnMySubSub1RamChoiceRtCall, 0, myChoiceRam, 5, 6, &menuMySubSub1CustomChoice);
RENDERING_CALLBACK_NAME_INVOKE(fnMySubSub1EepromChoiceRtCall, enumItemRenderFn, "EepromChoice", 29, onRomChoice)
ScrollChoiceMenuItem menuMySubSub1EepromChoice(16, fnMySubSub1EepromChoiceRtCall, 0, 128, 11, 4, &menuMySubSub1RamChoice);
RENDERING_CALLBACK_NAME_INVOKE(fnMySubSub1RGBRtCall, rgbAlphaItemRenderFn, "RGB", -1, onRgb)
Rgb32MenuItem menuMySubSub1RGB(15, fnMySubSub1RGBRtCall, true, &menuMySubSub1EepromChoice);
RENDERING_CALLBACK_NAME_INVOKE(fnMySubSub1DateFieldRtCall, dateItemRenderFn, "Date Field", 25, NO_CALLBACK)
DateFormattedMenuItem menuMySubSub1DateField(fnMySubSub1DateFieldRtCall, 14, &menuMySubSub1RGB);
RENDERING_CALLBACK_NAME_INVOKE(fnMySubSub1TimeFieldRtCall, timeItemRenderFn, "Time Field", -1, NO_CALLBACK)
TimeFormattedMenuItem menuMySubSub1TimeField(fnMySubSub1TimeFieldRtCall, 13, (MultiEditWireType)8, &menuMySubSub1DateField);
RENDERING_CALLBACK_NAME_INVOKE(fnMySubSub1IPAddressRtCall, ipAddressRenderFn, "IP Address", 21, onIpChange)
IpAddressMenuItem menuMySubSub1IPAddress(fnMySubSub1IPAddressRtCall, 12, &menuMySubSub1TimeField);
RENDERING_CALLBACK_NAME_INVOKE(fnMySubSub1TextItemRtCall, textItemRenderFn, "Text Item", 7, NO_CALLBACK)
TextMenuItem menuMySubSub1TextItem(fnMySubSub1TextItemRtCall, 11, 14, &menuMySubSub1IPAddress);
RENDERING_CALLBACK_NAME_INVOKE(fnMySubSub1IntLargeRtCall, largeNumItemRenderFn, "Int Large", -1, NO_CALLBACK)
EditableLargeNumberMenuItem menuMySubSub1IntLarge(fnMySubSub1IntLargeRtCall, 10, 8, 0, false, &menuMySubSub1TextItem);
RENDERING_CALLBACK_NAME_INVOKE(fnMySubSub1DecLargeRtCall, largeNumItemRenderFn, "Dec Large", -1, onDecLarge)
EditableLargeNumberMenuItem menuMySubSub1DecLarge(fnMySubSub1DecLargeRtCall, 9, 8, 3, true, &menuMySubSub1IntLarge);
RENDERING_CALLBACK_NAME_INVOKE(fnMySubSub1RtCall, backSubItemRenderFn, "Sub1", -1, NO_CALLBACK)
const SubMenuInfo minfoMySubSub1 = { "Sub1", 8, 0xffff, 0, NO_CALLBACK };
BackMenuItem menuBackMySubSub1(fnMySubSub1RtCall, &menuMySubSub1DecLarge);
SubMenuItem menuMySubSub1(&minfoMySubSub1, &menuBackMySubSub1, NULL);
ListRuntimeMenuItem menuMySubMyList(7, 0, fnMySubMyListRtCall, &menuMySubSub1);
const AnyMenuInfo minfoMySubMyAction = { "My Action", 6, 0xffff, 0, onActionItem };
ActionMenuItem menuMySubMyAction(&minfoMySubMyAction, &menuMySubMyList);
const FloatMenuInfo minfoMySubMyFloat = { "My Float", 5, 0xffff, 3, NO_CALLBACK };
FloatMenuItem menuMySubMyFloat(&minfoMySubMyFloat, &menuMySubMyAction);
const BooleanMenuInfo minfoMySubMyBoolean = { "My Boolean", 4, 6, 1, onBoolChange, NAMING_YES_NO };
BooleanMenuItem menuMySubMyBoolean(&minfoMySubMyBoolean, false, &menuMySubMyFloat);
const char enumStrMySubMyEnum_0[] = "Item1";
const char enumStrMySubMyEnum_1[] = "Item2";
const char* const enumStrMySubMyEnum[]  = { enumStrMySubMyEnum_0, enumStrMySubMyEnum_1 };
const EnumMenuInfo minfoMySubMyEnum = { "MyEnum", 3, 4, 1, onEnumChange, enumStrMySubMyEnum };
EnumMenuItem menuMySubMyEnum(&minfoMySubMyEnum, 0, &menuMySubMyBoolean);
const AnalogMenuInfo minfoMySubMyAnalog = { "My Analog", 2, 2, 255, onAnalogItem, 0, 1, "Unit" };
AnalogMenuItem menuMySubMyAnalog(&minfoMySubMyAnalog, 0, &menuMySubMyEnum);

void setupMenu() {
    menuMySubSub1TextItem.setReadOnly(true);
    menuMySubSub1DecLarge.setLocalOnly(true);

    switches.initialise(io23017, true, MenuFontDef(&sans24p7b, 1), MenuFontDef(&sans24p7b, 1));
    switches.addSwitch(BUTTON_PIN, &null);
    switches.onRelease(BUTTON_PIN, [](uint8_t /*key*/, bool held) {
            anotherFn(20);
        });
    initialiseMyTheme();
    turboTron.begin(&Serial, &applicationInfo, &menuMySubMyAnalog, MBED_RTOS);
}

