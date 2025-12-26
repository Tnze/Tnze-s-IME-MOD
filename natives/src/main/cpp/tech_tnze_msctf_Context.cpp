#include <msctf.h>

#include "tech_tnze_msctf_Context.h"
#include "util.h"


extern "C" JNIEXPORT jobject JNICALL
Java_tech_tnze_msctf_Context_getSource(JNIEnv *env, jobject thiz)
{
    HRESULT ret;
    ITfSource *source;
    ITfContext *threadMgr = reinterpret_cast<ITfContext *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));

    ret = threadMgr->QueryInterface(IID_PPV_ARGS(&source));
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "QueryInterface(ITfSource)", ret));
        return NULL;
    }
    jclass sourceClazz = env->FindClass("tech/tnze/msctf/Source");
    return env->NewObject(
        sourceClazz,
        env->GetMethodID(sourceClazz, "<init>", "(J)V"),
        reinterpret_cast<jlong>(source));
}