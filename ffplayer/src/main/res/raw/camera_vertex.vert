//将Java层顶点坐标绑定到该变量，确定要绘制的形状
attribute vec4 vPosition;


//接收Java层的摄像头纹理坐标
attribute vec4 vCoord;

//摄像头的纹理矩阵
uniform mat4 vMatrix;

//传给片元着色器像素点，摄像头采集的图像的坐标系是二维的，所以只有两个点表示坐标
varying vec2 aCoord;
void main() {
    //gl_Position是OpenGL渲染程序的内置变量，我们把Java层的顶点数据通过顶点坐标缓冲区赋值给该变量
    //opengl根据gl_Position确定要绘制的形状
    gl_Position = vPosition;
    //只保留x和y分量
    aCoord = (vMatrix * vCoord).xy;
}