#include <jni.h>
#include <msctf.h>
#include <winrt/base.h>

class JniUiElementSink : public ITfUIElementSink
{
public:
    JniUiElementSink(JNIEnv *env, jobject ref);

    HRESULT STDMETHODCALLTYPE BeginUIElement(
        /* [in] */ DWORD dwUIElementId,
        /* [out][in] */ BOOL *pbShow) override;

    HRESULT STDMETHODCALLTYPE UpdateUIElement(
        /* [in] */ DWORD dwUIElementId) override;

    HRESULT STDMETHODCALLTYPE EndUIElement(
        /* [in] */ DWORD dwUIElementId) override;

    HRESULT STDMETHODCALLTYPE QueryInterface(
        /* [in] */ REFIID riid,
        /* [iid_is][out] */ void **ppvObject) override;

    ULONG STDMETHODCALLTYPE AddRef(void) override;

    ULONG STDMETHODCALLTYPE Release(void) override;

private:
    ULONG refCount;
    JavaVM *jvm;
    jobject global_ref;
    jclass global_clz;
    jmethodID beginId, updateId, endId;

    ~JniUiElementSink();
};
