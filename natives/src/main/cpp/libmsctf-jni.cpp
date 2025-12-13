#include <jni.h>
#include <string>
#include <msctf.h>
#include <combaseapi.h>
#include <winrt/base.h>

#pragma comment(lib, "ole32.lib")

extern "C" static jthrowable HRESULT_TO_EXCEPTION(JNIEnv *env, const char *message_utf, HRESULT hr)
{
    jclass winExClazz = env->FindClass("tech/tnze/msctf/WindowsException");
    jobject winEx = env->NewObject(
        winExClazz,
        env->GetMethodID(winExClazz, "<init>", "(Ljava/lang/String;I)V"),
        env->NewStringUTF(message_utf),
        static_cast<jint>(hr));
    return static_cast<jthrowable>(winEx);
}

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

extern "C" JNIEXPORT jlong JNICALL
Java_tech_tnze_msctf_ThreadManager_createInstance(JNIEnv *env, jclass clazz)
{
    HRESULT ret;
    ITfThreadMgr *pTfThreadMgr;
    ret = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "CoInitializeEx", ret));
        return 0;
    }
    ret = CoCreateInstance(CLSID_TF_ThreadMgr, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&pTfThreadMgr));
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "CoCreateInstance", ret));
        return 0;
    }
    return reinterpret_cast<jlong>(pTfThreadMgr);
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_ThreadManager_releaseInstance(JNIEnv *env, jobject thiz, jlong p)
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
    jclass docManagerClazz = env->GetObjectClass(documentManager);
    ITfThreadMgr *threadMgr = reinterpret_cast<ITfThreadMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    ITfDocumentMgr *docMgr = reinterpret_cast<ITfDocumentMgr *>(env->GetLongField(documentManager, env->GetFieldID(docManagerClazz, "pointer", "J")));
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

class JniUiElementSink : public ITfUIElementSink
{
public:
    JniUiElementSink(JNIEnv *env, jobject ref)
        : refCount(1),
          env(env),
          global_ref(env->NewGlobalRef(ref))
    {
        jclass clazz = env->GetObjectClass(global_ref);
        beginId = env->GetMethodID(clazz, "begin", "(I)Z");
        updateId = env->GetMethodID(clazz, "update", "(I)V");
        endId = env->GetMethodID(clazz, "end", "(I)V");
    }

    HRESULT STDMETHODCALLTYPE BeginUIElement(
        /* [in] */ DWORD dwUIElementId,
        /* [out][in] */ BOOL *pbShow) override
    {
        jboolean ret = env->CallBooleanMethod(global_ref, beginId, static_cast<jint>(dwUIElementId));
        if (env->ExceptionCheck())
        {
            env->ExceptionClear();
            return E_FAIL;
        }
        *pbShow = static_cast<BOOL>(ret);
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE UpdateUIElement(
        /* [in] */ DWORD dwUIElementId) override
    {
        env->CallVoidMethod(global_ref, updateId, static_cast<jint>(dwUIElementId));
        if (env->ExceptionCheck())
        {
            env->ExceptionClear();
            return E_FAIL;
        }
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE EndUIElement(
        /* [in] */ DWORD dwUIElementId) override
    {
        env->CallVoidMethod(global_ref, endId, static_cast<jint>(dwUIElementId));
        if (env->ExceptionCheck())
        {
            env->ExceptionClear();
            return E_FAIL;
        }
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE QueryInterface(
        /* [in] */ REFIID riid,
        /* [iid_is][out] */ void **ppvObject) override
    {
        if (!ppvObject)
            return E_POINTER;
        if (riid == IID_IUnknown)
        {
            *ppvObject = static_cast<IUnknown *>(this);
            AddRef();
            return S_OK;
        }
        if (riid == IID_ITfUIElementSink)
        {
            *ppvObject = static_cast<ITfUIElementSink *>(this);
            AddRef();
            return S_OK;
        }
        *ppvObject = nullptr;
        return E_NOINTERFACE;
    }

    ULONG STDMETHODCALLTYPE AddRef(void) override
    {
        return InterlockedIncrement(&refCount);
    }

    ULONG STDMETHODCALLTYPE Release(void) override
    {
        ULONG r = InterlockedDecrement(&refCount);
        if (r == 0)
            delete this;
        return r;
    }

private:
    ULONG refCount;
    JNIEnv *env;
    jobject global_ref;
    jmethodID beginId, updateId, endId;

    ~JniUiElementSink()
    {
        env->DeleteGlobalRef(global_ref);
    }
};

// struct JniUiElementSink
//     : winrt::implements<JniUiElementSink, ITfUIElementSink>
// {
// private:
//     JNIEnv *env;
//     jobject global_ref;
//     jmethodID beginId, updateId, endId;

// public:
//     JniUiElementSink(JNIEnv *env, jobject ref)
//         : env(env),
//           global_ref(env->NewGlobalRef(ref))
//     {
//         jclass clazz = env->GetObjectClass(global_ref);
//         beginId = env->GetMethodID(clazz, "begin", "(I)Z");
//         updateId = env->GetMethodID(clazz, "update", "(I)V");
//         endId = env->GetMethodID(clazz, "end", "(I)V");
//     }

//     ~JniUiElementSink()
//     {
//         env->DeleteGlobalRef(global_ref);
//     }

//     HRESULT STDMETHODCALLTYPE BeginUIElement(
//         /* [in] */ DWORD dwUIElementId,
//         /* [out][in] */ BOOL *pbShow)
//     {
//         jboolean ret = env->CallBooleanMethod(global_ref, beginId, static_cast<jint>(dwUIElementId));
//         if (env->ExceptionCheck())
//         {
//             env->ExceptionClear();
//             return E_FAIL;
//         }
//         *pbShow = static_cast<BOOL>(ret);
//         return S_OK;
//     }

//     HRESULT STDMETHODCALLTYPE UpdateUIElement(
//         /* [in] */ DWORD dwUIElementId)
//     {
//         env->CallVoidMethod(global_ref, updateId, static_cast<jint>(dwUIElementId));
//         if (env->ExceptionCheck())
//         {
//             env->ExceptionClear();
//             return E_FAIL;
//         }
//         return S_OK;
//     }

//     HRESULT STDMETHODCALLTYPE EndUIElement(
//         /* [in] */ DWORD dwUIElementId)
//     {
//         env->CallVoidMethod(global_ref, endId, static_cast<jint>(dwUIElementId));
//         if (env->ExceptionCheck())
//         {
//             env->ExceptionClear();
//             return E_FAIL;
//         }
//         return S_OK;
//     }
// };

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_Source_releaseInstance(JNIEnv *env, jclass clazz, jlong p)
{
    ITfSource *source = reinterpret_cast<ITfSource *>(p);
    source->Release();
}

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

struct JniContextOwnerCompositionSink : public ITfContextOwnerCompositionSink
{

public:
    JniContextOwnerCompositionSink(JNIEnv *env, jobject ref)
        : env(env),
          global_ref(env->NewGlobalRef(ref))
    {
        jclass clazz = env->GetObjectClass(global_ref);
        startId = env->GetMethodID(clazz, "onStartComposition", "(Ltech/tnze/msctf/CompositionView;)Z");
        updateId = env->GetMethodID(clazz, "onUpdateComposition", "(Ltech/tnze/msctf/CompositionView;Ltech/tnze/msctf/Range;)V");
        endId = env->GetMethodID(clazz, "onEndComposition", "(Ltech/tnze/msctf/CompositionView;)V");

        compositionClazz = env->FindClass("tech/tnze/msctf/CompositionView");
        compositionNew = env->GetMethodID(compositionClazz, "<init>", "(J)V");
    }

    HRESULT STDMETHODCALLTYPE OnStartComposition(
        /* [in] */ __RPC__in_opt ITfCompositionView *pComposition,
        /* [out] */ __RPC__out BOOL *pfOk)
    {
        pComposition->AddRef();
        jobject composition = env->NewObject(compositionClazz, compositionNew, reinterpret_cast<jlong>(pComposition));
        jboolean ok = env->CallBooleanMethod(global_ref, startId, composition);
        *pfOk = static_cast<BOOL>(ok);
        if (env->ExceptionCheck())
        {
            env->ExceptionClear();
            return E_FAIL;
        }
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE OnUpdateComposition(
        /* [in] */ __RPC__in_opt ITfCompositionView *pComposition,
        /* [in] */ __RPC__in_opt ITfRange *pRangeNew)
    {
        pComposition->AddRef();
        jobject composition = env->NewObject(compositionClazz, compositionNew, reinterpret_cast<jlong>(pComposition));
        env->CallVoidMethod(global_ref, updateId, composition);
        if (env->ExceptionCheck())
        {
            env->ExceptionClear();
            return E_FAIL;
        }
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE OnEndComposition(
        /* [in] */ __RPC__in_opt ITfCompositionView *pComposition)
    {
        pComposition->AddRef();
        jobject composition = env->NewObject(compositionClazz, compositionNew, reinterpret_cast<jlong>(pComposition));
        env->CallVoidMethod(global_ref, endId, composition);
        if (env->ExceptionCheck())
        {
            env->ExceptionClear();
            return E_FAIL;
        }
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE QueryInterface(
        /* [in] */ REFIID riid,
        /* [iid_is][out] */ void **ppvObject) override
    {
        if (!ppvObject)
            return E_POINTER;
        if (riid == IID_IUnknown)
        {
            *ppvObject = static_cast<IUnknown *>(this);
            AddRef();
            return S_OK;
        }
        if (riid == IID_ITfContextOwnerCompositionSink)
        {
            *ppvObject = static_cast<ITfContextOwnerCompositionSink *>(this);
            AddRef();
            return S_OK;
        }
        *ppvObject = nullptr;
        return E_NOINTERFACE;
    }

    ULONG STDMETHODCALLTYPE AddRef(void) override
    {
        return InterlockedIncrement(&refCount);
    }

    ULONG STDMETHODCALLTYPE Release(void) override
    {
        ULONG r = InterlockedDecrement(&refCount);
        if (r == 0)
            delete this;
        return r;
    }

private:
    ULONG refCount;
    JNIEnv *env;
    jobject global_ref;
    jclass compositionClazz;
    jmethodID compositionNew;
    jmethodID startId, updateId, endId;

    ~JniContextOwnerCompositionSink()
    {
        env->DeleteGlobalRef(global_ref);
    }
};

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_CompositionView_close(JNIEnv *env, jobject thiz)
{
    ITfCompositionView *v = reinterpret_cast<ITfCompositionView *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    v->Release();
}

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

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_Context_close(JNIEnv *env, jobject thiz)
{
    ITfContext *context = reinterpret_cast<ITfContext *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    context->Release();
}
