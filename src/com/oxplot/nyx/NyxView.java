package com.oxplot.nyx;

import com.oxplot.nyx.util.Composition;
import com.oxplot.nyx.util.TimingFunction;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class NyxView extends GLSurfaceView {

  private NyxRenderer renderer;

  private TimingFunction futureTimingFn;
  private int futureDuration;
  private String futureText;
  private double futureTextSize;

  public NyxView(Context context, AttributeSet attrs) {
    super(context, attrs);
    if (isInEditMode())
      return;
    setEGLContextClientVersion(2);
    renderer = new NyxRenderer(context);
    futureTextSize = renderer.baseTextSize;
    futureTimingFn = renderer.baseColorIntp.timing;
    futureDuration = 1;
    setRenderer(renderer);
  }

  public NyxView(Context context) {
    this(context, null);
  }

  @Override
  public void onPause() {
    if (renderer != null)
      super.onPause();
  }

  @Override
  public void onResume() {
    if (renderer != null)
      super.onResume();
  }

  /**
   * Set the text color. This will be animated according the timing function and
   * duration set by {@link NyxView#setTimingFunction(TimingFunction)} and
   * {@link NyxView#setDuration(int)}.
   * 
   * @param color
   *          Color to set.
   */
  public void setBaseColor(int color) {
    setBaseColor(Color.red(color) / 255.0, Color.green(color) / 255.0,
        Color.blue(color) / 255.0);
  }

  public void setBaseColor(final double r, final double g, final double b) {
    final TimingFunction tf = futureTimingFn;
    final int dur = futureDuration;
    queueEvent(new Runnable() {
      public void run() {
        renderer.baseColorIntp.timing = tf;
        renderer.baseColorIntp.go(new double[] { r, g, b }, dur);
      }
    });
  }

  /**
   * Set the composition. This will set the composition by animating it
   * according to the timing function and duration set by
   * {@link NyxView#setTimingFunction(TimingFunction)} and
   * {@link NyxView#setDuration(int)}.
   * 
   * @param c
   *          Composition
   */
  public void setComposition(Composition c) {
    final double k1 = c.k1, k2 = c.k2, k3 = c.k3;
    final TimingFunction tf = futureTimingFn;
    final int dur = futureDuration;
    queueEvent(new Runnable() {
      public void run() {
        renderer.baseCompIntp.timing = tf;
        renderer.baseCompIntp.go(new double[] { k1, k2, k3 }, dur);
      }
    });
  }

  /**
   * Set the duration of the future animations in milliseconds.
   * 
   * @param duration
   *          Duration of animation in milliseconds.
   */
  public void setDuration(int duration) {
    futureDuration = duration;
  }

  /**
   * Set the timing function for future animations.
   * 
   * @param tf
   *          Timing function.
   */
  public void setTimingFunction(TimingFunction tf) {
    futureTimingFn = tf;
  }

  /**
   * Set the text content. To see the effect of this operation, you must call
   * {@link NyxView#rerender()}.
   * 
   * @param text
   *          Text to set.
   */
  public void setText(String text) {
    futureText = text;
  }

  /**
   * Set the relative position of the text. This causes the text to move
   * according to the timing function set by
   * {@link NyxView#setTimingFunction(TimingFunction)}.
   * 
   * @param pos
   *          Relative destination position of text. <code>0</code> for exactly
   *          the middle of the screen. <code>-1</code> for top of the text
   *          aligned with top of the screen and <code>1</code> for bottom of
   *          text aligned with bottom of the screen.
   */
  public void setTextPosition(final double pos) {
    final TimingFunction tf = futureTimingFn;
    final int dur = futureDuration;
    queueEvent(new Runnable() {
      public void run() {
        renderer.baseTextPosIntp.timing = tf;
        renderer.baseTextPosIntp.go(new double[] { pos }, dur);
      }
    });
  }

  /**
   * Set the size of text. To see the effect of this operation, you must call
   * {@link NyxView#rerender()}.
   * 
   * @param size
   *          Relative size of the text. <code>1</code> for normal size. Less
   *          than <code>1</code> for smaller and higher than <code>1</code> for
   *          larger text size.
   */
  public void setTextSize(double size) {
    futureTextSize = size;
  }

  /**
   * Rerender the content offline on a background thread and upon completion,
   * update the view.
   */
  public void rerender() {
    rerender(null);
  }

  public void rerender(final Runnable callback) {
    final String text = futureText;
    final double size = futureTextSize;
    queueEvent(new Runnable() {
      public void run() {
        renderer.baseText = text;
        renderer.baseTextSize = size;
        renderer.updateBase();
        if (callback != null)
          post(callback);
      }
    });
  }
  
}
