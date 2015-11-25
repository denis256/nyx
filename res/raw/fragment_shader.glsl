// Hey you curious soul. I'm so flattered that you cared enough
// to get down and dirty. No, I'm not gonna say you can't do this
// , you can't do that. Go mix it up and enjoy it too.

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
  for (int i = 0; i < 4; i++) {
    c[i] = clamp(float(u_ks[0] * c[i] * c[i] + u_ks[1] * c[i] + u_ks[2]), 0.0, 1.0);
  }
  gl_FragColor = c;
  
}