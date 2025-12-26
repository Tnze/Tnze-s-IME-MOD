#include <jni.h>
#include <string>
#include <msctf.h>
#include <combaseapi.h>
#include <winrt/base.h>

#include "tech_tnze_msctf_WindowsException.h"
#include "tech_tnze_msctf_CompositionView.h"
#include "tech_tnze_msctf_Context.h"
#include "util.h"

#pragma comment(lib, "ole32.lib")
#pragma comment(lib, "oleaut32.lib")

extern "C" JNIEXPORT jstring JNICALL
Java_tech_tnze_msctf_WindowsException_getOsMessage(JNIEnv *env, jobject thiz)
{
    LPWSTR buffer;
    jint hr = env->GetIntField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "hr", "I"));
    static_assert(sizeof(wchar_t) == sizeof(jchar)); // this cast maybe only works on Windows
    DWORD len = FormatMessageW(
        FORMAT_MESSAGE_ALLOCATE_BUFFER |
            FORMAT_MESSAGE_FROM_SYSTEM |
            FORMAT_MESSAGE_IGNORE_INSERTS,
        nullptr,
        static_cast<HRESULT>(hr),
        MAKELANGID(LANG_ENGLISH, SUBLANG_ENGLISH_US),
        reinterpret_cast<LPWSTR>(&buffer),
        0,
        nullptr);
    jstring msg = env->NewString(reinterpret_cast<jchar *>(buffer), len);
    LocalFree(buffer);
    return msg;
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_Context_close(JNIEnv *env, jobject thiz)
{
    ITfContext *context = reinterpret_cast<ITfContext *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    context->Release();
}
