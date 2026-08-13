package com.grafizum.repio

/**
 * These subclasses are used as entry points in the AndroidManifest.xml
 * to allow different Splash Screen themes for each App Icon variant.
 * 
 * Android activity-aliases do not support custom themes, so separate 
 * classes are necessary to bypass this platform limitation.
 */
class MainActivityFlame : MainActivity()
class MainActivityBeige : MainActivity()
class MainActivityPurple : MainActivity()
class MainActivityEmerald : MainActivity()
class MainActivityCrimson : MainActivity()
class MainActivityAzure : MainActivity()
class MainActivityOcean : MainActivity()
class MainActivityRose : MainActivity()
