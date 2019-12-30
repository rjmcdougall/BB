LOCAL_PATH:= $(call /Users/rmc/src.local/bb/bb)
include $(CLEAR_VARS)
 
LOCAL_MODULE    := libbbhardwarejni
LOCAL_SRC_FILES := bbhardware-jni.c

LOCAL_PACKAGE_NAME := com.richardmcdougall.bb
 
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_LIBRARIES += liblog
include $(BUILD_SHARED_LIBRARY)
 
include $(BUILD_PACKAGE)

