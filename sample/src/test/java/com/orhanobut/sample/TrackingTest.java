package com.orhanobut.sample;

import com.orhanobut.tracklytics.Event;
import com.orhanobut.tracklytics.EventSubscriber;
import com.orhanobut.tracklytics.Tracklytics;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

// This is not an example of how to test
// The only purpose of this code is to see if the test byte code has the aspects
public class TrackingTest {

  private final Map<String, Event> triggeredEvents = new HashMap<>();

  @Before public void setup() {
    Tracklytics.init(new EventSubscriber() {
      @Override public void onEventTracked(Event event) {
        triggeredEvents.put(event.name, event);
      }
    });
  }

  @Test public void confirmKotlinAspects() {
    new FooKotlin().trackFoo("any_value");

    assertThat(triggeredEvents).containsKey("event_kotlin");
    Event event = triggeredEvents.get("event_kotlin");
    Map<String, Object> attributes = event.attributes;
    assertThat(attributes).containsKey("screen_name");
    assertThat(attributes).containsKey("fun_attribute");
    assertThat(attributes.get("fun_attribute")).isEqualTo("any_value");
  }
  @Test public void confirmJavaObjectAspects() {
    new JavaObject().track();

    assertThat(triggeredEvents).containsKey("event_java_object");
    Event event = triggeredEvents.get("event_java_object");
    assertThat(event.attributes).containsKey("screen_name");
  }

  @Test public void confirmJavaAspects() {
    new KotlinObject().track();

    assertThat(triggeredEvents).containsKey("event_kotlin_object");
  }
}