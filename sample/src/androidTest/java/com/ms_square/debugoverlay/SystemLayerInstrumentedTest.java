package com.ms_square.debugoverlay;

import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.ms_square.debugoverlay.sample.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
@LargeTest
public class SystemLayerInstrumentedTest extends DebugOverlayInstrumentedTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void installSimple() throws Exception {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                debugOverlay = DebugOverlay.with(getApplication());
                debugOverlay.install();
            }
        });

        waitForOverlay();
    }

    @Override
    ActivityTestRule getActivityRule() {
        return activityRule;
    }

    @Override
    boolean testSystemLayer() {
        return true;
    }
}
