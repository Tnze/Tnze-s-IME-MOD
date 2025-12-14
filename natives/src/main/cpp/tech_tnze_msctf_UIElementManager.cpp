#include <msctf.h>

#include "tech_tnze_msctf_UIElementManager.h"
#include "util.h"

extern "C" JNIEXPORT jobject JNICALL
Java_tech_tnze_msctf_UIElementManager_getUIElement(JNIEnv *env, jobject thiz, jint uiElementId)
{
    ITfUIElement *element;
    ITfUIElementMgr *uiElementMgr = reinterpret_cast<ITfUIElementMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = uiElementMgr->GetUIElement(static_cast<DWORD>(uiElementId), &element);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "GetUIElement", ret));
        return nullptr;
    }

    jclass uiElemClazz = env->FindClass("tech/tnze/msctf/UIElement");
    return env->NewObject(
        uiElemClazz,
        env->GetMethodID(uiElemClazz, "<init>", "(J)V"),
        reinterpret_cast<jlong>(element));
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_UIElementManager_close(JNIEnv *env, jobject thiz)
{
    reinterpret_cast<ITfUIElementMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")))->Release();
}
