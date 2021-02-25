package com.orhanobut.sample;

import com.orhanobut.tracklytics.FixedAttribute;
import com.orhanobut.tracklytics.TrackEvent;

@FixedAttribute(key="screen_name", value = "FooKotlin")
class JavaObject {
    @TrackEvent("event_java_object")
    void track() {
    }
}
