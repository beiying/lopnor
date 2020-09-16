//定义float数据是什么精度的
precision mediump float;

varying vec2 aCoord;//接收顶点着色器传过来的采样点的坐标

//OpenGL内部采样器，不是从Android的surfaceTexture的纹理采数据，所以不需要Android的扩展纹理采样器
//代表采样器采集的摄像头的数据，不需要从Java层传入颜色数据
uniform sampler2D vTexture;
uniform vec4 vColor;
void main() {
    //从摄像头采样得到的数据不是正的，需要进行转换
    //将采样器从摄像头中采集到aCoord对应位置的数据转换为对应的着色器颜色，代替通常Java层设置的颜色值
    gl_FragColor = texture2D(vTexture, aCoord);
}