package com.oxplot.nyx.util;

import java.util.Arrays;

public class Interpolator {
  public TimingFunction timing;
  private double[] min;
  private double[] max;
  private double[] tgt;
  public double[] cur;
  private double[] src;
  private long srcts;
  private long tgtts;

  public Interpolator(TimingFunction tf, double[] initial, double[] min,
      double[] max) {
    timing = tf;
    this.min = min;
    this.max = max;
    cur = Arrays.copyOf(initial, initial.length);
    tgt = Arrays.copyOf(initial, initial.length);
    src = Arrays.copyOf(initial, initial.length);
    srcts = 0;
    tgtts = 1;
  }

  public void step() {
    double passed = (System.currentTimeMillis() - srcts)
        / ((double) (tgtts - srcts));
    passed = timing.at(Math.max(0, Math.min(1, passed))); // clamp it
    for (int i = 0; i < cur.length; i++) {
      cur[i] = passed * (tgt[i] - src[i]) + src[i];
      cur[i] = Math.max(min[i], Math.min(max[i], cur[i]));
    }
  }

  public void go(double[] tgt, long dur) {
    if (dur < 0)
      throw new IllegalArgumentException();
    else if (dur == 0) {
      System.arraycopy(tgt, 0, cur, 0, tgt.length);
    } else {
      srcts = System.currentTimeMillis();
      tgtts = srcts + dur;
      System.arraycopy(cur, 0, src, 0, cur.length);
    }
    System.arraycopy(tgt, 0, this.tgt, 0, tgt.length);
  }
}
