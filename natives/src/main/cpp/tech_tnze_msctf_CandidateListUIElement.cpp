#include <msctf.h>

#include "tech_tnze_msctf_CandidateListUIElement.h"
#include "util.h"

extern "C" JNIEXPORT jint JNICALL
Java_tech_tnze_msctf_CandidateListUIElement_getCount(JNIEnv *env, jobject thiz)
{
    UINT result;
    ITfCandidateListUIElement *elem = reinterpret_cast<ITfCandidateListUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = elem->GetCount(&result);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "GetCount", ret));
        return 0;
    }
    return static_cast<jint>(result);
}

extern "C" JNIEXPORT jint JNICALL
Java_tech_tnze_msctf_CandidateListUIElement_getCurrentPage(JNIEnv *env, jobject thiz)
{
    UINT result;
    ITfCandidateListUIElement *elem = reinterpret_cast<ITfCandidateListUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    HRESULT ret = elem->GetCurrentPage(&result);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "GetCurrentPage", ret));
        return 0;
    }
    return static_cast<jint>(result);
}

extern "C" JNIEXPORT jint JNICALL
Java_tech_tnze_msctf_CandidateListUIElement_getPageIndex(JNIEnv *env, jobject thiz, jintArray indexes)
{
    HRESULT ret;
    ITfCandidateListUIElement *elem = reinterpret_cast<ITfCandidateListUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    UINT count;

    if (indexes == nullptr)
    {
        ret = elem->GetPageIndex(NULL, 0, &count);
    }
    else
    {
        jint *array = env->GetIntArrayElements(indexes, nullptr);
        static_assert(sizeof(jint) == sizeof(UINT));
        ret = elem->GetPageIndex(reinterpret_cast<UINT *>(array), static_cast<UINT>(env->GetArrayLength(indexes)), &count);
        env->ReleaseIntArrayElements(indexes, array, 0);
    }

    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "GetPageIndex", ret));
        return 0;
    }
    return static_cast<jint>(count);
}

extern "C" JNIEXPORT jstring JNICALL
Java_tech_tnze_msctf_CandidateListUIElement_getString(JNIEnv *env, jobject thiz, jint index)
{
    BSTR result;
    ITfCandidateListUIElement *elem = reinterpret_cast<ITfCandidateListUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")));
    UINT count;

    HRESULT ret = elem->GetString(static_cast<UINT>(index), &result);
    if (FAILED(ret))
    {
        env->Throw(HRESULT_TO_EXCEPTION(env, "GetString", ret));
        return 0;
    }
    static_assert(sizeof(wchar_t) == sizeof(jchar));
    return env->NewString(reinterpret_cast<const jchar *>(result), SysStringLen(result));
}

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_CandidateListUIElement_close(JNIEnv *env, jobject thiz)
{
    reinterpret_cast<ITfCandidateListUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")))->Release();
}
