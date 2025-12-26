#include <jni.h>
#include <msctf.h>
#include <winrt/base.h>

class JniContextOwner : public ITfContextOwner
{
public:
    JniContextOwner(JNIEnv *env, jobject ref);

    HRESULT STDMETHODCALLTYPE GetACPFromPoint(
        /* [in] */ const POINT *ptScreen,
        /* [in] */ DWORD dwFlags,
        /* [out] */ LONG *pacp) override;

    HRESULT STDMETHODCALLTYPE GetTextExt(
        /* [in] */ LONG acpStart,
        /* [in] */ LONG acpEnd,
        /* [out] */ RECT *prc,
        /* [out] */ BOOL *pfClipped) override;

    HRESULT STDMETHODCALLTYPE GetScreenExt(
        /* [out] */ RECT *prc) override;

    HRESULT STDMETHODCALLTYPE GetStatus(
        /* [out] */ TF_STATUS *pdcs) override;

    HRESULT STDMETHODCALLTYPE GetWnd(
        /* [out] */ HWND *phwnd) override;

    HRESULT STDMETHODCALLTYPE GetAttribute(
        /* [in] */ REFGUID rguidAttribute,
        /* [out] */ VARIANT *pvarValue) override;

    HRESULT STDMETHODCALLTYPE QueryInterface(
        /* [in] */ REFIID riid,
        /* [iid_is][out] */ void **ppvObject) override;

    ULONG STDMETHODCALLTYPE AddRef(void) override;

    ULONG STDMETHODCALLTYPE Release(void) override;

private:
    ULONG refCount;
    JavaVM *jvm;
    jobject global_ref;
    jclass global_clz, global_InvalidPointException_clz, global_NoLayoutException_clz;
    jmethodID getACPFromPoint, getAttribute;

    ~JniContextOwner();
};