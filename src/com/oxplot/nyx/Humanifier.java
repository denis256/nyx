package com.oxplot.nyx;

import java.util.Calendar;

public class Humanifier {

  private static final String[] HOURS = new String[] { "one", "two", "three",
      "four", "five", "six", "seven", "eight", "nine", "ten", "eleven" };
  private static final String[] MINUTES = new String[] { "half", "twenty five",
      "twenty", "a quarter", "ten", "five", "" };

  public Humanifier() {
  }

  public String humanify(Calendar cal) {
    int m = cal.get(Calendar.MINUTE) / 5;
    String mStr = MINUTES[Math.abs(6 - m)];

    if (m > 0 && m <= 6)
      mStr += " past ";
    else if (m > 6 && m < 12)
      mStr += " to ";

    int h = (cal.get(Calendar.HOUR_OF_DAY) + (m > 6 ? 1 : 0)) % 24;
    String hStr;
    if (h % 12 > 0)
      hStr = HOURS[h % 12 - 1] + (m == 0 ? " o’clock" : "");
    else
      hStr = h == 12 ? "noon" : "midnight";

    return mStr + hStr;
  }

  public String humanify() {
    return humanify(Calendar.getInstance());
  }
}
