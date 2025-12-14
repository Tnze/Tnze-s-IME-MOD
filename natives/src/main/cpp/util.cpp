#include "util.h"

jthrowable HRESULT_TO_EXCEPTION(JNIEnv *env, const char *message_utf, HRESULT hr)
{
    jclass winExClazz = env->FindClass("tech/tnze/msctf/WindowsException");
    jobject winEx = env->NewObject(
        winExClazz,
        env->GetMethodID(winExClazz, "<init>", "(Ljava/lang/String;I)V"),
        env->NewStringUTF(message_utf),
        static_cast<jint>(hr));
    return static_cast<jthrowable>(winEx);
}
