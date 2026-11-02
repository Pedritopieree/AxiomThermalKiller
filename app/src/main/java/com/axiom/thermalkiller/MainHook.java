package com.axiom.thermalkiller;

import android.provider.Settings;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "AxiomThermalKiller";

    @Override
    public void initZygote(StartupParam startupParam) {
        XposedBridge.log(TAG + " : Zygote armed - full unlock mode");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        // ========== SYSTEM THERMAL + POWER ==========
        if (lpparam.packageName.equals("android")) {
            try {
                Class<?> thermal = XposedHelpers.findClass("com.android.server.power.ThermalManagerService", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(thermal, "onTemperatureChanged", "android.os.Temperature", new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        Object t = param.args[0];
                        if (t != null) XposedHelpers.setFloatField(t, "mValue", 28.0f);
                    }
                });
                XposedHelpers.findAndHookMethod(thermal, "getCurrentTemperatures", XC_MethodReplacement.returnConstant(new Object[0]));
            } catch (Throwable ignored) {}

            try {
                Class<?> pms = XposedHelpers.findClass("com.android.server.power.PowerManagerService", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(pms, "setPowerMode", int.class, boolean.class, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        param.args[0] = 1; // PERFORMANCE
                        param.args[1] = true;
                    }
                });
            } catch (Throwable ignored) {}
        }

        // ========== VIVO CORE SERVICES ==========
        String[] vivoPkgs = {
            "android", "com.vivo.daemon", "com.vivo.abe", "com.vivo.pem",
            "com.vivo.sps", "com.vivo.gamecube", "com.vivo.gamespace", "com.vivo.gamewatch"
        };

        for (String pkg : vivoPkgs) {
            if (!lpparam.packageName.equals(pkg)) continue;

            // Thermal control
            try {
                Class<?> vt = XposedHelpers.findClassIfExists("com.vivo.common.VivoThermalControl", lpparam.classLoader);
                if (vt != null) {
                    XposedHelpers.findAndHookMethod(vt, "isOverheat", XC_MethodReplacement.returnConstant(false));
                    XposedHelpers.findAndHookMethod(vt, "needThrottle", XC_MethodReplacement.returnConstant(false));
                    XposedHelpers.findAndHookMethod(vt, "getCurrentTemperature", XC_MethodReplacement.returnConstant(28.0f));
                }
            } catch (Throwable ignored) {}

            // Force Monster / high performance profile
            try {
                Class<?> gameMode = XposedHelpers.findClassIfExists("com.vivo.gamemode.GameModeService", lpparam.classLoader);
                if (gameMode != null) {
                    XposedHelpers.findAndHookMethod(gameMode, "isOverheat", XC_MethodReplacement.returnConstant(false));
                }
            } catch (Throwable ignored) {}
        }

        // ========== FORCE ALL FEATURE FLAGS ==========
        if (lpparam.packageName.equals("android") || lpparam.packageName.contains("vivo")) {
            try {
                XposedHelpers.findAndHookMethod(Settings.Global.class, "getInt",
                    android.content.ContentResolver.class, String.class, int.class, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[1];
                        if (key == null) return;

                        // Unlock everything related to performance / game features
                        if (key.contains("game_mode") || key.contains("performance") ||
                            key.contains("thermal") || key.contains("monster") ||
                            key.contains("super_resolution") || key.contains("touch_sampling") ||
                            key.contains("frame_interpolation") || key.contains("high_touch") ||
                            key.contains("vivo_game") || key.contains("gamecube") ||
                            key.contains("gamespace") || key.contains("super_")) {
                            param.setResult(1);
                        }
                    }
                });
            } catch (Throwable ignored) {}

            // Also force SystemProperties
            try {
                Class<?> sp = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(sp, "get", String.class, String.class, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[0];
                        if (key == null) return;
                        if (key.contains("thermal") || key.contains("throttle")) {
                            param.setResult("0");
                        }
                        if (key.contains("perf") || key.contains("game") || key.contains("boost") ||
                            key.contains("monster") || key.contains("super")) {
                            param.setResult("1");
                        }
                    }
                });
            } catch (Throwable ignored) {}
        }

        // ========== FORCE HIGH REFRESH + TOUCH ==========
        if (lpparam.packageName.equals("android") || lpparam.packageName.equals("com.android.systemui")) {
            try {
                XposedHelpers.findAndHookMethod("android.view.SurfaceControl", lpparam.classLoader,
                    "setFrameRate", float.class, int.class, int.class, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        param.args[0] = 144.0f; // force high refresh
                    }
                });
            } catch (Throwable ignored) {}
        }

        XposedBridge.log(TAG + " full hooks live on " + lpparam.packageName);
    }
}
