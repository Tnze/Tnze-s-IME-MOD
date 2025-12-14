#include <jni.h>
#include <winerror.h>

jthrowable HRESULT_TO_EXCEPTION(JNIEnv *env, const char *message_utf, HRESULT hr);
