package com.android.systemui.doze;

import android.app.AlarmManager;
import android.content.Context;
import android.hardware.SensorManager;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.Handler;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.R$string;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.dock.DockManager;
import com.android.systemui.doze.DozeMachine.Part;
import com.android.systemui.doze.DozeMachine.Service;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.AsyncSensorManager;
import com.android.systemui.util.wakelock.DelayedWakeLock;
import com.android.systemui.util.wakelock.WakeLock;

public class DozeFactory {
    public DozeMachine assembleMachine(DozeService dozeService) {
        DozeService dozeService2 = dozeService;
        SensorManager sensorManager = (SensorManager) Dependency.get(AsyncSensorManager.class);
        AlarmManager alarmManager = (AlarmManager) dozeService2.getSystemService(AlarmManager.class);
        DockManager dockManager = (DockManager) Dependency.get(DockManager.class);
        DozeHost host = getHost(dozeService);
        AmbientDisplayConfiguration ambientDisplayConfiguration = new AmbientDisplayConfiguration(dozeService2);
        DozeParameters instance = DozeParameters.getInstance(dozeService);
        Handler handler = new Handler();
        DelayedWakeLock delayedWakeLock = new DelayedWakeLock(handler, WakeLock.createPartial(dozeService2, "Doze"));
        Service wrapIfNeeded = DozeSuspendScreenStatePreventingAdapter.wrapIfNeeded(DozeScreenStatePreventingAdapter.wrapIfNeeded(new DozeBrightnessHostForwarder(dozeService2, host), instance), instance);
        DozeMachine dozeMachine = new DozeMachine(wrapIfNeeded, ambientDisplayConfiguration, delayedWakeLock);
        Part[] partArr = new Part[8];
        partArr[0] = new DozePauser(handler, dozeMachine, alarmManager, instance.getPolicy());
        partArr[1] = new DozeFalsingManagerAdapter(FalsingManagerFactory.getInstance(dozeService));
        Part[] partArr2 = partArr;
        DozeMachine dozeMachine2 = dozeMachine;
        SensorManager sensorManager2 = sensorManager;
        Service service = wrapIfNeeded;
        DelayedWakeLock delayedWakeLock2 = delayedWakeLock;
        Handler handler2 = handler;
        DozeParameters dozeParameters = instance;
        AmbientDisplayConfiguration ambientDisplayConfiguration2 = ambientDisplayConfiguration;
        partArr2[2] = createDozeTriggers(dozeService, sensorManager, host, alarmManager, ambientDisplayConfiguration, instance, handler, delayedWakeLock2, dozeMachine2, dockManager);
        partArr2[3] = createDozeUi(dozeService, host, delayedWakeLock2, dozeMachine2, handler2, alarmManager, dozeParameters);
        Handler handler3 = handler2;
        DozeParameters dozeParameters2 = dozeParameters;
        partArr2[4] = new DozeScreenState(service, handler3, dozeParameters2, delayedWakeLock2);
        partArr2[5] = createDozeScreenBrightness(dozeService, service, sensorManager2, host, dozeParameters2, handler3);
        Part[] partArr3 = partArr2;
        partArr3[6] = new DozeWallpaperState(dozeService, getBiometricUnlockController(dozeService));
        DozeDockHandler dozeDockHandler = new DozeDockHandler(dozeService, dozeMachine2, host, ambientDisplayConfiguration2, handler3, dockManager);
        partArr3[7] = dozeDockHandler;
        DozeMachine dozeMachine3 = dozeMachine2;
        dozeMachine3.setParts(partArr3);
        return dozeMachine3;
    }

    private Part createDozeScreenBrightness(Context context, Service service, SensorManager sensorManager, DozeHost dozeHost, DozeParameters dozeParameters, Handler handler) {
        DozeScreenBrightness dozeScreenBrightness = new DozeScreenBrightness(context, service, sensorManager, DozeSensors.findSensorWithType(sensorManager, context.getString(R$string.doze_brightness_sensor_type)), dozeHost, handler, dozeParameters.getPolicy());
        return dozeScreenBrightness;
    }

    private DozeTriggers createDozeTriggers(Context context, SensorManager sensorManager, DozeHost dozeHost, AlarmManager alarmManager, AmbientDisplayConfiguration ambientDisplayConfiguration, DozeParameters dozeParameters, Handler handler, WakeLock wakeLock, DozeMachine dozeMachine, DockManager dockManager) {
        DozeTriggers dozeTriggers = new DozeTriggers(context, dozeMachine, dozeHost, alarmManager, ambientDisplayConfiguration, dozeParameters, sensorManager, handler, wakeLock, true, dockManager);
        return dozeTriggers;
    }

    private Part createDozeUi(Context context, DozeHost dozeHost, WakeLock wakeLock, DozeMachine dozeMachine, Handler handler, AlarmManager alarmManager, DozeParameters dozeParameters) {
        DozeUi dozeUi = new DozeUi(context, alarmManager, dozeMachine, wakeLock, dozeHost, handler, dozeParameters, KeyguardUpdateMonitor.getInstance(context));
        return dozeUi;
    }

    public static DozeHost getHost(DozeService dozeService) {
        return (DozeHost) ((SystemUIApplication) dozeService.getApplication()).getComponent(DozeHost.class);
    }

    public static BiometricUnlockController getBiometricUnlockController(DozeService dozeService) {
        return (BiometricUnlockController) ((SystemUIApplication) dozeService.getApplication()).getComponent(BiometricUnlockController.class);
    }
}
