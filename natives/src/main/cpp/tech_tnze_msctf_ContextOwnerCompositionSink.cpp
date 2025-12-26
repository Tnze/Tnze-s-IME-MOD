#include "tech_tnze_msctf_ContextOwnerCompositionSink.h"

JniContextOwnerCompositionSink::JniContextOwnerCompositionSink(JNIEnv *env, jobject ref)
    : global_ref(env->NewGlobalRef(ref)),
      global_clz(static_cast<jclass>(env->NewGlobalRef(env->GetObjectClass(global_ref)))),
      compositionClazz(static_cast<jclass>(env->NewGlobalRef(env->FindClass("tech/tnze/msctf/CompositionView")))),
      rangeClazz(static_cast<jclass>(env->NewGlobalRef(env->FindClass("tech/tnze/msctf/Range"))))
{
    startId = env->GetMethodID(global_clz, "onStartComposition", "(Ltech/tnze/msctf/CompositionView;)Z");
    updateId = env->GetMethodID(global_clz, "onUpdateComposition", "(Ltech/tnze/msctf/CompositionView;Ltech/tnze/msctf/Range;)V");
    endId = env->GetMethodID(global_clz, "onEndComposition", "(Ltech/tnze/msctf/CompositionView;)V");

    compositionNew = env->GetMethodID(compositionClazz, "<init>", "(J)V");
    rangeNew = env->GetMethodID(rangeClazz, "<init>", "(J)V");

    jint ret = env->GetJavaVM(&jvm);
}

HRESULT STDMETHODCALLTYPE JniContextOwnerCompositionSink::OnStartComposition(
    /* [in] */ ITfCompositionView *pComposition,
    /* [out] */ BOOL *pfOk)
{
    JNIEnv *env;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);

    pComposition->AddRef();
    jobject composition = env->NewObject(compositionClazz, compositionNew, reinterpret_cast<jlong>(pComposition));
    jboolean ok = env->CallBooleanMethod(global_ref, startId, composition);
    *pfOk = static_cast<BOOL>(ok);
    *pfOk = TRUE;
    HRESULT ret = S_OK;
    if (env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        env->ExceptionClear();
        ret = E_FAIL;
    }

    jvm->DetachCurrentThread();
    return ret;
}

HRESULT STDMETHODCALLTYPE JniContextOwnerCompositionSink::OnUpdateComposition(
    /* [in] */ ITfCompositionView *pComposition,
    /* [in] */ ITfRange *pRangeNew)
{
    JNIEnv *env;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);

    pComposition->AddRef();
    jobject composition = env->NewObject(compositionClazz, compositionNew, reinterpret_cast<jlong>(pComposition));
    jobject range = pRangeNew ? env->NewObject(rangeClazz, rangeNew, reinterpret_cast<jlong>(pRangeNew)) : nullptr;
    env->CallVoidMethod(global_ref, updateId, composition, range);

    HRESULT ret = S_OK;
    if (env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        env->ExceptionClear();
        ret = E_FAIL;
    }

    jvm->DetachCurrentThread();
    return ret;
}

HRESULT STDMETHODCALLTYPE JniContextOwnerCompositionSink::OnEndComposition(
    /* [in] */ ITfCompositionView *pComposition)
{
    JNIEnv *env;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);

    pComposition->AddRef();
    jobject composition = env->NewObject(compositionClazz, compositionNew, reinterpret_cast<jlong>(pComposition));
    env->CallVoidMethod(global_ref, endId, composition);

    HRESULT ret = S_OK;
    if (env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        env->ExceptionClear();
        ret = E_FAIL;
    }

    jvm->DetachCurrentThread();
    return ret;
}

HRESULT STDMETHODCALLTYPE JniContextOwnerCompositionSink::QueryInterface(
    /* [in] */ REFIID riid,
    /* [iid_is][out] */ void **ppvObject)
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

ULONG STDMETHODCALLTYPE JniContextOwnerCompositionSink::AddRef(void)
{
    return InterlockedIncrement(&refCount);
}

ULONG STDMETHODCALLTYPE JniContextOwnerCompositionSink::Release(void)
{
    ULONG r = InterlockedDecrement(&refCount);
    if (r == 0)
        delete this;
    return r;
}

JniContextOwnerCompositionSink::~JniContextOwnerCompositionSink()
{
    JNIEnv *env;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);
    env->DeleteGlobalRef(global_ref);
    jvm->DetachCurrentThread();
}
