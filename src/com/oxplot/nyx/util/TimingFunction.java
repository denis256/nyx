package com.oxplot.nyx.util;

public abstract class TimingFunction {

  public static final TimingFunction EASE = new Bezier(.25, .1, .25, 1);
  public static final TimingFunction EASE_IN = new Bezier(.42, 0, 1, 1);
  public static final TimingFunction EASE_OUT = new Bezier(0, 0, .58, 1);
  public static final TimingFunction EASE_IN_OUT = new Bezier(.42, 0, .58, 1);
  public static final TimingFunction LINEAR = new Linear();

  private static class Linear extends TimingFunction {
    public double at(double t) {
      return t;
    }
  }

  private static class Bezier extends TimingFunction {

    private double Ax, Bx, Cx, Ay, By, Cy;

    public Bezier(double x1, double y1, double x2, double y2) {
      Cx = 3 * x1;
      Bx = 3 * (x2 - x1) - Cx;
      Ax = 1 - Cx - Bx;

      Cy = 3 * y1;
      By = 3 * (y2 - y1) - Cy;
      Ay = 1 - Cy - By;
    }

    private double bezierXDer(double t) {
      return Cx + t * (2 * Bx + 3 * Ax * t);
    }

    private double bezierX(double t) {
      return t * (Cx + t * (Bx + t * Ax));
    }

    private double bezierY(double t) {
      return t * (Cy + t * (By + t * Ay));
    }

    private double findXFor(double t) {
      double x = t, i = 0, z;
      while (i < 5) {
        z = bezierX(x) - t;
        if (Math.abs(z) < 1e-3)
          break;

        x = x - z / bezierXDer(x);
        i++;
      }
      return x;
    }

    public double at(double t) {
      return bezierY(findXFor(t));
    }

  }

  public abstract double at(double t);

  public static TimingFunction createBezier(double x1, double y1, double x2,
      double y2) {
    return new Bezier(x1, y1, x2, y2);
  }

}
