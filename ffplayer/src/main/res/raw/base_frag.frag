//float数据是什么精度的
precision mediump float;
varying vec2 aCoord;//采样点的坐标

//采样器，不是从Android的surfaceTexture的纹理采数据，所以不需要Android的扩展纹理采样器

uniform sampler2D vTexture；
uniform vec4 vColor;
void main() {
    //采样器采集aCoord的像素
    gl_FragColor = texture2D(vTexture, aCoord);
}