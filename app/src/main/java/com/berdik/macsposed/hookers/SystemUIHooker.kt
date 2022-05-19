package com.berdik.macsposed.hookers

import android.annotation.SuppressLint
import android.util.ArraySet
import com.github.kyuubiran.ezxhelper.utils.*
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class SystemUIHooker {
    companion object {
        private const val tileId = "custom(com.berdik.macsposed/.QuickTile)"
        private var tileAdded = false

        @SuppressLint("PrivateApi")
        fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
            findAllMethods(lpparam.classLoader.loadClass("com.android.systemui.qs.QSPanelControllerBase")) {
                name == "setTiles" && isPublic && paramCount == 0
            }.hookMethod {
                before { param ->
                    if (!tileAdded) {
                        val tileHost = XposedHelpers.getObjectField(param.thisObject, "mHost")
                        XposedHelpers.callMethod(tileHost, "addTile", tileId, -1)
                        XposedBridge.log("[MACsposed] Tile added!")
                        tileAdded = true
                    }
                }
            }

            // Properly fixing the unchecked cast warning with Kotlin adds more performance overhead than it is worth,
            // so we are suppressing the warning instead.
            @Suppress("UNCHECKED_CAST")
            findAllMethods(lpparam.classLoader.loadClass("com.android.systemui.qs.PagedTileLayout")) {
                name == "startTileReveal" && isPublic
            }.hookMethod {
                before { param ->
                    val tilesToReveal = param.args[0] as ArraySet<String>
                    tilesToReveal.add(tileId)
                    XposedBridge.log("[MACsposed] Tile revealed!")
                }
            }
        }
    }
}