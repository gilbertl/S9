LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	com_gilbertl_s9_BinaryDictionary.cpp \
	dictionary.cpp

LOCAL_LDLIBS := -llog

LOCAL_MODULE := s9

include $(BUILD_SHARED_LIBRARY)
