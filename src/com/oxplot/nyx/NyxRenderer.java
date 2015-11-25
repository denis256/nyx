package com.oxplot.nyx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.oxplot.nyx.util.Interpolator;
import com.oxplot.nyx.util.TimingFunction;
import com.oxplot.nyx.util.Util;

public class NyxRenderer implements GLSurfaceView.Renderer {

  private static final float MAX_BLUR_RADIUS = 25.0f;
  private static final int VERTICES_COUNT = 4;
  private static final int COMPONENT_PER_VERTEX = 4;
  private static final int VERTEX_POS_COMPONENTS = 2;
  private static final int BASE_COLOR = Color.rgb(128, 0, 0);
  private static final float TEXT_SIZE_RATIO = 7.0f;
  private static final float BLUR_RADIUS_RATIO = 0.1f;

  private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
  private FloatBuffer verticesBuffer;

  private Context context;

  public String baseText = "NYX";
  public double baseTextSize = 1.0;

  public Interpolator baseColorIntp;
  public Interpolator baseCompIntp;
  public Interpolator baseTextPosIntp;

  private int viewWidth;
  private int viewHeight;

  private int vShader;
  private int fShader;
  private int program;

  private int uKsIndex;
  private int uColorIndex;
  private int uTextureIndex;
  private int uShiftIndex;
  private int aPosIndex;
  private int aTexIndex;

  private Typeface typeface;
  private TextPaint textPaint;

  private RenderScript rs;
  private ScriptIntrinsicBlur blurer;
  private int textureId;

  private double heightRatio = 1.0;

  public NyxRenderer(Context context) {
    this.context = context;

    // Interpolators

    baseColorIntp = new Interpolator(TimingFunction.LINEAR, new double[] { 0,
        0, 0 }, new double[] { 0, 0, 0 }, new double[] { 1, 1, 1 });
    baseCompIntp = new Interpolator(TimingFunction.LINEAR, new double[] { 0, 0,
        0 }, new double[] { -10, -10, -10 }, new double[] { 10, 10, 10 });
    baseTextPosIntp = new Interpolator(TimingFunction.LINEAR,
        new double[] { 0 }, new double[] { -2 }, new double[] { 2 });

    // 2D stuff
    setup2DGFX();
  }

  private void interpolate() {
    baseColorIntp.step();
    baseCompIntp.step();
    baseTextPosIntp.step();
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    interpolate();
    double[] c = baseColorIntp.cur, k = baseCompIntp.cur, p = baseTextPosIntp.cur;
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    GLES20.glUniform3f(uColorIndex, (float) c[0], (float) c[1], (float) c[2]);
    GLES20.glUniform3f(uKsIndex, (float) k[0], (float) k[1], (float) k[2]);
    GLES20.glUniform1f(uShiftIndex, (float) (p[0] * heightRatio));
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTICES_COUNT);
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    this.viewWidth = width;
    this.viewHeight = height;
    GLES20.glViewport(0, 0, width, height);
    updateBase();
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    // set the background color
    GLES20.glClearColor(0.0f, 0, 0, 1.0f);

    // enable blending
    GLES20.glEnable(GLES20.GL_BLEND);
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

    // texture scaling configuration
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
        GLES20.GL_LINEAR_MIPMAP_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
        GLES20.GL_LINEAR);

    // compile the shaders and link the program
    compileAndLink();

    viewWidth = viewHeight = 0;

  }

  public void updateBase() {

    if (viewHeight < 1 || viewWidth < 1)
      return;

    // build the 2D graphics

    float finalTextSizeRatio = (float) (TEXT_SIZE_RATIO * (1 / baseTextSize));
    float finalBlurRadius = Math.min((viewWidth / finalTextSizeRatio)
        * BLUR_RADIUS_RATIO, MAX_BLUR_RADIUS);
    double finalSizeRatio = ((finalBlurRadius * finalTextSizeRatio) / BLUR_RADIUS_RATIO)
        / viewWidth;

    viewWidth = (int) Math.round(finalSizeRatio * viewWidth);
    viewHeight = (int) Math.round(finalSizeRatio * viewHeight);

    Bitmap textBitmap = Bitmap.createBitmap(viewWidth, viewHeight,
        Bitmap.Config.ARGB_8888);
    textBitmap.eraseColor(Color.TRANSPARENT);
    textPaint.setTextSize(viewWidth / finalTextSizeRatio);

    Canvas canvas = new Canvas(textBitmap);
    StaticLayout wrappedText = new StaticLayout(baseText, textPaint,
        (int) (viewWidth - viewWidth / (finalTextSizeRatio * 2)),
        Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
    canvas.save();
    canvas.translate(viewWidth / (finalTextSizeRatio * 4.0f), viewHeight / 2.0f
        - wrappedText.getHeight() / 2.0f);
    wrappedText.draw(canvas);
    canvas.restore();

    heightRatio = 1.0 - (Math.min(wrappedText.getHeight(), viewHeight) / (double) viewHeight);

    // apply blur - FIXME this should run on a background thread as not to
    // interrupt the rendering thread

    Allocation inAlloc = Allocation.createFromBitmap(rs, textBitmap);
    Allocation outAlloc = Allocation.createTyped(rs, inAlloc.getType());
    blurer.setInput(inAlloc);
    blurer.setRadius(finalBlurRadius);
    blurer.forEach(outAlloc);
    outAlloc.copyTo(textBitmap);
    inAlloc.destroy();
    outAlloc.destroy();

    // create the opengl texture

    if (textureId != 0) {
      GLES20.glDeleteTextures(1, new int[] { textureId }, 0);
    }
    int[] textureIds = new int[1];
    GLES20.glGenTextures(1, textureIds, 0);
    textureId = textureIds[0];
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textBitmap, 0);
    GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
    GLES20.glUniform1i(uTextureIndex, 0);

  }

  private void setup2DGFX() {

    // typeface setup
    typeface = Typeface.createFromAsset(context.getAssets(), "bebas.otf");

    // text paint setup
    textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
    textPaint.setHinting(TextPaint.HINTING_ON);
    textPaint.setSubpixelText(false);
    textPaint.setTypeface(typeface);
    // we set this specific color in order to find how it's affected by the blur
    // so we can change the color of the blurred image without needing to redraw
    // the text and reblur it - see the fragment shader to see how this color
    // value is used to achieve the above
    textPaint.setColor(BASE_COLOR);

    rs = RenderScript.create(context);
    blurer = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
  }

  private void compileAndLink() {
    // Compile vertex shader
    String vShaderSrc = Util.loadRawRes(context, R.raw.vertex_shader);
    vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
    GLES20.glShaderSource(vShader, vShaderSrc);
    GLES20.glCompileShader(vShader);
    // System.out.println(GLES20.glGetShaderInfoLog(vShader)); // XXX

    // Compile fragment shader
    String fShaderSrc = Util.loadRawRes(context, R.raw.fragment_shader);
    fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
    GLES20.glShaderSource(fShader, fShaderSrc);
    GLES20.glCompileShader(fShader);
    // System.out.println(GLES20.glGetShaderInfoLog(fShader)); // XXX

    // Link the shaders into a program
    program = GLES20.glCreateProgram();
    GLES20.glAttachShader(program, vShader);
    GLES20.glAttachShader(program, fShader);
    GLES20.glLinkProgram(program);
    // GLES20.glValidateProgram(program);
    // System.out.println(GLES20.glGetProgramInfoLog(program)); // XXX
    GLES20.glUseProgram(program);

    // find all uniform locations
    uKsIndex = GLES20.glGetUniformLocation(program, "u_ks");
    uColorIndex = GLES20.glGetUniformLocation(program, "u_color");
    uTextureIndex = GLES20.glGetUniformLocation(program, "u_texture");
    uShiftIndex = GLES20.glGetUniformLocation(program, "u_sh");

    aPosIndex = GLES20.glGetAttribLocation(program, "a_position");
    aTexIndex = GLES20.glGetAttribLocation(program, "a_texpos");

    // setup vertex shader attributes

    // @formatter:off
    float[] vertices = new float[] {
        -1.0f, -1.0f, 0, 1.0f,
        -1.0f, 1.0f, 0.0f, 0.0f,
        1.0f, -1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 0.0f
    };
    // @formatter:on

    verticesBuffer = ByteBuffer
        .allocateDirect(vertices.length * BYTES_PER_FLOAT)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices);

    verticesBuffer.position(0);
    GLES20.glVertexAttribPointer(aPosIndex, VERTEX_POS_COMPONENTS,
        GLES20.GL_FLOAT, false, COMPONENT_PER_VERTEX * BYTES_PER_FLOAT,
        verticesBuffer);
    verticesBuffer.position(VERTEX_POS_COMPONENTS);
    GLES20.glVertexAttribPointer(aTexIndex, VERTEX_POS_COMPONENTS,
        GLES20.GL_FLOAT, false, COMPONENT_PER_VERTEX * BYTES_PER_FLOAT,
        verticesBuffer);
    GLES20.glEnableVertexAttribArray(aPosIndex);
    GLES20.glEnableVertexAttribArray(aTexIndex);
  }

}
