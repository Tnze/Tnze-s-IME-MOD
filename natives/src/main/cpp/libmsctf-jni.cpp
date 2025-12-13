#include <jni.h>
#include <string>
#include <msctf.h>
#include <combaseapi.h>
#include <iostream>

#pragma comment(lib, "ole32.lib")

extern "C" static jthrowable HRESULT_TO_EXCEPTION(JNIEnv *env, HRESULT hr)
{
    LPWSTR buffer;
    DWORD len = FormatMessageW(
        FORMAT_MESSAGE_ALLOCATE_BUFFER |
            FORMAT_MESSAGE_FROM_SYSTEM |
            FORMAT_MESSAGE_IGNORE_INSERTS,
        nullptr,
        hr,
        MAKELANGID(LANG_ENGLISH, SUBLANG_ENGLISH_US),
        reinterpret_cast<LPWSTR>(&buffer),
        0,
        nullptr);
    static_assert(sizeof(wchar_t) == sizeof(jchar)); // this cast maybe only works on Windows
    jstring str = env->NewString(reinterpret_cast<jchar *>(buffer), len);
    LocalFree(buffer);

    jclass runtimeExceptionClazz = env->FindClass("java/lang/RuntimeException");
    return (jthrowable)env->NewObject(
        runtimeExceptionClazz,
        env->GetMethodID(runtimeExceptionClazz, "<init>", "(Ljava/lang/String;)V"),
        str);
}

extern "C" JNIEXPORT jlong JNICALL
Java_tech_tnze_msctf_ThreadManager_createInstance(JNIEnv *env, jclass clazz)
{
    HRESULT ret;
    ITfThreadMgr *pTfThreadMgr;
    ret = CoInitializeEx(NULL, COINIT_MULTITHREADED);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, ret));
        return 0;
    }
    ret = CoCreateInstance(CLSID_TF_ThreadMgr, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&pTfThreadMgr));
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, ret));
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
        env->Throw(HRESULT_TO_EXCEPTION(env, ret));
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
        env->Throw(HRESULT_TO_EXCEPTION(env, ret));
        return 0;
    }

    ret = threadMgrEx->ActivateEx(&clientId, static_cast<DWORD>(flags));
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, ret));
        goto exit;
    }

exit:
    threadMgrEx->Release();
    return static_cast<jint>(clientId);
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_ThreadManager_deactivate(JNIEnv *env, jobject thiz)
{
    ITfThreadMgr *threadMgr = reinterpret_cast<ITfThreadMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = threadMgr->Deactivate();
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, ret));
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_tech_tnze_msctf_ThreadManager_getSource(JNIEnv *env, jobject thiz)
{
    ITfSource *source;
    ITfThreadMgr *threadMgr = reinterpret_cast<ITfThreadMgr *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = threadMgr->QueryInterface(IID_PPV_ARGS(&source));
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, ret));
        return NULL;
    }
    jclass sourceClazz = env->FindClass("tech/tnze/msctf/Source");
    return env->NewObject(
        sourceClazz,
        env->GetMethodID(sourceClazz, "<init>", "(J)V"),
        reinterpret_cast<jlong>(source));
}

class JniUiElementSink : public ITfUIElementSink
{
private:
    ULONG refCount;
    JNIEnv *env;
    jobject global_ref;
    jmethodID beginId, updateId, endId;

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

    ~JniUiElementSink()
    {
        env->DeleteGlobalRef(global_ref);
    }

    HRESULT STDMETHODCALLTYPE BeginUIElement(
        /* [in] */ DWORD dwUIElementId,
        /* [out][in] */ BOOL *pbShow)
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
        /* [in] */ DWORD dwUIElementId)
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
        /* [in] */ DWORD dwUIElementId)
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
        /* [iid_is][out] */ _COM_Outptr_ void __RPC_FAR * __RPC_FAR * ppvObject)
    {
        if (!ppvObject)
            return E_POINTER;

        if (riid == IID_IUnknown)
        {
            *ppvObject = static_cast<IUnknown *>(this);
            AddRef();
            return S_OK;
        }
        else if (riid == IID_ITfUIElementSink)
        {
            *ppvObject = static_cast<ITfUIElementSink *>(this);
            AddRef();
            return S_OK;
        }
        else
        {
            *ppvObject = nullptr;
            return E_NOINTERFACE;
        }
    }

    ULONG STDMETHODCALLTYPE AddRef(void)
    {
        return InterlockedIncrement(&refCount);
    }

    ULONG STDMETHODCALLTYPE Release(void)
    {
        ULONG r = InterlockedDecrement(&refCount);
        if (r == 0)
            delete this;
        return r;
    }
};

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
        env->Throw(HRESULT_TO_EXCEPTION(env, ret));
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
        env->Throw(HRESULT_TO_EXCEPTION(env, ret));
    }
}