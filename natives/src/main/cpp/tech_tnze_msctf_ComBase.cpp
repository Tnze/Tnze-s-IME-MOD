#include <jni.h>
#include <combaseapi.h>

#include "tech_tnze_msctf_ComBase.h"
#include "util.h"

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_ComBase_CoInitialize(JNIEnv *env, jclass)
{
    HRESULT ret = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "CoInitializeEx", ret));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_ComBase_CoUninitialize(JNIEnv *env, jclass)
{
    CoUninitialize();
}
