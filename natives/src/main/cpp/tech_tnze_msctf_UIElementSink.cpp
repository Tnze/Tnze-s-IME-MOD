#include "tech_tnze_msctf_UIElementSink.h"

JniUiElementSink::JniUiElementSink(JNIEnv *env, jobject ref)
    : refCount(1),
      global_ref(env->NewGlobalRef(ref)),
      global_clz(static_cast<jclass>(env->NewGlobalRef(env->GetObjectClass(global_ref))))
{
    beginId = env->GetMethodID(global_clz, "begin", "(I)Z");
    updateId = env->GetMethodID(global_clz, "update", "(I)V");
    endId = env->GetMethodID(global_clz, "end", "(I)V");

    env->GetJavaVM(&jvm);
}

HRESULT STDMETHODCALLTYPE JniUiElementSink::BeginUIElement(
    /* [in] */ DWORD dwUIElementId,
    /* [out][in] */ BOOL *pbShow)
{
    JNIEnv *env;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);

    jboolean show = env->CallBooleanMethod(global_ref, beginId, static_cast<jint>(dwUIElementId));
    HRESULT ret = S_OK;
    if (env->ExceptionCheck())
    {
        env->ExceptionClear();
        ret = E_FAIL;
    }
    else
    {
        *pbShow = static_cast<BOOL>(show);
    }

    jvm->DetachCurrentThread();
    return ret;
}

HRESULT STDMETHODCALLTYPE JniUiElementSink::UpdateUIElement(
    /* [in] */ DWORD dwUIElementId)
{
    JNIEnv *env;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);

    env->CallVoidMethod(global_ref, updateId, static_cast<jint>(dwUIElementId));
    HRESULT ret = S_OK;
    if (env->ExceptionCheck())
    {
        env->ExceptionClear();
        ret = E_FAIL;
    }

    jvm->DetachCurrentThread();
    return ret;
}

HRESULT STDMETHODCALLTYPE JniUiElementSink::EndUIElement(
    /* [in] */ DWORD dwUIElementId)
{
    JNIEnv *env;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);
    env->CallVoidMethod(global_ref, endId, static_cast<jint>(dwUIElementId));
    HRESULT ret = S_OK;
    if (env->ExceptionCheck())
    {
        env->ExceptionClear();
        ret = E_FAIL;
    }

    jvm->DetachCurrentThread();
    return ret;
}

HRESULT STDMETHODCALLTYPE JniUiElementSink::QueryInterface(
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
    if (riid == IID_ITfUIElementSink)
    {
        *ppvObject = static_cast<ITfUIElementSink *>(this);
        AddRef();
        return S_OK;
    }
    *ppvObject = nullptr;
    return E_NOINTERFACE;
}

ULONG STDMETHODCALLTYPE JniUiElementSink::AddRef(void)
{
    return InterlockedIncrement(&refCount);
}

ULONG STDMETHODCALLTYPE JniUiElementSink::Release(void)
{
    ULONG r = InterlockedDecrement(&refCount);
    if (r == 0)
        delete this;
    return r;
}

JniUiElementSink::~JniUiElementSink()
{
    JNIEnv *env;
    jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), NULL);
    env->DeleteGlobalRef(global_clz);
    env->DeleteGlobalRef(global_ref);
    jvm->DetachCurrentThread();
}
