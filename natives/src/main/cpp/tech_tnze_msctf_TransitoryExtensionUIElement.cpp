#include <msctf.h>

#include "tech_tnze_msctf_TransitoryExtensionUIElement.h"
#include "util.h"

extern "C" JNIEXPORT void JNICALL
Java_tech_tnze_msctf_TransitoryExtensionUIElement_close(JNIEnv *env, jobject thiz)
{
    reinterpret_cast<ITfToolTipUIElement *>(env->GetLongField(thiz, env->GetFieldID(env->GetObjectClass(thiz), "pointer", "J")))->Release();
}
