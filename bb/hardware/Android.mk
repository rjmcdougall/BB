# Copyright 2007 The Android Open Source Project
#
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := com.richardmcdougall.bb
LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_JNI_SHARED_LIBRARIES := libbbhardwarejni 
LOCAL_SDK_VERSION := current
include $(BUILD_JAVA_LIBRARY)
