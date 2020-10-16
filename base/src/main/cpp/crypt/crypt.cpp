#include <jni.h>
#include <string>
#include <android/log.h>
#include "Md5.h"
//
// Created by beiying on 2020/10/15.
//
using namespace std;
static char* EXTRA_SIGNATURE = "BEIYING";
static char* PACKAGE_NAME = "com.beiying.lopnor";
static char* APP_SIGNATURE = "3082030d308201f5a003020102020419f3ba88300d06092a864886f70d01010b05003037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f6964204465627567301e170d3135303731333134343435395a170d3435303730353134343435395a3037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f696420446562756730820122300d06092a864886f70d01010105000382010f003082010a02820101008ceedaaf81bbff814485b1ba7b4ab07266837e221e9802602fd9d3ceeeb119876e9840dd22ec9677a1845bdb9280e3f0c4030cd0645692d3fc9731d98c6959025139eb014b8833c9a9f33bce9c8f671462d1c0a0bfeee80ca1eabc3a6956c6e8d1245a28e5fc2b8c08b1e83c95701b228bdc2b67d05b5a771d83e51b6914c6e75fbf117eb0c10acbcec726917ae5bd377b2d72664e0c3029dfc250f2e4cc81ea274c46f376c94d37f7269245a78ae148594b20c9e0fe316d3fb928eeef73336648264d199982274353cd1a7f48d76b7c0f83d821536a6facf451d8c8741cf2a034e425a7f2c0aae885ff8a8b6abded1520215628a258829dcb5ef48d7851c8650203010001a321301f301d0603551d0e04160414681b8308682b249b122ea490f01b96fc7bb50923300d06092a864886f70d01010b050003820101000b90cfe324b2780f27dd3a87f62402edaa0f0dd9849defb493c7e5c1b1b5f483be0fd7ed5a305317b6f2ed459c574c68b8fda719c75e4a4f1e15fc0e00d71a9bf98659901632c3befd9490052734bfe3db1f57524b4d51f9f9c7d7ee0968c4b4bd03b72accb4fad02bf1b2f5ab92188862fed8e07c0a0fcc60702a58d41bf65194a2a89f0d9c78fc768b368280195dbef95e72e231cb31d66f741c9843b44796d54b95e6981b5f08f35289d90b4925ea6d90788ee8e839e9b2bd06a405bcdafc5c93d2b56164fd07072fdd4b929ab72ed04210c63d567e71081f9e2ee252cfa836c75bbc9e4d9da4e9a6d563324e00f2d759b703c1ccea64738a839ce3e82000";
static int is_verify = 0;
extern "C"
JNIEXPORT jstring JNICALL
Java_com_beiying_lopnor_base_crypt_CryptUtil_signatureParams(JNIEnv *env, jobject thiz,
                                                             jstring _params) {

    if (is_verify == 0) {
        return env->NewStringUTF("error_signature");
    }
    const char* params = env->GetStringUTFChars(_params, 0);

    //增加一些签名规则：待加密的字符串前面混入一些其他字符串，去掉待加密字符串的后两位
    string signature_str(params);
    signature_str.insert(0, EXTRA_SIGNATURE);
    signature_str = signature_str.substr(0, signature_str.length() - 2);

    //开始Md5加密
    MD5_CTX *ctx = new MD5_CTX();
    MD5Init(ctx);
    MD5Update(ctx, (unsigned char *) signature_str.c_str(), signature_str.length());
    unsigned char digest[16];
    MD5Final(digest, ctx);

    //生产32位的Md5加密字符串
    char md5_str[32];
    for (int i = 0; i < 16;i++) {
        sprintf(md5_str, "%s%02x", md5_str, digest[i]);
    }

    env->ReleaseStringUTFChars(_params, params);
    return env->NewStringUTF(md5_str);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_beiying_lopnor_base_crypt_CryptUtil_signatureVerify(JNIEnv *env, jobject thiz,
                                                             jobject context) {
    //1、获取包名
    jclass j_clz = env->GetObjectClass(context);
    jmethodID j_mid = env->GetMethodID(j_clz, "getPackageName", "()Ljava/lang/String;");
    jstring j_package_name = (jstring)env->CallObjectMethod(context, j_mid);
    //2、对比包名
    const char *c_package_name = env->GetStringUTFChars(j_package_name, NULL);
    if (strcmp(c_package_name, PACKAGE_NAME) != 0) {
        return;
    }
    __android_log_print(ANDROID_LOG_ERROR, "JNI_TAG", "包名一致：%s", c_package_name);
    //3.获取签名
    jmethodID j_getPackageManager = env->GetMethodID(j_clz, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    jobject j_package_manager = env->CallObjectMethod(context, j_getPackageManager);
    jclass  j_package_manager_clz = env->GetObjectClass(j_package_manager);

    jmethodID j_getPackageInfo = env->GetMethodID(j_package_manager_clz, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    jobject j_package_info = env->CallObjectMethod(j_package_manager, j_getPackageInfo, j_package_name, 0x00000040);
    jclass j_package_info_clz = env->GetObjectClass(j_package_info);
    jfieldID j_signature = env->GetFieldID(j_package_info_clz, "signatures", "[Landroid/content/pm/Signature;");
    jobjectArray signatures = (jobjectArray)env->GetObjectField(j_package_info, j_signature);
    jobject signature_first = env->GetObjectArrayElement(signatures, 0);
    jclass signature_clz = env->GetObjectClass(signature_first);
    jmethodID signature_toCharsString = env->GetMethodID(signature_clz, "toCharsString", "()Ljava/lang/String;");
    jstring signature_str = (jstring)env->CallObjectMethod(signature_first, signature_toCharsString);
    const char * c_signature_str = env->GetStringUTFChars(signature_str, NULL);
    //4.对比签名
    if (strcmp(c_signature_str, APP_SIGNATURE) != 0) {
        return;
    }
    __android_log_print(ANDROID_LOG_ERROR, "JNI_TAG", "签名一致：%s", c_signature_str);

    //签名认真成功
    is_verify = 1;
}