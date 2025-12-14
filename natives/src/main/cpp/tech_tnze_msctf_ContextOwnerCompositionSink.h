#include <jni.h>
#include <msctf.h>
#include <winrt/base.h>

class JniContextOwnerCompositionSink : public ITfContextOwnerCompositionSink
{
public:
    JniContextOwnerCompositionSink(JNIEnv *env, jobject ref);

    HRESULT STDMETHODCALLTYPE OnStartComposition(
        /* [in] */ ITfCompositionView *pComposition,
        /* [out] */ BOOL *pfOk) override;

    HRESULT STDMETHODCALLTYPE OnUpdateComposition(
        /* [in] */ ITfCompositionView *pComposition,
        /* [in] */ ITfRange *pRangeNew) override;

    HRESULT STDMETHODCALLTYPE OnEndComposition(
        /* [in] */ ITfCompositionView *pComposition) override;

    HRESULT STDMETHODCALLTYPE QueryInterface(
        /* [in] */ REFIID riid,
        /* [iid_is][out] */ void **ppvObject) override;

    ULONG STDMETHODCALLTYPE AddRef(void) override;

    ULONG STDMETHODCALLTYPE Release(void) override;

private:
    ULONG refCount;
    JavaVM *jvm;
    jobject global_ref;
    jclass global_clz, compositionClazz, rangeClazz;
    jmethodID compositionNew, rangeNew;
    jmethodID startId, updateId, endId;

    ~JniContextOwnerCompositionSink();
};