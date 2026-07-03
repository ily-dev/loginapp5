LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := main_$(PREFERRED_ABI)

# Add your application source files here...
LOCAL_SRC_FILES := start.c

LOCAL_CFLAGS += -I$(PYTHON_INCLUDE_ROOT) $(EXTRA_CFLAGS)

# 🔥 WICHTIG: Python direkt verknüpfen
LOCAL_LDLIBS := -llog -lm -lpython3.11 $(EXTRA_LDLIBS)

# 🔥 Zusätzlich: Python-Library-Pfad angeben
LOCAL_LDFLAGS += -L$(PYTHON_LINK_ROOT) $(APPLICATION_ADDITIONAL_LDFLAGS)

# 🔥 Gemeinsame Bibliotheken (optional)
LOCAL_SHARED_LIBRARIES := python_shared

include $(BUILD_SHARED_LIBRARY)