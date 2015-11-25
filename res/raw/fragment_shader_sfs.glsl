// SFS

precision mediump float;

uniform vec3 u_color;
uniform vec3 u_ks;

uniform sampler2D u_texture;
varying vec2 v_texpos;

void main()
{
  vec4 c = texture2D(u_texture, v_texpos);
  float r = c.r;
  for (int i = 0; i < 3; i++) {
     c[i] = clamp((u_color[i] * r) / 0.5, 0.0, 1.0);
  }
  float val = (c[0] + c[1] + c[2]) / 3.0;
  float rval = u_ks[0] * val * val + u_ks[1] * val + u_ks[2];
  for (int i = 0; i < 3; i++) {
    c[i] = clamp((rval - 0.5) * c[i], 0.0, 1.0);
  }
  gl_FragColor = c;
  
}