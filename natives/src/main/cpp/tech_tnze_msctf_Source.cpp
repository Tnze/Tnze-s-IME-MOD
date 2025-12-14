#include "tech_tnze_msctf_Source.h"
#include "tech_tnze_msctf_UIElementSink.h"
#include "util.h"

extern "C" JNIEXPORT jint JNICALL
Java_tech_tnze_msctf_Source_adviceUIElementSink(JNIEnv *env, jobject thiz, jobject uiElementSink)
{
    DWORD cookie;
    ITfSource *source = reinterpret_cast<ITfSource *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    JniUiElementSink *sink = new JniUiElementSink(env, uiElementSink);
    HRESULT ret = source->AdviseSink(IID_ITfUIElementSink, sink, &cookie);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "AdviseSink", ret));
        return 0;
    }
    sink->Release();
    return static_cast<jint>(cookie);
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_Source_unadviseSink(JNIEnv *env, jobject thiz, jint cookie)
{
    ITfSource *source = reinterpret_cast<ITfSource *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = source->UnadviseSink(static_cast<DWORD>(cookie));
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "UnadviseSink", ret));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_Source_releaseInstance(JNIEnv *env, jclass clazz, jlong p)
{
    ITfSource *source = reinterpret_cast<ITfSource *>(p);
    source->Release();
}
