package com.axiom.thermalkiller;

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
        XposedBridge.log(TAG + " : Full GameCube-style unlock armed");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        // ===== Super Resolution (VSR) =====
        try {
            Class<?> vsr = XposedHelpers.findClassIfExists("com.vivo.services.game.sr.VSRController", lpparam.classLoader);
            if (vsr != null) {
                // Force support for any package
                XposedHelpers.findAndHookMethod(vsr, "isSupport", String.class, XC_MethodReplacement.returnConstant(true));
                XposedHelpers.findAndHookMethod(vsr, "isSupported", String.class, XC_MethodReplacement.returnConstant(true));
            }
        } catch (Throwable ignored) {}

        // ===== Frame Interpolation =====
        try {
            Class<?> fi = XposedHelpers.findClassIfExists("com.vivo.gameassistant.frameinterpolation.f", lpparam.classLoader);
            if (fi != null) {
                XposedHelpers.findAndHookMethod(fi, "isSupport", String.class, XC_MethodReplacement.returnConstant(true));
            }
        } catch (Throwable ignored) {}

        // ===== Game Manager =====
        try {
            Class<?> gms = XposedHelpers.findClassIfExists("com.vivo.services.game.GameManagerService", lpparam.classLoader);
            if (gms != null) {
                // Force high performance / monster style
                XposedHelpers.findAndHookMethod(gms, "isGame", String.class, XC_MethodReplacement.returnConstant(true));
            }
        } catch (Throwable ignored) {}

        // ===== Thermal still =====
        if (lpparam.packageName.equals("android") || lpparam.packageName.contains("vivo")) {
            try {
                Class<?> thermal = XposedHelpers.findClassIfExists("com.vivo.common.VivoThermalControl", lpparam.classLoader);
                if (thermal != null) {
                    XposedHelpers.findAndHookMethod(thermal, "isOverheat", XC_MethodReplacement.returnConstant(false));
                    XposedHelpers.findAndHookMethod(thermal, "needThrottle", XC_MethodReplacement.returnConstant(false));
                }
            } catch (Throwable ignored) {}
        }

        // ===== Force every game as supported =====
        try {
            // Common support list pattern
            Class<?> support = XposedHelpers.findClassIfExists("com.vivo.common.supportlist.pojo.a", lpparam.classLoader);
            if (support != null) {
                XposedHelpers.findAndHookMethod(support, "isSupport", String.class, XC_MethodReplacement.returnConstant(true));
            }
        } catch (Throwable ignored) {}

        XposedBridge.log(TAG + " unlock hooks applied on " + lpparam.packageName);
    }
}
