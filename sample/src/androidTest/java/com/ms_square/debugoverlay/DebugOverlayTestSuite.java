package com.ms_square.debugoverlay;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

// Runs all debug overlay tests.
@RunWith(Suite.class)
@Suite.SuiteClasses({SystemLayerInstrumentedTest.class, AppLayerInstrumentedTest.class})
public class DebugOverlayTestSuite {
}
