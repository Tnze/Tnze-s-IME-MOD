#include <msctf.h>
#include <winrt/base.h>

#include "tech_tnze_msctf_ThreadManager.h"
#include "util.h"

extern "C" JNIEXPORT jlong JNICALL
Java_tech_tnze_msctf_ThreadManager_createInstance(JNIEnv *env, jclass clazz)
{
    ITfThreadMgr *pTfThreadMgr;
    HRESULT ret = CoCreateInstance(CLSID_TF_ThreadMgr, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&pTfThreadMgr));
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "CoCreateInstance", ret));
        return 0;
    }
    return reinterpret_cast<jlong>(pTfThreadMgr);
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_ThreadManager_releaseInstance(JNIEnv *env, jclass thiz, jlong p)
{
    reinterpret_cast<ITfThreadMgr *>(p)->Release();
}

extern "C" JNIEXPORT jint JNICALL
Java_tech_tnze_msctf_ThreadManager_activate(JNIEnv *env, jobject thiz)
{
    TfClientId clientId;
    ITfThreadMgr *threadMgr = reinterpret_cast<ITfThreadMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = threadMgr->Activate(&clientId);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "Activate", ret));
        return 0;
    }
    return static_cast<jint>(clientId);
}

extern "C" JNIEXPORT jint JNICALL
Java_tech_tnze_msctf_ThreadManager_activateEx(JNIEnv *env, jobject thiz, jint flags)
{
    HRESULT ret;
    TfClientId clientId;
    ITfThreadMgrEx *threadMgrEx;
    ITfThreadMgr *threadMgr = reinterpret_cast<ITfThreadMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    ret = threadMgr->QueryInterface(IID_PPV_ARGS(&threadMgrEx));
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "QueryInterface(ITfThreadMgrEx)", ret));
        return 0;
    }

    ret = threadMgrEx->ActivateEx(&clientId, static_cast<DWORD>(flags));
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "ActivateEx", ret));
        goto exit;
    }

exit:
    threadMgrEx->Release();
    return static_cast<jint>(clientId);
}

extern "C" JNIEXPORT jobject JNICALL
Java_tech_tnze_msctf_ThreadManager_associateFocus(JNIEnv *env, jobject thiz, jlong winHandle, jobject documentManager)
{
    jclass docManagerClazz = env->FindClass("tech/tnze/msctf/DocumentManager");
    ITfThreadMgr *threadMgr = reinterpret_cast<ITfThreadMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    ITfDocumentMgr *docMgr = nullptr;
    if (documentManager != nullptr)
    {
        docMgr = reinterpret_cast<ITfDocumentMgr *>(env->GetLongField(documentManager, env->GetFieldID(docManagerClazz, "pointer", "J")));
    }
    ITfDocumentMgr *privDocMgr;
    HRESULT ret = threadMgr->AssociateFocus(reinterpret_cast<HWND>(winHandle), docMgr, &privDocMgr);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "AssociateFocus", ret));
        return nullptr;
    }

    if (privDocMgr == NULL)
    {
        return nullptr;
    }

    return env->NewObject(
        docManagerClazz,
        env->GetMethodID(docManagerClazz, "<init>", "(J)V"),
        reinterpret_cast<jlong>(privDocMgr));
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_ThreadManager_deactivate(JNIEnv *env, jobject thiz)
{
    ITfThreadMgr *threadMgr = reinterpret_cast<ITfThreadMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = threadMgr->Deactivate();
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "Deactivate", ret));
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_tech_tnze_msctf_ThreadManager_getSource(JNIEnv *env, jobject thiz)
{
    HRESULT ret;
    ITfSource *source;
    ITfThreadMgr *threadMgr = reinterpret_cast<ITfThreadMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));

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

extern "C" JNIEXPORT jobject JNICALL
Java_tech_tnze_msctf_ThreadManager_createDocumentManager(JNIEnv *env, jobject thiz)
{
    ITfDocumentMgr *docMgr;
    ITfThreadMgr *threadMgr = reinterpret_cast<ITfThreadMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = threadMgr->CreateDocumentMgr(&docMgr);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "CreateDocumentMgr", ret));
        return nullptr;
    }

    jclass docMgrClazz = env->FindClass("tech/tnze/msctf/DocumentManager");
    return env->NewObject(
        docMgrClazz,
        env->GetMethodID(docMgrClazz, "<init>", "(J)V"),
        reinterpret_cast<jlong>(docMgr));
}

extern "C" JNIEXPORT jobject JNICALL
Java_tech_tnze_msctf_ThreadManager_getUIElementManager(JNIEnv *env, jobject thiz)
{
    HRESULT ret;
    ITfUIElementMgr *uiElemMgr;
    ITfThreadMgr *threadMgr = reinterpret_cast<ITfThreadMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));

    ret = threadMgr->QueryInterface(IID_PPV_ARGS(&uiElemMgr));
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "QueryInterface(ITfUIElementMgr)", ret));
        return NULL;
    }
    jclass sourceClazz = env->FindClass("tech/tnze/msctf/UIElementManager");
    return env->NewObject(
        sourceClazz,
        env->GetMethodID(sourceClazz, "<init>", "(J)V"),
        reinterpret_cast<jlong>(uiElemMgr));
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_ThreadManager_enumDocumentManagers(JNIEnv *env, jobject thiz, jobject consumer)
{
    winrt::com_ptr<IEnumTfDocumentMgrs> iter;
    ITfThreadMgr *threadMgr = reinterpret_cast<ITfThreadMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = threadMgr->EnumDocumentMgrs(iter.put());
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "EnumDocumentMgrs", ret));
        return;
    }

    jclass consumerClazz = env->GetObjectClass(consumer);
    jmethodID acceptMethod = env->GetMethodID(consumerClazz, "accept", "(Ljava/lang/Object;)V");
    jclass docMgrClazz = env->FindClass("tech/tnze/msctf/DocumentManager");
    jmethodID docMgrConstructor = env->GetMethodID(docMgrClazz, "<init>", "(J)V");

    ITfDocumentMgr *value;
    ULONG n;
    for (;;)
    {
        ret = iter->Next(1, &value, &n);
        if (FAILED(ret))
        {
            env->Throw(HRESULT_TO_EXCEPTION(env, "Next", ret));
            return;
        }

        if (n > 0)
        {
            jobject docMgr = env->NewObject(docMgrClazz, docMgrConstructor, reinterpret_cast<jlong>(value));
            env->CallVoidMethod(consumer, acceptMethod, docMgr);

            if (env->ExceptionCheck())
            {
                return;
            }
        }
        else
        {
            break;
        }
    }
}
