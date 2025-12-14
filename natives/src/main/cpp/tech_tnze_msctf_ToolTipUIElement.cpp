#include <msctf.h>

#include "tech_tnze_msctf_ToolTipUIElement.h"
#include "util.h"

extern "C" JNIEXPORT jstring JNICALL
Java_tech_tnze_msctf_ToolTipUIElement_getString(JNIEnv *env, jobject thiz)
{
    BSTR result;
    ITfToolTipUIElement *elem = reinterpret_cast<ITfToolTipUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    UINT count;

    HRESULT ret = elem->GetString(&result);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "GetString", ret));
        return 0;
    }
    static_assert(sizeof(wchar_t) == sizeof(jchar));
    return env->NewString(reinterpret_cast<const jchar *>(result), SysStringLen(result));
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_ToolTipUIElement_close(JNIEnv *env, jobject thiz)
{
    reinterpret_cast<ITfToolTipUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")))->Release();
}
