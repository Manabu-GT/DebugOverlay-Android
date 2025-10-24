package com.ms_square.debugoverlay;

import android.app.Application;
import android.content.Context;
import android.support.test.runner.AndroidJUnitRunner;

// This is specified in the build.gradle file as the test runner.
public class DebugOverlayTestRunner extends AndroidJUnitRunner {

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.newApplication(cl, Application.class.getName(), context);
    }
}
