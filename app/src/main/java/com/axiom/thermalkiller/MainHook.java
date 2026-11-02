import android.content.Context;
import android.content.pm.ApplicationInfo;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("com.vivo.gamecube") &&
            !lpparam.packageName.equals("com.vivo.gamewatch")) return;

        ClassLoader cl = lpparam.classLoader;

        // ─── THERMAL BYPASS ───────────────────────────────────────────────
        // TemperatureHelper — retorna temp segura para o GameCube não throttle
        hookAllMethods(cl, "com.vivo.common.utils.TemperatureHelper", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.getResult() instanceof Float)  param.setResult(35.0f);
                if (param.getResult() instanceof Integer) param.setResult(35);
            }
        });

        // ─── THERMAL FPS BYPASS ───────────────────────────────────────────
        // Bloquear reduções de FPS induzidas por temperatura
        String[] thermalFpsClasses = {
            "com.vivo.gameassistant.frameinterpolation.FrameInterpolationState",
            "com.vivo.gameassistant.frameinterpolation.ChangeFrameReason",
            "com.vivo.gameassistant.frameinterpolation.f",
            "com.vivo.gameassistant.frameinterpolation.h",
        };
        for (String cls : thermalFpsClasses) {
            try {
                Class<?> clazz = cl.loadClass(cls);
                for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                    String n = m.getName().toLowerCase();
                    if (n.contains("thermal") || n.contains("reduce") || n.contains("mapfps")) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(null));
                    }
                }
            } catch (Throwable ignored) {}
        }

        // ─── THERMAL GRAPHICS BYPASS ──────────────────────────────────────
        // Bypass de reduções de SR/MEMC induzidas por temperatura
        String[] thermalGfxClasses = {
            "com.vivo.gameassistant.superresolution.a",
            "com.vivo.gameassistant.superresolution.b",
            "com.vivo.gameassistant.superresolution.d",
            "com.vivo.gameassistant.superresolution.i",
            "com.vivo.gameassistant.superresolution.o",
            "com.vivo.common.utils.h0",
        };
        for (String cls : thermalGfxClasses) {
            try {
                Class<?> clazz = cl.loadClass(cls);
                for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                    String n = m.getName().toLowerCase();
                    if (n.contains("thermal") || n.contains("reduce") || n.contains("disable") || n.contains("memc")) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(null));
                    }
                }
            } catch (Throwable ignored) {}
        }

        // ─── MONSTER+ UNLOCK ──────────────────────────────────────────────
        String[] monsterClasses = {
            "com.vivo.gameassistant.gamemanipulation.e",
            "com.vivo.common.gamemanipulation.a",
            "com.vivo.common.gamemanipulation.b",
        };
        for (String cls : monsterClasses) {
            try {
                Class<?> clazz = cl.loadClass(cls);
                for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                    if (m.getReturnType() == boolean.class) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true));
                    }
                }
            } catch (Throwable ignored) {}
        }

        // EdgeDock Monster+ enable
        hookBooleanReturn(cl, "com.vivo.gameassistant.homegui.sideslide.dock.view.EdgeDockView", true);

        // ─── 144Hz HOLD ───────────────────────────────────────────────────
        // RRM display rate — forçar 144Hz em idle
        String[] rrmClasses = {
            "com.vivo.services.rrm.RMWms",
            "com.vivo.services.rms.RMWms",
            "com.vivo.services.rms.display.DisplayRateManager",
        };
        for (String cls : rrmClasses) {
            try {
                Class<?> clazz = cl.loadClass(cls);
                for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                    if (m.getReturnType() == int.class) {
                        String n = m.getName().toLowerCase();
                        if (n.contains("fps") || n.contains("rate") || n.contains("refresh")) {
                            XposedBridge.hookMethod(m, new XC_MethodHook() {
                                @Override protected void afterHookedMethod(MethodHookParam param) {
                                    int v = (int) param.getResult();
                                    if (v < 144) param.setResult(144);
                                }
                            });
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // ─── FRAME HOOKS (OS4 + OS5) ──────────────────────────────────────
        // gameFps=144, depressFps=60, interFps=144
        String[] frameClasses = {
            "com.vivo.gameassistant.framedisplay.a",
            "com.vivo.gameassistant.framedisplay.d",
            "com.vivo.gameassistant.framedisplay.e", // OS5
        };
        for (String cls : frameClasses) {
            try {
                Class<?> clazz = cl.loadClass(cls);
                for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                    if (m.getReturnType() == int.class) {
                        String n = m.getName().toLowerCase();
                        final boolean isDepress = n.contains("depress");
                        final boolean isInter   = n.contains("inter");
                        final boolean isGame    = n.contains("game") || n.contains("fps");
                        if (isDepress || isInter || isGame) {
                            XposedBridge.hookMethod(m, new XC_MethodHook() {
                                @Override protected void afterHookedMethod(MethodHookParam param) {
                                    if (isDepress) param.setResult(60);
                                    else param.setResult(144);
                                }
                            });
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // ─── SUPER RESOLUTION ─────────────────────────────────────────────
        String[] srClasses = {
            "com.vivo.gameassistant.superresolution.a",
            "com.vivo.gameassistant.superresolution.b",
            "com.vivo.gameassistant.superresolution.d",
            "com.vivo.gameassistant.superresolution.i",
            "com.vivo.gameassistant.superresolution.o",
            "com.vivo.gameassistant.utils.e0",
        };
        for (String cls : srClasses) {
            try {
                Class<?> clazz = cl.loadClass(cls);
                for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                    if (m.getReturnType() == boolean.class) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true));
                    }
                }
            } catch (Throwable ignored) {}
        }

        // ─── VSR BACKEND ──────────────────────────────────────────────────
        // setVSR — override algorithm=1, targetShortSide=0, sourceSize=0
        String[] vsrClasses = {
            "com.vivo.services.game.sr.VSRController",
            "vivo.common.AbsVivoDisplayEnhanceManager",
        };
        for (String cls : vsrClasses) {
            try {
                Class<?> clazz = cl.loadClass(cls);
                for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals("setVSR") || m.getName().equals("requestVSR")) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override protected void beforeHookedMethod(MethodHookParam param) {
                                if (param.args != null && param.args.length >= 1) {
                                    param.args[0] = 1; // algorithm
                                }
                            }
                        });
                    }
                }
            } catch (Throwable ignored) {}
        }

        // ─── GAMEPOWEROPT — FI UNLOCK (PD2453 probe: Enable=0) ───────────
        // GamePowerOptFeature estava desligada → forçar enable para desbloquear FI
        String[] gamePowerClasses = {
            "com.vivo.services.power.feature.GamePowerOptFeature",
            "com.vivo.services.power.feature.PowerFeatureManager",
            "com.vivo.power.feature.GamePowerOptFeature",
        };
        for (String cls : gamePowerClasses) {
            hookBooleanReturn(cl, cls, true);
        }

        // ─── FRAME INTERPOLATION / MEMC ───────────────────────────────────
        // PD2453 probe: sys.game.memc.gles.enable=1, memc.vulkan.enable=1
        // FI whitelist v15: gamecube_frame_interpolation="1:0:0:60:144"
        String[] fiClasses = {
            "com.vivo.gameassistant.frameinterpolation.FrameInterpolationManager",
            "com.vivo.gameassistant.frameinterpolation.FrameInterpolationState",
            "com.vivo.gameassistant.frameinterpolation.FrameInterpolationSession",
            "com.vivo.gameassistant.memc.MemcManager",
            "com.vivo.gameassistant.memc.MemcController",
            "com.vivo.gameassistant.memc.MemcState",
            "com.vivo.gameassistant.frameinterpolation.a",
            "com.vivo.gameassistant.frameinterpolation.b",
            "com.vivo.gameassistant.frameinterpolation.c",
            "com.vivo.gameassistant.frameinterpolation.g",
        };
        for (String cls : fiClasses) {
            try {
                Class<?> clazz = cl.loadClass(cls);
                for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                    String n = m.getName().toLowerCase();
                    // Forçar enable/support/whitelist → true
                    if (m.getReturnType() == boolean.class &&
                        (n.contains("enable") || n.contains("support") || n.contains("can") ||
                         n.contains("allow") || n.contains("active") || n.contains("white") ||
                         n.contains("memc") || n.contains("interpolat"))) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true));
                    }
                    // FPS_MAX_THRESHOLD: tuna=120 → forçar 144
                    if (m.getReturnType() == int.class && m.getParameterTypes().length == 0 &&
                        (n.contains("max") && n.contains("fps") || n.contains("threshold"))) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(144));
                    }
                    // gamecube_frame_interpolation string → forçar "1:0:0:60:144"
                    if (m.getReturnType() == String.class &&
                        (n.contains("param") || n.contains("config") || n.contains("setting"))) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override protected void afterHookedMethod(MethodHookParam param) {
                                Object r = param.getResult();
                                if (r instanceof String && ((String) r).contains(":")) {
                                    param.setResult("1:0:0:60:144");
                                }
                            }
                        });
                    }
                    // getFiTargetFps / getFiSourceFps
                    if (m.getReturnType() == int.class && m.getParameterTypes().length == 0) {
                        if (n.contains("target") && n.contains("fps")) {
                            XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(144));
                        }
                        if (n.contains("source") && n.contains("fps")) {
                            XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(60));
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // ─── SF OFFSET V6 (PD2453 probe: menor latência a 144fps) ────────
        // SfDurationPercentage=149 a 144fps (advanced_sf_offsets.xml Device v6)
        try {
            Class<?> clazz = cl.loadClass("com.qualcomm.qti.qdisplay.sfoffset.AdvancedSfOffsetManager");
            for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                if (m.getReturnType() == long.class) {
                    String n = m.getName().toLowerCase();
                    if (n.contains("duration") || n.contains("offset")) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(149L));
                    }
                    if (n.contains("app") && n.contains("duration")) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(10333333L));
                    }
                }
            }
        } catch (Throwable ignored) {}

        // ─── AUDIO UNLOCK ─────────────────────────────────────────────────
        String[] audioClasses = {
            "com.vivo.gameassistant.gamecustomsound.GameSoundSupportGame",
            "com.vivo.gameassistant.gamecustomsound.c",
            "com.vivo.gameassistant.gamecustomsound.d",
        };
        for (String cls : audioClasses) {
            hookBooleanReturn(cl, cls, true);
        }

        // ─── GYRO ANTI-SHAKE ──────────────────────────────────────────────
        try {
            Class<?> clazz = cl.loadClass("com.vivo.gameassistant.utils.z0");
            for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                if (m.getName().toLowerCase().contains("gyro")) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam param) {
                            XposedBridge.log("GCUnlock: gyro anti-shake intercepted");
                        }
                    });
                }
            }
        } catch (Throwable ignored) {}

        XposedBridge.log("GCUnlock: all hooks installed for " + lpparam.packageName);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────
    private void hookBooleanReturn(ClassLoader cl, String className, boolean returnValue) {
        try {
            Class<?> clazz = cl.loadClass(className);
            for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                if (m.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(returnValue));
                }
            }
        } catch (Throwable ignored) {}
    }

    private void hookAllMethods(ClassLoader cl, String className, XC_MethodHook hook) {
        try {
            Class<?> clazz = cl.loadClass(className);
            for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                XposedBridge.hookMethod(m, hook);
            }
        } catch (Throwable ignored) {}
    }
}
