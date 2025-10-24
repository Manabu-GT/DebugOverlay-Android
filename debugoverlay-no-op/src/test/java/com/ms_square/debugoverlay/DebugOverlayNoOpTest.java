package com.ms_square.debugoverlay;

import android.app.Application;
import android.graphics.Color;

import com.ms_square.debugoverlay.modules.CpuFreqModule;
import com.ms_square.debugoverlay.modules.CpuUsageModule;
import com.ms_square.debugoverlay.modules.FpsModule;
import com.ms_square.debugoverlay.modules.LogcatLine;
import com.ms_square.debugoverlay.modules.LogcatLineColorScheme;
import com.ms_square.debugoverlay.modules.LogcatLineFilter;
import com.ms_square.debugoverlay.modules.LogcatModule;
import com.ms_square.debugoverlay.modules.MemInfoModule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Simply check calling public APIs do not result in error and they do absolutely nothing.
 */
@RunWith(MockitoJUnitRunner.class)
public class DebugOverlayNoOpTest {

    @Mock
    Application mockApplication;

    @Mock
    ViewModule mockViewModule;

    @Mock
    DataModule mockDataModule;

    @Mock
    OverlayModule mockOverlayModule;

    @Test
    public void cpuUsageModuleConstructors() {
        new CpuUsageModule();
        new CpuUsageModule(0);
        new CpuUsageModule(0, 0);
        new CpuUsageModule(mockViewModule);
        new CpuUsageModule(0, mockViewModule);

        verifyNoInteractions(mockViewModule);
    }

    @Test
    public void cpuFreqModuleConstructors() {
        new CpuFreqModule();
        new CpuFreqModule(0);
        new CpuFreqModule(0, 0);
        new CpuFreqModule(mockViewModule);
        new CpuFreqModule(0, mockViewModule);

        verifyNoInteractions(mockViewModule);
    }

    @Test
    public void fpsModuleConstructors() {
        new FpsModule();
        new FpsModule(0);
        new FpsModule(0, 0);
        new FpsModule(mockViewModule);
        new FpsModule(0, mockViewModule);

        verifyNoInteractions(mockViewModule);
    }

    @Test
    public void logCatModuleConstructors() {
        LogcatLineFilter mockedFilter = Mockito.mock(LogcatLineFilter.class);
        LogcatLineColorScheme mockedColorScheme = Mockito.mock(LogcatLineColorScheme.class);
        new LogcatModule();
        new LogcatModule(0);
        new LogcatModule(0, mockedFilter);
        new LogcatModule(0, mockedColorScheme);
        new LogcatModule(0, mockedFilter, mockedColorScheme);

        verifyNoInteractions(mockViewModule, mockedFilter, mockedColorScheme);
    }

    @Test
    public void memInfoModuleConstructors() {
        new MemInfoModule(mockApplication);
        new MemInfoModule(mockApplication, 0);
        new MemInfoModule(mockApplication, 0, 0);
        new MemInfoModule(mockApplication, mockViewModule);
        new MemInfoModule(mockApplication, 0, mockViewModule);

        verifyNoInteractions(mockApplication, mockViewModule);
    }

    @Test
    public void installSimple() {
        DebugOverlay.with(mockApplication).install();
        verifyNoInteractions(mockApplication);
    }

    @Test
    public void installModule() {
        new DebugOverlay.Builder(mockApplication)
                .modules(mockOverlayModule)
                .build()
                .install();
        verifyNoInteractions(mockApplication, mockOverlayModule);
    }

    @Test
    public void installCustomModule() {
        new DebugOverlay.Builder(mockApplication)
                .modules(new OverlayModule(mockDataModule, mockViewModule){})
                .build()
                .install();

        verifyNoInteractions(mockApplication, mockDataModule, mockViewModule);
    }

    @Test
    public void installAppLayer() {
        new DebugOverlay.Builder(mockApplication)
                .allowSystemLayer(false)
                .modules(mockOverlayModule)
                .build()
                .install();

        verifyNoInteractions(mockApplication, mockOverlayModule);
    }

    @Test
    public void installModulesWithPosition() {
        assertEquals(9, Position.values().length);

        new DebugOverlay.Builder(mockApplication)
                .modules(mockOverlayModule,
                        new MemInfoModule(mockApplication),
                        new FpsModule())
                .position(Position.values()[0])
                .build()
                .install();

        verifyNoInteractions(mockApplication, mockOverlayModule);
    }

    @Test
    public void installWithNotification() {
        new DebugOverlay.Builder(mockApplication)
                .notification(false)
                .build()
                .install();

        new DebugOverlay.Builder(mockApplication)
                .notification(true)
                .build()
                .install();

        new DebugOverlay.Builder(mockApplication)
                .notification(true, "test")
                .build()
                .install();

        verifyNoInteractions(mockApplication);
    }

    @Test
    public void installLogcatModuleWCustomColor() {
        new DebugOverlay.Builder(mockApplication)
                .modules(new LogcatModule(10, (LogcatLineColorScheme) (priority, tag) -> Color.CYAN))
                .build();

        verifyNoInteractions(mockApplication);
    }

    @Test
    public void installLogcatModuleWCustomFilter() {
        new DebugOverlay.Builder(mockApplication)
                .modules(new LogcatModule(10, new LogcatLineFilter.SimpleLogcatLineFilter(LogcatLine.Priority.INFO)))
                .build()
                .install();

        new DebugOverlay.Builder(mockApplication)
                .modules(new LogcatModule(10, (LogcatLineFilter) (priority, tag) -> false))
                .build()
                .install();

        verifyNoInteractions(mockApplication);
    }

    @Test
    public void installModulesCustomStyle() {
        new DebugOverlay.Builder(mockApplication)
                .modules(mockOverlayModule,
                        new CpuUsageModule(),
                        new MemInfoModule(mockApplication),
                        new FpsModule())
                .position(Position.TOP_START)
                .bgColor(0)
                .textColor(Color.MAGENTA)
                .textSize(14f)
                .textAlpha(.8f)
                .build()
                .install();

        verifyNoInteractions(mockApplication, mockOverlayModule);
    }

    @Test
    public void logging() {
        DebugOverlay.with(mockApplication).install();
        DebugOverlay.enableDebugLogging(false);
        verifyNoInteractions(mockApplication);
        DebugOverlay.enableDebugLogging(true);
        verifyNoInteractions(mockApplication);
    }
}
