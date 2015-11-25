package com.oxplot.nyx;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.service.dreams.DreamService;
import android.view.View;
import android.view.View.OnLayoutChangeListener;

import com.oxplot.nyx.util.Composition;
import com.oxplot.nyx.util.TimingFunction;
import com.oxplot.nyx.util.Util;

public class NyxDreamService extends DreamService implements
    SensorEventListener, OnLayoutChangeListener {

  private static final double LIGHT_SENSITIVITY = 2.5;
  private static final int LIGHT_SENSOR_INTERVAL = 250000;
  private static final int UPDATE_INTERVAL = 500;
  private static final int TEXT_CHECK_INTERVAL = 1300;
  private static final int TEXT_CHANGE_DURATION = 1400;
  private static final TimingFunction TEXT_CHANGE_TIMING = TimingFunction.EASE_IN_OUT;
  private static final int BLACKOUT_COLOR = Color.BLACK;
  private Runnable changebackCB;
  private Runnable mainUpdateEvent;
  private Runnable textCheckCB;

  private int curColor;
  private int baseColor;
  private int dimColor;
  private int oldLight;
  private int curLight;
  private String oldText;
  private String curText;
  private boolean curIsPortrait;
  private boolean oldIsPortrait;

  private static final int COLOR_DURATION = 1000;
  private static final TimingFunction COLOR_TIMING = TimingFunction.LINEAR;

  private static final TimingFunction POS_TIMING = TimingFunction.EASE_IN_OUT;
  private static final int POS_DURATION = 5 * 60 * 1000;

  private Composition defaultComposition;

  private NyxView nyxView;

  private Humanifier humanifier;

  private SensorManager sensors;
  private Sensor lightSensor;

  private boolean textChanging = false;

  public void onAttachedToWindow() {
    super.onAttachedToWindow();

    defaultComposition = Util.loadCompositionRes(this,
        R.string.default_composition);

    // callback to restore the color from black to current color after text
    // change

    changebackCB = new Runnable() {
      public void run() {
        nyxView.setBackgroundColor(Color.TRANSPARENT); // XXX unblank screen
        nyxView.setTimingFunction(TEXT_CHANGE_TIMING);
        nyxView.setDuration(TEXT_CHANGE_DURATION);
        nyxView.setBaseColor(curColor);
        nyxView.postDelayed(new Runnable() {
          public void run() {
            textChanging = false;
          }
        }, TEXT_CHANGE_DURATION);
      }
    };

    // main update event

    mainUpdateEvent = new Runnable() {
      public void run() {
        if (!textChanging) {
          if (curLight != oldLight) {
            oldLight = curLight;
            curColor = getLightCompensatedColor(curLight);
            scheduleColorChange(curColor);
          }
          if (!curText.equals(oldText) || oldIsPortrait != curIsPortrait) {
            oldText = curText;
            oldIsPortrait = curIsPortrait;
            scheduleTextChange(curText, curIsPortrait, oldText == null);
          }
        }
        nyxView.postDelayed(this, UPDATE_INTERVAL);
      }
    };

    // text check callback

    textCheckCB = new Runnable() {
      public void run() {
        curText = determineText();
        nyxView.postDelayed(this, TEXT_CHECK_INTERVAL);
      }
    };

    // main day dream behaviors

    setInteractive(false);
    setFullscreen(true);
    setScreenBright(true);

    humanifier = new Humanifier();

    // main nyx view

    nyxView = new NyxView(this);
    setContentView(nyxView);
    nyxView.setDuration(0);
    nyxView.setComposition(defaultComposition);

    // sensors

    sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
    lightSensor = sensors.getDefaultSensor(Sensor.TYPE_LIGHT);
  }

  public void onDetachedFromWindoww() {
    super.onDetachedFromWindow();
  }

  public void onDreamingStarted() {
    super.onDreamingStarted();

    // load the preferences

    loadPrefs();
    curColor = baseColor;
    curText = determineText();
    oldIsPortrait = curIsPortrait = isOrientationPortrait();

    // run the update event loop and register other callbacks

    nyxView.post(mainUpdateEvent);
    nyxView.post(textCheckCB);
    nyxView.addOnLayoutChangeListener(this);
    sensors.registerListener(this, lightSensor, LIGHT_SENSOR_INTERVAL);

  }

  public void onDreamingStopped() {
    super.onDreamingStopped();
    sensors.unregisterListener(this);
    nyxView.removeCallbacks(mainUpdateEvent);
    nyxView.removeCallbacks(textCheckCB);
    nyxView.removeOnLayoutChangeListener(this);
  }

  private void loadPrefs() {
    SharedPreferences prefs = getSharedPreferences(
        getString(R.string.prefs_name), MODE_PRIVATE);
    String cs = prefs.getString("color_scheme",
        getString(R.string.default_color_scheme));
    baseColor = getResources().getColor(
        getResources().getIdentifier("color_scheme_" + cs, "color",
            getPackageName()));
    dimColor = getResources().getColor(
        getResources().getIdentifier("color_scheme_" + cs + "_dim", "color",
            getPackageName()));
    oldLight = -1; // XXX bit of hack forces curColor to be recalculated
  }

  private String determineText() {
    // TODO show other things besides humanified clock
    return "It’s " + humanifier.humanify();
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    curLight = (int) (event.values[0] + 1);
  }

  private void scheduleTextChange(final String text, final boolean isPortrait,
      boolean immediate) {
    Runnable runnable = new Runnable() {
      public void run() {
        double sizeRatio = isPortrait ? 1.5 : 1.0;
        nyxView.setTextSize(sizeRatio);
        nyxView.setText(text);
        nyxView.rerender(changebackCB);
        scheduleRandomGlide(); // XXX take it out of here
      }
    };

    textChanging = true;

    if (immediate) {
      nyxView.setDuration(0);
      nyxView.setBaseColor(BLACKOUT_COLOR);
      runnable.run();
    } else {
      nyxView.setTimingFunction(TEXT_CHANGE_TIMING);
      nyxView.setDuration(TEXT_CHANGE_DURATION);
      nyxView.setBaseColor(BLACKOUT_COLOR);
      nyxView.postDelayed(runnable, TEXT_CHANGE_DURATION);
    }
  }

  private void scheduleColorChange(final int color) {
    nyxView.setTimingFunction(COLOR_TIMING);
    nyxView.setDuration(COLOR_DURATION);
    nyxView.setBaseColor(color);
  }

  private int getLightCompensatedColor(int light) {
    int[] base = new int[] { Color.red(baseColor), Color.green(baseColor),
        Color.blue(baseColor) };
    int[] dim = new int[] { Color.red(dimColor), Color.green(dimColor),
        Color.blue(dimColor) };

    double ratio = Math.min(LIGHT_SENSITIVITY, Math.max(0, Math.log10(light)))
        / LIGHT_SENSITIVITY;
    return Color.rgb((int) Math.min(255, (base[0] - dim[0]) * ratio + dim[0]),
        (int) Math.min(255, (base[1] - dim[1]) * ratio + dim[1]),
        (int) Math.min(255, (base[2] - dim[2]) * ratio + dim[2]));
  }

  @Override
  public void onLayoutChange(View view, int left, int top, int right,
      int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
    curIsPortrait = isOrientationPortrait();
    // XXX lots of hackery is going on here
    if (curIsPortrait != oldIsPortrait) {
      nyxView.setBackgroundColor(BLACKOUT_COLOR); // XXX blank screen
      oldText = null; // force an immediate change (ie no fade out, just fade
                      // in) see mainUpdateEvent
    }
    nyxView.removeCallbacks(mainUpdateEvent); // XXX force immediate event run
    nyxView.post(mainUpdateEvent);
  }

  private boolean isOrientationPortrait() {
    return nyxView.getBottom() - nyxView.getTop() > nyxView.getRight()
        - nyxView.getLeft();
  }

  private void scheduleRandomGlide() {
    double start = Math.random() * 2 - 1;
    double end = start > 0 ? -1 : 1;
    nyxView.setTimingFunction(POS_TIMING);
    nyxView.setDuration(0);
    nyxView.setTextPosition(start);
    nyxView.setDuration(POS_DURATION);
    nyxView.setTextPosition(end);
  }

}
