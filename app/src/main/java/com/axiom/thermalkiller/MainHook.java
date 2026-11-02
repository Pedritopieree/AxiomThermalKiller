package com.axiom.thermalkiller;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "AxiomThermalKiller";

    @Override
    public void initZygote(StartupParam startupParam) {
        XposedBridge.log(TAG + " : Full unlock mode v3");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        // Force Super Resolution support for every package
        hookSupport(lpparam, "com.vivo.services.game.sr.VSRController", "isSupport");
        hookSupport(lpparam, "com.vivo.services.game.sr.VSRController", "isSupported");
        hookSupport(lpparam, "com.vivo.gameassistant.superresolution.a", "isSupport");
        hookSupport(lpparam, "com.vivo.gameassistant.superresolution.b", "isSupport");

        // Force Frame Interpolation support
        hookSupport(lpparam, "com.vivo.gameassistant.frameinterpolation.f", "isSupport");
        hookSupport(lpparam, "com.vivo.gameassistant.frameinterpolation.h", "isSupport");

        // Force GameManager to treat everything as game
        hookSupport(lpparam, "com.vivo.services.game.GameManagerService", "isGame");

        // Force support list
        hookSupport(lpparam, "com.vivo.common.supportlist.pojo.a", "isSupport");
        hookSupport(lpparam, "com.vivo.common.gamemanipulation.a", "isSupport");

        // Thermal
        if (lpparam.packageName.contains("vivo") || lpparam.packageName.equals("android")) {
            try {
                Class<?> t = XposedHelpers.findClassIfExists("com.vivo.common.VivoThermalControl", lpparam.classLoader);
                if (t != null) {
                    XposedHelpers.findAndHookMethod(t, "isOverheat", XC_MethodReplacement.returnConstant(false));
                    XposedHelpers.findAndHookMethod(t, "needThrottle", XC_MethodReplacement.returnConstant(false));
                }
            } catch (Throwable ignored) {}
        }

        XposedBridge.log(TAG + " hooks live on " + lpparam.packageName);
    }

    private void hookSupport(XC_LoadPackage.LoadPackageParam lpparam, String className, String method) {
        try {
            Class<?> clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
            if (clazz != null) {
                XposedHelpers.findAndHookMethod(clazz, method, String.class, XC_MethodReplacement.returnConstant(true));
                XposedBridge.log(TAG + " forced " + className + "." + method);
            }
        } catch (Throwable ignored) {}
    }
}
