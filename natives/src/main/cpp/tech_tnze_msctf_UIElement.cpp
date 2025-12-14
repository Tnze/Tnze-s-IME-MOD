#include <msctf.h>

#include "tech_tnze_msctf_UIElement.h"
#include "util.h"

extern "C" JNIEXPORT jstring JNICALL
Java_tech_tnze_msctf_UIElement_getDescription(JNIEnv *env, jobject thiz)
{
    BSTR desc;
    ITfUIElement *uiElement = reinterpret_cast<ITfUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = uiElement->GetDescription(&desc);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "GetDescription", ret));
        return nullptr;
    }

    static_assert(sizeof(wchar_t) == sizeof(jchar));
    return env->NewString(reinterpret_cast<const jchar *>(desc), SysStringLen(desc));
}

extern "C" JNIEXPORT jstring JNICALL
Java_tech_tnze_msctf_UIElement_getGUID(JNIEnv *env, jobject thiz)
{
    GUID guid;
    OLECHAR str[39];
    ITfUIElement *uiElement = reinterpret_cast<ITfUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = uiElement->GetGUID(&guid);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "GetGUID", ret));
        return nullptr;
    }

    int len = StringFromGUID2(guid, str, _countof(str));
    return env->NewString(reinterpret_cast<const jchar *>(str), static_cast<jsize>(len) - 1);
}

extern "C" JNIEXPORT jobject JNICALL
Java_tech_tnze_msctf_UIElement_intoCandidateListUIElement(JNIEnv *env, jobject thiz)
{
    ITfCandidateListUIElement *target;
    ITfUIElement *uiElement = reinterpret_cast<ITfUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = uiElement->QueryInterface(IID_PPV_ARGS(&target));
    if (FAILED(ret))
    {
        return nullptr;
    }

    jclass clz = env->FindClass("tech/tnze/msctf/CandidateListUIElement");
    return env->NewObject(
        clz,
        env->GetMethodID(clz, "<init>", "(J)V"),
        reinterpret_cast<jlong>(target));
}

extern "C" JNIEXPORT jobject JNICALL
Java_tech_tnze_msctf_UIElement_intoToolTipUIElement(JNIEnv *env, jobject thiz)
{
    ITfToolTipUIElement *target;
    ITfUIElement *uiElement = reinterpret_cast<ITfUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = uiElement->QueryInterface(IID_PPV_ARGS(&target));
    if (FAILED(ret))
    {
        return nullptr;
    }

    jclass clz = env->FindClass("tech/tnze/msctf/ToolTipUIElement");
    return env->NewObject(
        clz,
        env->GetMethodID(clz, "<init>", "(J)V"),
        reinterpret_cast<jlong>(target));
}

extern "C" JNIEXPORT jobject JNICALL
Java_tech_tnze_msctf_UIElement_intoTransitoryExtensionUIElement(JNIEnv *env, jobject thiz)
{
    ITfTransitoryExtensionUIElement *target;
    ITfUIElement *uiElement = reinterpret_cast<ITfUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = uiElement->QueryInterface(IID_PPV_ARGS(&target));
    if (FAILED(ret))
    {
        return nullptr;
    }

    jclass clz = env->FindClass("tech/tnze/msctf/TransitoryExtensionUIElement");
    return env->NewObject(
        clz,
        env->GetMethodID(clz, "<init>", "(J)V"),
        reinterpret_cast<jlong>(target));
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_UIElement_close(JNIEnv *env, jobject thiz)
{
    reinterpret_cast<ITfUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")))->Release();
}
