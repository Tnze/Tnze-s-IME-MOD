#include "tech_tnze_msctf_ContextOwner.h"

JniContextOwner::JniContextOwner(JNIEnv *env, jobject ref)
    : global_ref(env->NewGlobalRef(ref)),
      global_clz(static_cast<jclass>(env->NewGlobalRef(env->GetObjectClass(global_ref)))),
      global_InvalidPointException_clz(static_cast<jclass>(env->NewGlobalRef(env->FindClass("tech/tnze/msctf/ContextOwner$InvalidPointException")))),
      global_NoLayoutException_clz(static_cast<jclass>(env->NewGlobalRef(env->FindClass("tech/tnze/msctf/ContextOwner$NoLayoutException"))))
{
    getACPFromPoint = env->GetMethodID(global_clz, "getACPFromPoint", "(JJI)J");
    jint ret = env->GetJavaVM(&jvm);
}

HRESULT STDMETHODCALLTYPE JniContextOwner::GetACPFromPoint(
    /* [in] */ const POINT *ptScreen,
    /* [in] */ DWORD dwFlags,
    /* [out] */ LONG *pacp)
{
    JNIEnv *env;
    HRESULT hr = S_OK;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);

    jlong ret = env->CallLongMethod(
        global_ref,
        getACPFromPoint,
        static_cast<jlong>(ptScreen->x),
        static_cast<jlong>(ptScreen->y),
        static_cast<jint>(dwFlags));

    if (env->ExceptionCheck())
    {
        env->ExceptionDescribe();
        jthrowable exception = env->ExceptionOccurred();
        if (env->IsInstanceOf(exception, global_InvalidPointException_clz))
        {
            hr = TS_E_INVALIDPOINT;
        }
        else if (env->IsInstanceOf(exception, global_NoLayoutException_clz))
        {
            hr = TS_E_NOLAYOUT;
        }
        else
        {
            hr = E_FAIL;
        }
        env->ExceptionClear();
        goto exit;
    }

    *pacp = static_cast<LONG>(ret);
exit:
    jvm->DetachCurrentThread();
    return hr;
}

HRESULT STDMETHODCALLTYPE JniContextOwner::GetTextExt(
    /* [in] */ LONG acpStart,
    /* [in] */ LONG acpEnd,
    /* [out] */ RECT *prc,
    /* [out] */ BOOL *pfClipped)
{
    JNIEnv *env;
    HRESULT hr;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);
    // env->CallObjectMethod(global_ref, getAttribute, )
exit:
    jvm->DetachCurrentThread();
    return hr;
}

HRESULT STDMETHODCALLTYPE JniContextOwner::GetScreenExt(
    /* [out] */ RECT *prc)
{
    HRESULT hr = S_OK;
    JNIEnv *env;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);
    prc->top = 100;
    prc->left = 100;
    prc->bottom = 150;
    prc->right = 300;
    jvm->DetachCurrentThread();
    return hr;
}

HRESULT STDMETHODCALLTYPE JniContextOwner::GetStatus(
    /* [out] */ TF_STATUS *pdcs)
{
    HRESULT hr = S_OK;
    JNIEnv *env;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);
    pdcs->dwStaticFlags = 0;
    pdcs->dwDynamicFlags = 0;
    jvm->DetachCurrentThread();
    return hr;
}

HRESULT STDMETHODCALLTYPE JniContextOwner::GetWnd(
    /* [out] */ HWND *phwnd)
{
    HRESULT hr = S_OK;
    JNIEnv *env;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);
    jvm->DetachCurrentThread();
    return hr;
}

HRESULT STDMETHODCALLTYPE JniContextOwner::GetAttribute(
    /* [in] */ REFGUID rguidAttribute,
    /* [out] */ VARIANT *pvarValue)
{
    JNIEnv *env;
    HRESULT hr = S_OK;
    OLECHAR str[39];
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);
    int len = StringFromGUID2(rguidAttribute, str, _countof(str));
    jobject ret = env->CallObjectMethod(
        global_ref,
        getAttribute,
        env->NewString(reinterpret_cast<const jchar *>(str),
                       static_cast<jsize>(len) - 1));
exit:
    jvm->DetachCurrentThread();
    return hr;
}

HRESULT STDMETHODCALLTYPE JniContextOwner::QueryInterface(
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
    if (riid == IID_ITfContextOwner)
    {
        *ppvObject = static_cast<ITfContextOwner *>(this);
        AddRef();
        return S_OK;
    }
    *ppvObject = nullptr;
    return E_NOINTERFACE;
}

ULONG STDMETHODCALLTYPE JniContextOwner::AddRef(void)
{
    return InterlockedIncrement(&refCount);
}

ULONG STDMETHODCALLTYPE JniContextOwner::Release(void)
{
    ULONG r = InterlockedDecrement(&refCount);
    if (r == 0)
        delete this;
    return r;
}

JniContextOwner::~JniContextOwner()
{
    JNIEnv *env;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);
    env->DeleteGlobalRef(global_ref);
    jvm->DetachCurrentThread();
}