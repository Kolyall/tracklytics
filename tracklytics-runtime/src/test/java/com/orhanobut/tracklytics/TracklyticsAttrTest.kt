package com.orhanobut.tracklytics

import com.google.common.truth.Truth
import junit.framework.Assert
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.lang.reflect.Method
import java.util.HashMap

class TracklyticsAttrTest {
    @Mock
    lateinit var joinPoint: ProceedingJoinPoint

    @Mock
    lateinit var methodSignature: MethodSignature

    private val superAttributes: MutableMap<String?, Any?> = HashMap()
    lateinit var aspect: TracklyticsAspect
    private var trackEvent: TrackEvent? = null
    private var attributes: Map<String, Any>? = null
    private var aspectListener: AspectListener? = null

    @Before
    @Throws(Exception::class)
    fun setup() {
        MockitoAnnotations.initMocks(this)
        aspectListener = object : AspectListener {
            override fun onAspectEventTriggered(trackEvent: TrackEvent, attributes: Map<String, Any>) {
                this@TracklyticsAttrTest.trackEvent = trackEvent
                this@TracklyticsAttrTest.attributes = attributes
            }

            override fun onAspectSuperAttributeAdded(key: String, value: Any) {
                superAttributes[key] = value
            }

            override fun onAspectSuperAttributeRemoved(key: String) {
                superAttributes.remove(key)
            }
        }
        aspect = TracklyticsAspect()
        TracklyticsAspect.subscribe(aspectListener)
        Mockito.`when`(joinPoint.signature).thenReturn(methodSignature)
    }

    @Throws(Throwable::class)
    private fun invokeMethod(klass: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method {
        val method = initMethod(klass, methodName, *parameterTypes)
        val instance = Any()
        Mockito.`when`(joinPoint.getThis()).thenReturn(instance)
        aspect.weaveJoinPointTrackEvent(joinPoint)
        return method
    }

    @Throws(Throwable::class)
    private fun initMethod(klass: Class<*>, name: String, vararg parameterTypes: Class<*>): Method {
        val method = klass.getMethod(name, *parameterTypes)
        Mockito.`when`(methodSignature.method).thenReturn(method)
        return method
    }

    @Test
    @Throws(Throwable::class)
    fun trackEventWithoutAttributes() {
        class Foo {
            @TrackEvent("title")
            fun foo() {
            }
        }
        invokeMethod(Foo::class.java, "foo")
        val argument = ArgumentCaptor.forClass(
            MutableMap::class.java
        )
        assertTrack()
            .event("title")
            .noFilters()
            .noTags()
            .noAttributes()
    }

    @Test
    @Throws(Throwable::class)
    fun useReturnValueAsAttribute() {
        class Foo {
            @TrackEvent("title")
            @Attribute("key")
            fun foo(): String {
                return "test"
            }
        }
        Mockito.`when`(joinPoint.proceed()).thenReturn("test")
        invokeMethod(Foo::class.java, "foo")
        assertTrack()
            .event("title")
            .noTags()
            .noFilters()
            .attribute("key", "test")
    }

    @Test
    @Throws(Throwable::class)
    fun useReturnValueAndParametersAsAttributes() {
        class Foo {
            @TrackEvent("title")
            @Attribute("key1")
            fun foo(@Attribute("key2") param: String?): String {
                return "test"
            }
        }
        Mockito.`when`(joinPoint.proceed()).thenReturn("test")
        Mockito.`when`(joinPoint.args).thenReturn(arrayOf<Any>("param"))
        invokeMethod(Foo::class.java, "foo", String::class.java)
        assertTrack()
            .event("title")
            .noFilters()
            .noTags()
            .attribute("key1", "test")
            .attribute("key2", "param")
    }

    @Test
    @Throws(Throwable::class)
    fun useDefaultValueWhenThereIsNoReturnValue() {
        class Foo {
            @TrackEvent("title")
            @Attribute(value = "key1", defaultValue = "defaultValue")
            fun foo() {
            }
        }
        invokeMethod(Foo::class.java, "foo")
        assertTrack()
            .event("title")
            .noFilters()
            .noTags()
            .attribute("key1", "defaultValue")
    }

    @Test
    @Throws(Throwable::class)
    fun useReturnValueWhenItIsNotNull() {
        class Foo {
            @TrackEvent("title")
            @Attribute(value = "key1", defaultValue = "defaulValue")
            fun foo(): String {
                return "returnValue"
            }
        }
        Mockito.`when`(joinPoint.proceed()).thenReturn("returnValue")
        invokeMethod(Foo::class.java, "foo")
        assertTrack()
            .event("title")
            .noFilters()
            .noTags()
            .attribute("key1", "returnValue")
    }

    @Test
    @Throws(Throwable::class)
    fun useDefaultValueWhenParameterValueIsNull() {
        class Foo {
            @TrackEvent("title")
            fun foo(@Attribute(value = "key1", defaultValue = "default") `val`: String?) {
            }
        }
        Mockito.`when`(joinPoint.args).thenReturn(arrayOf(null))
        invokeMethod(Foo::class.java, "foo", String::class.java)
        assertTrack()
            .event("title")
            .noFilters()
            .noTags()
            .attribute("key1", "default")
    }

    @Test
    @Throws(Throwable::class)
    fun fixedAttributeOnMethodScope() {
        class Foo {
            @TrackEvent("title")
            @FixedAttribute(key = "key1", value = "value")
            fun foo(): String {
                return "returnValue"
            }
        }
        invokeMethod(Foo::class.java, "foo")
        assertTrack()
            .event("title")
            .noFilters()
            .noTags()
            .attribute("key1", "value")
    }

    @Test
    @Throws(Throwable::class)
    fun fixedAttributeOnClassScope() {
        @FixedAttributes(FixedAttribute(key = "key1", value = "value1"), FixedAttribute(key = "key2", value = "value2"))
        @FixedAttribute(key = "key3", value = "value3")
        class Foo {
            @TrackEvent("title")
            @FixedAttribute(key = "key4", value = "value4")
            fun foo() {
            }
        }
        invokeMethod(Foo::class.java, "foo")
        assertTrack()
            .event("title")
            .noFilters()
            .noTags()
            .attribute("key1", "value1")
            .attribute("key2", "value2")
            .attribute("key3", "value3")
            .attribute("key4", "value4")
    }

    @Test
    @Throws(Throwable::class)
    fun fixedAttributeAndAttributeAtSameTime() {
        class Foo {
            @TrackEvent("title")
            @Attribute("key1")
            @FixedAttribute(key = "key2", value = "value2")
            fun foo(): String {
                return "value1"
            }
        }
        Mockito.`when`(joinPoint.proceed()).thenReturn("value1")
        invokeMethod(Foo::class.java, "foo")
        assertTrack()
            .event("title")
            .noFilters()
            .noTags()
            .attribute("key1", "value1")
            .attribute("key2", "value2")
    }

    @Test
    @Throws(Throwable::class)
    fun fixedAttributes() {
        class Foo {
            @TrackEvent("title")
            @FixedAttributes(FixedAttribute(key = "key1", value = "value1"), FixedAttribute(key = "key2", value = "value2"))
            @FixedAttribute(key = "key3", value = "value3")
            fun foo() {
            }
        }
        invokeMethod(Foo::class.java, "foo")
        assertTrack()
            .event("title")
            .noFilters()
            .noTags()
            .attribute("key1", "value1")
            .attribute("key2", "value2")
            .attribute("key3", "value3")
    }

    @Test
    @Throws(Throwable::class)
    fun superAttribute() {
        class Foo {
            @TrackEvent("title")
            @Attribute(value = "key1", isSuper = true)
            fun foo(@Attribute(value = "key2", isSuper = true) value: String?): String {
                return "value1"
            }
        }
        Mockito.`when`(joinPoint.proceed()).thenReturn("value1")
        Mockito.`when`(joinPoint.args).thenReturn(arrayOf<Any>("value2"))
        invokeMethod(Foo::class.java, "foo", String::class.java)
        Truth.assertThat(superAttributes).containsExactly("key1", "value1", "key2", "value2")
    }

    @Test
    @Throws(Throwable::class)
    fun superFixedAttribute() {
        class Foo {
            @TrackEvent("title")
            @FixedAttributes(FixedAttribute(key = "key1", value = "value1"), FixedAttribute(key = "key2", value = "value2", isSuper = true))
            @FixedAttribute(key = "key3", value = "value3", isSuper = true)
            fun foo(): String {
                return "returnValue"
            }
        }
        Mockito.`when`(joinPoint.proceed()).thenReturn("value1")
        invokeMethod(Foo::class.java, "foo")
        Truth.assertThat(superAttributes).containsExactly("key2", "value2", "key3", "value3")
    }

    @Test
    @Throws(Throwable::class)
    fun superTransformAttribute() {
        class Foo {
            @TrackEvent("event")
            @TransformAttributeMap(keys = [0, 1], values = ["value1", "value2"])
            @TransformAttribute(value = "key1", isSuper = true)
            fun foo(@TransformAttribute(value = "key2", isSuper = true) value: Int): Int {
                return 0
            }
        }
        Mockito.`when`(joinPoint.proceed()).thenReturn(0)
        Mockito.`when`(joinPoint.args).thenReturn(arrayOf<Any>(1))
        invokeMethod(Foo::class.java, "foo", Int::class.java)
        Truth.assertThat(superAttributes).containsExactly("key1", "value1", "key2", "value2")
    }

    @Test
    @Throws(Throwable::class)
    fun trackable() {
        class Bar : Trackable {
            override fun getTrackableAttributes(): Map<String, Any> {
                val values: MutableMap<String, Any> = HashMap()
                values["key1"] = "value1"
                values["key2"] = "value2"
                return values
            }
        }

        class Foo {
            @TrackEvent("title")
            fun foo(@TrackableAttribute bar: Bar?) {
            }
        }
        Mockito.`when`(joinPoint.args).thenReturn(arrayOf<Any>(Bar()))
        invokeMethod(Foo::class.java, "foo", Bar::class.java)
        assertTrack()
            .event("title")
            .noFilters()
            .noTags()
            .attribute("key1", "value1")
            .attribute("key2", "value2")
    }

    @Test
    @Throws(Throwable::class)
    fun ignoreNullValuesOnTrackable() {
        class Bar : Trackable {
            override fun getTrackableAttributes(): Map<String, Any>? {
                return null
            }
        }

        class Foo {
            @TrackEvent("title")
            fun foo(@TrackableAttribute bar: Bar?) {
            }
        }
        Mockito.`when`(joinPoint.args).thenReturn(arrayOf<Any>(Bar()))
        invokeMethod(Foo::class.java, "foo", Bar::class.java)
        assertTrack()
            .event("title")
            .noFilters()
            .noTags()
            .noAttributes()
    }

    @Test
    @Throws(Throwable::class)
    fun throwExceptionWhenTrackableAnnotationNotMatchWithValue() {
        class Foo {
            @TrackEvent("title")
            fun foo(@TrackableAttribute bar: String?) {
            }
        }
        Mockito.`when`(joinPoint.args).thenReturn(arrayOf<Any>("sdfsd"))
        try {
            invokeMethod(Foo::class.java, "foo", String::class.java)
            Assert.fail("Should throw exception")
        } catch (e: Exception) {
            Truth.assertThat(e).hasMessage("Trackable interface must be implemented for the parameter type")
        }
    }

    @Test
    @Throws(Throwable::class)
    fun methodParameterWithoutAnnotation() {
        class Foo {
            @TrackEvent("title")
            fun foo(@Attribute("Key") bar: String?, param2: String?) {
            }
        }
        Mockito.`when`(joinPoint.args).thenReturn(arrayOf<Any>("sdfsd"))
        invokeMethod(Foo::class.java, "foo", String::class.java, String::class.java)
        try {
            aspect.weaveJoinPointTrackEvent(joinPoint)
        } catch (e: Exception) {
            Assert.fail("Method parameters without annotation should be accepted")
        }
    }

    @Test
    @Throws(Throwable::class)
    fun classWideAttributeInAnonymousClass() {
        @FixedAttribute(key = "key1", value = "value1")
        class Foo {
            @FixedAttribute(key = "key2", value = "value2")
            inner class Inner {
                @TrackEvent("title")
                fun bar() {
                }
            }
        }
        invokeMethod(Foo.Inner::class.java, "bar")
        assertTrack()
            .event("title")
            .noFilters()
            .noTags()
            .attribute("key1", "value1")
            .attribute("key2", "value2")
    }

    @Test
    @Throws(Throwable::class)
    fun transformAttributeForParameters() {
        class Foo {
            @TrackEvent("event")
            @TransformAttributeMap(keys = [0, 1], values = ["value1", "value2"])
            fun foo(@TransformAttribute("key1") type: Int) {
            }
        }
        Mockito.`when`(joinPoint.args).thenReturn(arrayOf<Any>(0))
        invokeMethod(Foo::class.java, "foo", Int::class.java)
        assertTrack()
            .event("event")
            .noFilters()
            .noTags()
            .attribute("key1", "value1")
    }

    @Test
    @Throws(Throwable::class)
    fun transformAttributeMapInvalidState() {
        class Foo {
            @TrackEvent("event")
            @TransformAttributeMap(keys = [0, 1], values = ["value1"])
            fun foo(@TransformAttribute("key1") type: Int) {
            }
        }
        Mockito.`when`(joinPoint.args).thenReturn(arrayOf<Any>(0))
        try {
            invokeMethod(Foo::class.java, "foo", Int::class.java)
        } catch (e: Exception) {
            Truth.assertThat(e).hasMessage("TransformAttributeMap keys and values must have same length")
        }
    }

    @Test
    @Throws(Throwable::class)
    fun transformAttributeWithoutTransformAttributeMap() {
        class Foo {
            @TrackEvent("event")
            fun foo(@TransformAttribute("key1") type: Int) {
            }
        }
        Mockito.`when`(joinPoint.args).thenReturn(arrayOf<Any>(0))
        try {
            invokeMethod(Foo::class.java, "foo", Int::class.java)
        } catch (e: Exception) {
            Truth.assertThat(e).hasMessage("Method must have TransformAttributeMap when TransformAttribute is used")
        }
    }

    @Test
    @Throws(Throwable::class)
    fun transformAttributeForReturnValue() {
        class Foo {
            @TrackEvent("event")
            @TransformAttributeMap(keys = [0, 1], values = ["value1", "value2"])
            @TransformAttribute("key1")
            fun foo(): Int {
                return 1
            }
        }
        Mockito.`when`(joinPoint.proceed()).thenReturn(1)
        invokeMethod(Foo::class.java, "foo")
        assertTrack()
            .event("event")
            .noFilters()
            .noTags()
            .attribute("key1", "value2")
    }

    @Test
    @Throws(Throwable::class)
    fun transformAttributeDefaultValue() {
        class Foo {
            @TrackEvent("event")
            @TransformAttributeMap(keys = [0, 1], values = ["value1", "value2"])
            @TransformAttribute(value = "key1", defaultValue = "default1")
            fun foo(@TransformAttribute(value = "key2", defaultValue = "default2") `val`: Int): String? {
                return null
            }
        }
        Mockito.`when`(joinPoint.args).thenReturn(arrayOf(null))
        invokeMethod(Foo::class.java, "foo", Int::class.java)
        assertTrack()
            .event("event")
            .noFilters()
            .noTags()
            .attribute("key1", "default1")
            .attribute("key2", "default2")
    }

    @Test
    @Throws(Throwable::class)
    fun trackableAttributeForCurrentClass() {
        class Foo : Trackable {
            override fun getTrackableAttributes(): Map<String, Any> {
                val map: MutableMap<String, Any> = HashMap()
                map["key"] = "value"
                return map
            }

            @TrackEvent("event")
            @TrackableAttribute
            fun foo() {
            }
        }
        initMethod(Foo::class.java, "foo")
        Mockito.`when`(joinPoint.getThis()).thenReturn(Foo())
        aspect.weaveJoinPointTrackEvent(joinPoint)
        assertTrack()
            .event("event")
            .noFilters()
            .noTags()
            .attribute("key", "value")
    }

    @Test
    @Throws(Throwable::class)
    fun doNotUseTrackableAttributesWhenTrackableAttributeNotExists() {
        class Foo : Trackable {
            override fun getTrackableAttributes(): Map<String, Any> {
                val map: MutableMap<String, Any> = HashMap()
                map["key"] = "value"
                return map
            }

            @TrackEvent("event")
            fun foo() {
            }
        }
        Mockito.`when`(joinPoint.getThis()).thenReturn(Foo())
        invokeMethod(Foo::class.java, "foo")
        assertTrack()
            .event("event")
            .noFilters()
            .noTags()
            .noAttributes()
    }

    @Test
    @Throws(Throwable::class)
    fun ignoreNullValueOnTrackableAttributeForCurrentClass() {
        class Foo : Trackable {
            override fun getTrackableAttributes(): Map<String, Any>? {
                return null
            }

            @TrackEvent("event")
            @TrackableAttribute
            fun foo() {
            }
        }
        initMethod(Foo::class.java, "foo")
        Mockito.`when`(joinPoint.getThis()).thenReturn(Foo())
        aspect.weaveJoinPointTrackEvent(joinPoint)
        assertTrack()
            .event("event")
            .noFilters()
            .noTags()
            .noAttributes()
    }

    @Test
    @Throws(Throwable::class)
    fun overrideClassWideAttributeOnMethodWhenAttributesAreSame() {
        @FixedAttribute(key = "key", value = "class")
        @FixedAttributes(FixedAttribute(key = "key1", value = "class1"))
        class Foo {
            @TrackEvent("event")
            @FixedAttribute(key = "key", value = "method")
            @FixedAttributes(FixedAttribute(key = "key1", value = "method1"))
            fun foo() {
            }
        }
        invokeMethod(Foo::class.java, "foo")
        assertTrack()
            .event("event")
            .noFilters()
            .noTags()
            .attribute("key", "method")
            .attribute("key1", "method1")
    }

    @Test
    @Throws(Throwable::class)
    fun useThisClassWhenCalledFromSuperClass() {
        @FixedAttribute(key = "key0", value = "value0")
        open class Base {
            @TrackEvent("event")
            fun base() {
            }
        }

        @FixedAttribute(key = "key", value = "value")
        @FixedAttributes(FixedAttribute(key = "key2", value = "value2"))
        class Foo : Base()
        initMethod(Foo::class.java, "base")
        Mockito.`when`(joinPoint.getThis()).thenReturn(Foo())
        aspect.weaveJoinPointTrackEvent(joinPoint)
        assertTrack()
            .event("event")
            .noFilters()
            .noTags()
            .attribute("key0", "value0")
            .attribute("key", "value")
            .attribute("key2", "value2")
    }

    @Test
    @Throws(Throwable::class)
    fun filters() {
        class Foo {
            @TrackEvent(value = "event", filters = [100, 200])
            fun foo() {
            }
        }
        invokeMethod(Foo::class.java, "foo")
        val tags = intArrayOf(100, 200)
        assertTrack()
            .event("event")
            .noTags()
            .filters(100, 200)
            .noAttributes()
    }

    @Test
    @Throws(Throwable::class)
    fun tags() {
        class Foo {
            @TrackEvent(value = "event", tags = ["abc", "123"])
            fun foo() {
            }
        }
        invokeMethod(Foo::class.java, "foo")
        val tags = intArrayOf(100, 200)
        assertTrack()
            .event("event")
            .noFilters()
            .tags("abc", "123")
            .noAttributes()
    }

    private fun assertTrack(): AssertTracker {
        return AssertTracker(trackEvent, attributes)
    }
}