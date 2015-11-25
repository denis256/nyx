attribute vec4 a_position;
attribute vec2 a_texpos;
varying vec2 v_texpos;

uniform float u_sh;

void main()
{
  v_texpos = a_texpos;
  gl_Position = a_position + vec4(0, u_sh, 0, 0);
  
}