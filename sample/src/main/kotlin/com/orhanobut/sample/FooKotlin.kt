package com.orhanobut.sample

import com.orhanobut.tracklytics.FixedAttribute
import com.orhanobut.tracklytics.TrackEvent
import com.orhanobut.tracklytics.TransformAttribute

@FixedAttribute(key="screen_name", value = "FooKotlin")
open class FooKotlin {

  @TrackEvent("event_kotlin")
  open fun trackFoo(@TransformAttribute("fun_attribute") attribute: String?) {
  }
}