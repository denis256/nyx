package com.oxplot.nyx;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;

import com.oxplot.nyx.util.TimingFunction;
import com.oxplot.nyx.util.Util;

public class DialsActivity extends Activity implements
    OnSharedPreferenceChangeListener {
  private static final int OSC_PERIOD = 1300;
  private static final TimingFunction OSC_TIMING = TimingFunction.EASE_IN_OUT;

  private SharedPreferences prefs;
  private String[] colorSchemes;
  private LinearLayout schemesLL;
  private NyxView nyxView;
  private int[] colors = new int[2];
  private int curColor = 0;
  private Runnable colorOscillator;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dials);

    schemesLL = (LinearLayout) findViewById(R.id.colors);
    prefs = getSharedPreferences(getString(R.string.prefs_name), MODE_PRIVATE);
    colorSchemes = getResources().getStringArray(R.array.color_schemes);

    // load the color schemes

    OnClickListener ocl = new OnClickListener() {
      public void onClick(View v) {
        String sc = (String) v.getTag();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("color_scheme", sc);
        editor.commit();
      }
    };

    // color oscillator

    colorOscillator = new Runnable() {
      public void run() {
        nyxView.setDuration(OSC_PERIOD);
        nyxView.setTimingFunction(OSC_TIMING);
        nyxView.setBaseColor(colors[(++curColor) % 2]);
        nyxView.postDelayed(this, OSC_PERIOD);
      }
    };

    // construct the list of color schemes

    for (String cs : colorSchemes) {
      View v = getLayoutInflater().inflate(R.layout.color_scheme_item, null);
      RadioButton rb = (RadioButton) v.findViewById(R.id.color_scheme_name);
      rb.setTag(cs);
      rb.setText(getString(getResources().getIdentifier("color_scheme_" + cs,
          "string", getPackageName())));

      rb.setOnClickListener(ocl);
      schemesLL.addView(v);
    }

    nyxView = (NyxView) findViewById(R.id.nyxview);
    nyxView.setDuration(0);
    nyxView.setComposition(Util.loadCompositionRes(this,
        R.string.default_composition));
    nyxView.setText(getString(R.string.configure_me));
    nyxView
        .setTextSize(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 1.0
            : 1.5);
    nyxView.rerender();

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.dials, menu);
    return true;
  }

  @Override
  protected void onRestoreInstanceState(final Bundle savedInstanceState) {
    if (savedInstanceState != null && savedInstanceState.containsKey("scroll")) {
      final ScrollView sv = (ScrollView) findViewById(R.id.scroll);
      sv.post(new Runnable() {
        public void run() {
          sv.scrollTo(0, savedInstanceState.getInt("scroll"));
        }
      });
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putInt("scroll",
        ((ScrollView) findViewById(R.id.scroll)).getScrollY());
  }

  @Override
  public void onSharedPreferenceChanged(
      final SharedPreferences sharedPreferences, final String key) {
    if ("color_scheme".equals(key)) {
      refreshScheme();
      nyxView.removeCallbacks(colorOscillator);
      nyxView.post(colorOscillator);
    }
  }

  private void refreshScheme() {
    String newScheme = prefs.getString("color_scheme", getResources()
        .getString(R.string.default_color_scheme));
    int childCount = schemesLL.getChildCount();
    for (int i = 0; i < childCount; i++) {
      RadioButton rb = (RadioButton) schemesLL.getChildAt(i).findViewById(
          R.id.color_scheme_name);
      String sc = (String) rb.getTag();
      rb.setChecked(sc.equals(newScheme));
      if (sc.equals(newScheme)) {
        colors[0] = getResources().getColor(
            getResources().getIdentifier("color_scheme_" + sc, "color",
                getPackageName()));
        colors[1] = getResources().getColor(
            getResources().getIdentifier("color_scheme_" + sc + "_dim",
                "color", getPackageName()));
      }
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    prefs.unregisterOnSharedPreferenceChangeListener(this);
    nyxView.removeCallbacks(colorOscillator);
  }

  @Override
  protected void onResume() {
    super.onResume();
    prefs.registerOnSharedPreferenceChangeListener(this);
    nyxView.post(colorOscillator);
    refreshScheme();
  }

}
