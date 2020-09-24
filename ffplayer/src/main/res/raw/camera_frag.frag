#extension GL_OES_image_external : require

//定义float数据是什么精度的
precision mediump float;

varying vec2 aCoord;//接收顶点着色器传过来的采样点的坐标

//由于是从Android的surfaceTexture的纹理采集的摄像头数据，需要使用Android的扩展纹理采样器
uniform samplerExternalOES vTexture;
uniform vec4 vColor;
void main() {
    //从摄像头采样得到的数据不是正的，需要进行转换
    //将采样器从摄像头中采集到aCoord对应位置的数据转换为对应的着色器颜色，代替通常Java层设置的颜色值
    gl_FragColor = texture2D(vTexture, aCoord);
}