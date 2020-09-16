attribute vec4 vPosition;

//接收纹理坐标，接收采样器采样图片的图标
attribute vec2 vCoord;
//传给片元着色器像素点
varying vec2 aCoord;
void main() {
    gl_Position = vPosition;
}