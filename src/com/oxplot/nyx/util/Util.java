package com.oxplot.nyx.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import android.content.Context;

public class Util {
  public static Charset utf8 = Charset.forName("UTF-8");

  public static String loadRawRes(Context context, int resId) {

    try {
      StringBuilder builder = new StringBuilder();
      InputStream stream = context.getResources().openRawResource(resId);
      byte[] buffer = new byte[2048];
      int bytesRead = stream.read(buffer);
      while (bytesRead > 0) {
        builder.append(new String(buffer, 0, bytesRead, utf8));
        bytesRead = stream.read(buffer);
      }
      stream.close();
      return builder.toString();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static Composition loadCompositionRes(Context context, int resId) {
    String[] ks = context.getString(resId).split(",");
    return new Composition(Double.parseDouble(ks[0]),
        Double.parseDouble(ks[1]), Double.parseDouble(ks[2]));
  }

}
