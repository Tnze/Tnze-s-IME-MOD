#include <msctf.h>
#include <iostream>

#include "tech_tnze_msctf_Range.h"
#include "util.h"

extern "C" JNIEXPORT jstring JNICALL 
Java_tech_tnze_msctf_Range_getText(JNIEnv *env, jobject thiz, jint editCookie)
{
    HRESULT ret;
    ITfRange *range = reinterpret_cast<ITfRange *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    WCHAR achBuffer[64];
    ULONG num;

    ret = range->GetText(static_cast<TfEditCookie>(editCookie), TF_TF_MOVESTART | TF_TF_IGNOREEND, achBuffer, ARRAYSIZE(achBuffer), &num);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "GetText", ret));
        return nullptr;
    }
    std::cout << "num: " << num << std::endl;

    static_assert(sizeof(WCHAR) == sizeof(jchar));
    return env->NewString(reinterpret_cast<const jchar *>(achBuffer), num);
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_Range_close(JNIEnv *env, jobject thiz)
{
    reinterpret_cast<ITfRange *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")))->Release();
}
