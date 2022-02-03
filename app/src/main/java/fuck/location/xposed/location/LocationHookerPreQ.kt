package fuck.location.xposed.location

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationManager
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookMethod
import com.github.kyuubiran.ezxhelper.utils.isPublic
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import fuck.location.xposed.helpers.ConfigGateway
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.Exception

class LocationHookerPreQ {
    @SuppressLint("PrivateApi")
    @ExperimentalStdlibApi
    fun hookLastLocation(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz = lpparam.classLoader.loadClass("com.android.server.LocationManagerService")

        findAllMethods(clazz) {
            name == "getLastLocation" && isPublic
        }.hookMethod {
            after {
                val packageName = ConfigGateway.get().callerIdentityToPackageName(it.args[1])
                XposedBridge.log("FL: in getLastLocation (Pre Q)! Caller package name: $packageName")

                if (ConfigGateway.get().inWhitelist(packageName)) {
                    XposedBridge.log("FL: in whitelist! Return custom location")
                    val fakeLocation = ConfigGateway.get().readFakeLocation()

                    lateinit var location: Location
                    lateinit var originLocation: Location

                    if (it.result == null) {
                        location = Location(LocationManager.GPS_PROVIDER)
                        location.time = System.currentTimeMillis() - (100..10000).random()
                    } else {
                        originLocation = it.result as Location
                        location = Location(originLocation.provider)

                        location.time = originLocation.time
                        location.accuracy = originLocation.accuracy
                        location.bearing = originLocation.bearing
                        location.bearingAccuracyDegrees = originLocation.bearingAccuracyDegrees
                        location.elapsedRealtimeNanos = originLocation.elapsedRealtimeNanos
                        location.verticalAccuracyMeters = originLocation.verticalAccuracyMeters
                    }

                    location.latitude = fakeLocation?.x!!
                    location.longitude = fakeLocation.y
                    location.altitude = 0.0
                    location.speed = 0F
                    location.speedAccuracyMetersPerSecond = 0F

                    try {
                        HiddenApiBypass.invoke(location.javaClass, location, "setIsFromMockProvider", false)
                    } catch (e: Exception) {
                        XposedBridge.log("FL: Not possible to mock (Pre Q)! $e")
                    }

                    XposedBridge.log("FL: x: ${location.latitude}, y: ${location.longitude}")
                    it.result = location
                }
            }
        }

        findAllMethods(clazz) {
            name == "requestLocationUpdates" && isPublic
        }.hookMethod {
            before { param ->
                val packageName = param.args[3] as String
                XposedBridge.log("FL: in requestLocationUpdates (Pre Q)! Caller package name: $packageName")

                if (ConfigGateway.get().inWhitelist(packageName)) {
                    XposedBridge.log("FL: in whiteList! Inject custom location...")

                    val lastParam = param.args[1].javaClass
                    if (lastParam.typeName == "android.location.ILocationListener\$Stub\$Proxy") {
                        XposedBridge.log("FL: is LocationListener (Pre Q)!")

                        XposedBridge.log("FL: Finding method in LocationListener (Pre Q)")

                        val targetMethod = findAllMethods(lastParam) {
                            name == "onLocationChanged" && parameterCount == 1
                        }

                        targetMethod.hookMethod {
                            before { param ->
                                val originLocation = param.args[0] as Location

                                val fakeLocation = ConfigGateway.get().readFakeLocation()
                                val location = Location(originLocation.provider)

                                location.latitude = fakeLocation?.x!!
                                location.longitude = fakeLocation.y
                                location.altitude = 0.0
                                location.speed = 0F
                                location.speedAccuracyMetersPerSecond = 0F

                                location.time = originLocation.time
                                location.accuracy = originLocation.accuracy
                                location.bearing = originLocation.bearing
                                location.bearingAccuracyDegrees = originLocation.bearingAccuracyDegrees
                                location.elapsedRealtimeNanos = originLocation.elapsedRealtimeNanos
                                location.verticalAccuracyMeters = originLocation.verticalAccuracyMeters

                                try {
                                    HiddenApiBypass.invoke(location.javaClass, location, "setIsFromMockProvider", false)
                                } catch (e: Exception) {
                                    XposedBridge.log("FL: Not possible to mock (Pre Q)! $e")
                                }

                                param.args[0] = location
                            }
                        }
                    } else {
                        // TODO: Implement PendingIntent
                        XposedBridge.log("FL: is PendingIntent that currently not supported")
                    }
                }
            }
        }
    }
}