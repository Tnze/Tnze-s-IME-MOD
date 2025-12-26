#include <msctf.h>

#include "tech_tnze_msctf_CompositionView.h"
#include "util.h"

extern "C" JNIEXPORT jstring JNICALL
Java_tech_tnze_msctf_CompositionView_getOwnerClsid(JNIEnv *env, jobject thiz)
{
    CLSID clsid;
    OLECHAR str[39];
    ITfCompositionView *cv = reinterpret_cast<ITfCompositionView *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = cv->GetOwnerClsid(&clsid);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "GetOwnerClsid", ret));
        return nullptr;
    }

    int len = StringFromGUID2(clsid, str, _countof(str));
    return env->NewString(reinterpret_cast<const jchar *>(str), static_cast<jsize>(len) - 1);
}

extern "C" JNIEXPORT jobject JNICALL
Java_tech_tnze_msctf_CompositionView_getRange(JNIEnv *env, jobject thiz)
{
    ITfRange *range;
    ITfCompositionView *cv = reinterpret_cast<ITfCompositionView *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = cv->GetRange(&range);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "GetOwnerClsid", ret));
        return nullptr;
    }

    jclass rangeClazz = env->FindClass("tech/tnze/msctf/Range");
    jmethodID rangeNew = env->GetMethodID(rangeClazz, "<init>", "(J)V");
    return env->NewObject(rangeClazz, rangeNew, range);
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_CompositionView_close(JNIEnv *env, jobject thiz)
{
    reinterpret_cast<ITfCompositionView *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")))->Release();
}