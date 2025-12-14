#include <msctf.h>
#include <winrt/base.h>

#include "tech_tnze_msctf_DocumentManager.h"
#include "tech_tnze_msctf_ContextOwnerCompositionSink.h"
#include "util.h"

extern "C" JNIEXPORT jobject JNICALL
Java_tech_tnze_msctf_DocumentManager_createContext(JNIEnv *env, jobject thiz, jint clientId, jint flags, jobject sink)
{
    ITfContext *context;
    TfEditCookie editCookie;
    ITfDocumentMgr *docMgr = reinterpret_cast<ITfDocumentMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    JniContextOwnerCompositionSink *contextSink = new JniContextOwnerCompositionSink(env, sink);
    if (env->ExceptionCheck())
    {
        contextSink->Release();
        return nullptr;
    }

    HRESULT ret = docMgr->CreateContext(static_cast<TfClientId>(clientId), static_cast<DWORD>(flags), contextSink, &context, &editCookie);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "CreateContext", ret));
        return nullptr;
    }
    contextSink->Release();

    jclass contextClazz = env->FindClass("tech/tnze/msctf/Context");
    return env->NewObject(
        contextClazz,
        env->GetMethodID(contextClazz, "<init>", "(JI)V"),
        reinterpret_cast<jlong>(context),
        static_cast<jint>(editCookie));
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_DocumentManager_push(JNIEnv *env, jobject thiz, jobject ctx)
{
    ITfDocumentMgr *docMgr = reinterpret_cast<ITfDocumentMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    ITfContext *context = reinterpret_cast<ITfContext *>(env->GetLongField(ctx, env->GetFieldID(env->GetObjectClass(ctx), "pointer", "J")));
    HRESULT ret = docMgr->Push(context);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "Push", ret));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_DocumentManager_pop(JNIEnv *env, jobject thiz, jint flags)
{
    ITfDocumentMgr *docMgr = reinterpret_cast<ITfDocumentMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = docMgr->Pop(static_cast<DWORD>(flags));
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "Pop", ret));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_DocumentManager_close(JNIEnv *env, jobject thiz)
{
    ITfDocumentMgr *docMgr = reinterpret_cast<ITfDocumentMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    docMgr->Release();
}
