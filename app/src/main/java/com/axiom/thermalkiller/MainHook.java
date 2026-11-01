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
        XposedBridge.log(TAG + " : Zygote armed");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android")) {
            try {
                Class<?> thermal = XposedHelpers.findClass("com.android.server.power.ThermalManagerService", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(thermal, "onTemperatureChanged", "android.os.Temperature", new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        Object t = param.args[0];
                        if (t != null) XposedHelpers.setFloatField(t, "mValue", 31.0f);
                    }
                });
                XposedHelpers.findAndHookMethod(thermal, "getCurrentTemperatures", XC_MethodReplacement.returnConstant(new Object[0]));
            } catch (Throwable t) {}

            try {
                Class<?> pms = XposedHelpers.findClass("com.android.server.power.PowerManagerService", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(pms, "setPowerMode", int.class, boolean.class, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        param.args[0] = 1;
                        param.args[1] = true;
                    }
                });
            } catch (Throwable t) {}
        }

        String[] vivo = {"com.vivo.daemon", "com.vivo.abe", "com.vivo.pem", "com.vivo.sps", "android"};
        for (String pkg : vivo) {
            if (lpparam.packageName.equals(pkg)) {
                try {
                    Class<?> vt = XposedHelpers.findClassIfExists("com.vivo.common.VivoThermalControl", lpparam.classLoader);
                    if (vt != null) {
                        XposedHelpers.findAndHookMethod(vt, "isOverheat", XC_MethodReplacement.returnConstant(false));
                        XposedHelpers.findAndHookMethod(vt, "needThrottle", XC_MethodReplacement.returnConstant(false));
                        XposedHelpers.findAndHookMethod(vt, "getCurrentTemperature", XC_MethodReplacement.returnConstant(30.0f));
                    }
                } catch (Throwable ignored) {}
            }
        }

        if (lpparam.packageName.equals("android")) {
            try {
                XposedHelpers.findAndHookMethod(Settings.Global.class, "getInt",
                    android.content.ContentResolver.class, String.class, int.class, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[1];
                        if (key != null && (key.contains("game_mode") || key.contains("performance") || key.contains("thermal") || key.contains("vivo_"))) {
                            param.setResult(1);
                        }
                    }
                });
            } catch (Throwable t) {}
        }
        XposedBridge.log(TAG + " hooks live on " + lpparam.packageName);
    }
}
