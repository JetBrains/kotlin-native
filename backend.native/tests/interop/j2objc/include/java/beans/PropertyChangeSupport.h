//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: android/platform/libcore/ojluni/src/main/java/java/beans/PropertyChangeSupport.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_JavaBeansPropertyChangeSupport")
#ifdef RESTRICT_JavaBeansPropertyChangeSupport
#define INCLUDE_ALL_JavaBeansPropertyChangeSupport 0
#else
#define INCLUDE_ALL_JavaBeansPropertyChangeSupport 1
#endif
#undef RESTRICT_JavaBeansPropertyChangeSupport

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (JavaBeansPropertyChangeSupport_) && (INCLUDE_ALL_JavaBeansPropertyChangeSupport || defined(INCLUDE_JavaBeansPropertyChangeSupport))
#define JavaBeansPropertyChangeSupport_

#define RESTRICT_JavaIoSerializable 1
#define INCLUDE_JavaIoSerializable 1
#include "java/io/Serializable.h"

@class IOSObjectArray;
@class JavaBeansPropertyChangeEvent;
@protocol JavaBeansPropertyChangeListener;

/*!
 @brief This is a utility class that can be used by beans that support bound
  properties.It manages a list of listeners and dispatches 
 <code>PropertyChangeEvent</code>s to them.
 You can use an instance of this class
  as a member field of your bean and delegate these types of work to it.
  The <code>PropertyChangeListener</code> can be registered for all properties
  or for a property specified by name. 
 <p>
  Here is an example of <code>PropertyChangeSupport</code> usage that follows
  the rules and recommendations laid out in the JavaBeans&trade; specification: 
 @code

  public class MyBean {
      private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
      public void addPropertyChangeListener(PropertyChangeListener listener) {
          this.pcs.addPropertyChangeListener(listener);
      }
      public void removePropertyChangeListener(PropertyChangeListener listener) {
          this.pcs.removePropertyChangeListener(listener);
      }
      private String value;
      public String getValue() {
          return this.value;
      }
      public void setValue(String newValue) {
          String oldValue = this.value;
          this.value = newValue;
          this.pcs.firePropertyChange("value", oldValue, newValue);
      }
      [...]
  } 
  
@endcode
  <p>
  A <code>PropertyChangeSupport</code> instance is thread-safe. 
 <p>
  This class is serializable.  When it is serialized it will save
  (and restore) any listeners that are themselves serializable.  Any
  non-serializable listeners will be skipped during serialization.
 - seealso: VetoableChangeSupport
 */
@interface JavaBeansPropertyChangeSupport : NSObject < JavaIoSerializable >
@property (readonly, class) jlong serialVersionUID NS_SWIFT_NAME(serialVersionUID);

+ (jlong)serialVersionUID;

#pragma mark Public

/*!
 @brief Constructs a <code>PropertyChangeSupport</code> object.
 @param sourceBean The bean to be given as the source for any events.
 */
- (instancetype __nonnull)initWithId:(id)sourceBean;

/*!
 @brief Add a PropertyChangeListener to the listener list.
 The listener is registered for all properties.
  The same listener object may be added more than once, and will be called
  as many times as it is added.
  If <code>listener</code> is null, no exception is thrown and no action
  is taken.
 @param listener The PropertyChangeListener to be added
 */
- (void)addPropertyChangeListenerWithJavaBeansPropertyChangeListener:(id<JavaBeansPropertyChangeListener>)listener;

/*!
 @brief Add a PropertyChangeListener for a specific property.The listener
  will be invoked only when a call on firePropertyChange names that
  specific property.
 The same listener object may be added more than once.  For each
  property,  the listener will be invoked the number of times it was added
  for that property.
  If <code>propertyName</code> or <code>listener</code> is null, no
  exception is thrown and no action is taken.
 @param propertyName The name of the property to listen on.
 @param listener The PropertyChangeListener to be added
 */
- (void)addPropertyChangeListenerWithNSString:(NSString *)propertyName
          withJavaBeansPropertyChangeListener:(id<JavaBeansPropertyChangeListener>)listener;

/*!
 @brief Reports a boolean bound indexed property update to listeners
  that have been registered to track updates of
  all properties or a property with the specified name.
 <p>
  No event is fired if old and new values are equal. 
 <p>
  This is merely a convenience wrapper around the more general 
 <code>fireIndexedPropertyChange(String, int, Object, Object)</code> method.
 @param propertyName the programmatic name of the property that was changed
 @param index the index of the property element that was changed
 @param oldValue the old value of the property
 @param newValue the new value of the property
 @since 1.5
 */
- (void)fireIndexedPropertyChangeWithNSString:(NSString *)propertyName
                                      withInt:(jint)index
                                  withBoolean:(jboolean)oldValue
                                  withBoolean:(jboolean)newValue;

/*!
 @brief Reports an integer bound indexed property update to listeners
  that have been registered to track updates of
  all properties or a property with the specified name.
 <p>
  No event is fired if old and new values are equal. 
 <p>
  This is merely a convenience wrapper around the more general 
 <code>fireIndexedPropertyChange(String, int, Object, Object)</code> method.
 @param propertyName the programmatic name of the property that was changed
 @param index the index of the property element that was changed
 @param oldValue the old value of the property
 @param newValue the new value of the property
 @since 1.5
 */
- (void)fireIndexedPropertyChangeWithNSString:(NSString *)propertyName
                                      withInt:(jint)index
                                      withInt:(jint)oldValue
                                      withInt:(jint)newValue;

/*!
 @brief Reports a bound indexed property update to listeners
  that have been registered to track updates of
  all properties or a property with the specified name.
 <p>
  No event is fired if old and new values are equal and non-null. 
 <p>
  This is merely a convenience wrapper around the more general 
 <code>firePropertyChange(PropertyChangeEvent)</code> method.
 @param propertyName the programmatic name of the property that was changed
 @param index the index of the property element that was changed
 @param oldValue the old value of the property
 @param newValue the new value of the property
 @since 1.5
 */
- (void)fireIndexedPropertyChangeWithNSString:(NSString *)propertyName
                                      withInt:(jint)index
                                       withId:(id)oldValue
                                       withId:(id)newValue;

/*!
 @brief Fires a property change event to listeners
  that have been registered to track updates of
  all properties or a property with the specified name.
 <p>
  No event is fired if the given event's old and new values are equal and non-null.
 @param event the <code>PropertyChangeEvent</code>  to be fired
 */
- (void)firePropertyChangeWithJavaBeansPropertyChangeEvent:(JavaBeansPropertyChangeEvent *)event;

/*!
 @brief Reports a boolean bound property update to listeners
  that have been registered to track updates of
  all properties or a property with the specified name.
 <p>
  No event is fired if old and new values are equal. 
 <p>
  This is merely a convenience wrapper around the more general 
 <code>firePropertyChange(String, Object, Object)</code>  method.
 @param propertyName the programmatic name of the property that was changed
 @param oldValue the old value of the property
 @param newValue the new value of the property
 */
- (void)firePropertyChangeWithNSString:(NSString *)propertyName
                           withBoolean:(jboolean)oldValue
                           withBoolean:(jboolean)newValue;

/*!
 @brief Reports an integer bound property update to listeners
  that have been registered to track updates of
  all properties or a property with the specified name.
 <p>
  No event is fired if old and new values are equal. 
 <p>
  This is merely a convenience wrapper around the more general 
 <code>firePropertyChange(String, Object, Object)</code>  method.
 @param propertyName the programmatic name of the property that was changed
 @param oldValue the old value of the property
 @param newValue the new value of the property
 */
- (void)firePropertyChangeWithNSString:(NSString *)propertyName
                               withInt:(jint)oldValue
                               withInt:(jint)newValue;

/*!
 @brief Reports a bound property update to listeners
  that have been registered to track updates of
  all properties or a property with the specified name.
 <p>
  No event is fired if old and new values are equal and non-null. 
 <p>
  This is merely a convenience wrapper around the more general 
 <code>firePropertyChange(PropertyChangeEvent)</code> method.
 @param propertyName the programmatic name of the property that was changed
 @param oldValue the old value of the property
 @param newValue the new value of the property
 */
- (void)firePropertyChangeWithNSString:(NSString *)propertyName
                                withId:(id)oldValue
                                withId:(id)newValue;

/*!
 @brief Returns an array of all the listeners that were added to the
  PropertyChangeSupport object with addPropertyChangeListener().
 <p>
  If some listeners have been added with a named property, then
  the returned array will be a mixture of PropertyChangeListeners
  and <code>PropertyChangeListenerProxy</code>s. If the calling
  method is interested in distinguishing the listeners then it must
  test each element to see if it's a 
 <code>PropertyChangeListenerProxy</code>, perform the cast, and examine
  the parameter. 
 @code
 PropertyChangeListener[] listeners = bean.getPropertyChangeListeners();
  for (int i = 0; i < listeners.length; i++) {
    if (listeners[i] instanceof PropertyChangeListenerProxy) {
      PropertyChangeListenerProxy proxy =
                     (PropertyChangeListenerProxy)listeners[i];
      if (proxy.getPropertyName().equals("foo")) {
        // proxy is a PropertyChangeListener which was associated
        // with the property named "foo"
      }    }    }    
 
@endcode
 - seealso: PropertyChangeListenerProxy
 @return all of the <code>PropertyChangeListeners</code> added or an
          empty array if no listeners have been added
 @since 1.4
 */
- (IOSObjectArray *)getPropertyChangeListeners;

/*!
 @brief Returns an array of all the listeners which have been associated
  with the named property.
 @param propertyName The name of the property being listened to
 @return all of the <code>PropertyChangeListeners</code> associated with
          the named property.  If no such listeners have been added,
          or if <code>propertyName</code> is null, an empty array is
          returned.
 @since 1.4
 */
- (IOSObjectArray *)getPropertyChangeListenersWithNSString:(NSString *)propertyName;

/*!
 @brief Check if there are any listeners for a specific property, including
  those registered on all properties.If <code>propertyName</code>
  is null, only check for listeners registered on all properties.
 @param propertyName the property name.
 @return true if there are one or more listeners for the given property
 */
- (jboolean)hasListenersWithNSString:(NSString *)propertyName;

/*!
 @brief Remove a PropertyChangeListener from the listener list.
 This removes a PropertyChangeListener that was registered
  for all properties.
  If <code>listener</code> was added more than once to the same event
  source, it will be notified one less time after being removed.
  If <code>listener</code> is null, or was never added, no exception is
  thrown and no action is taken.
 @param listener The PropertyChangeListener to be removed
 */
- (void)removePropertyChangeListenerWithJavaBeansPropertyChangeListener:(id<JavaBeansPropertyChangeListener>)listener;

/*!
 @brief Remove a PropertyChangeListener for a specific property.
 If <code>listener</code> was added more than once to the same event
  source for the specified property, it will be notified one less time
  after being removed.
  If <code>propertyName</code> is null,  no exception is thrown and no
  action is taken.
  If <code>listener</code> is null, or was never added for the specified
  property, no exception is thrown and no action is taken.
 @param propertyName The name of the property that was listened on.
 @param listener The PropertyChangeListener to be removed
 */
- (void)removePropertyChangeListenerWithNSString:(NSString *)propertyName
             withJavaBeansPropertyChangeListener:(id<JavaBeansPropertyChangeListener>)listener;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_STATIC_INIT(JavaBeansPropertyChangeSupport)

/*!
 @brief Serialization version ID, so we're compatible with JDK 1.1
 */
inline jlong JavaBeansPropertyChangeSupport_get_serialVersionUID(void);
#define JavaBeansPropertyChangeSupport_serialVersionUID 6401253773779951803LL
J2OBJC_STATIC_FIELD_CONSTANT(JavaBeansPropertyChangeSupport, serialVersionUID, jlong)

FOUNDATION_EXPORT void JavaBeansPropertyChangeSupport_initWithId_(JavaBeansPropertyChangeSupport *self, id sourceBean);

FOUNDATION_EXPORT JavaBeansPropertyChangeSupport *new_JavaBeansPropertyChangeSupport_initWithId_(id sourceBean) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaBeansPropertyChangeSupport *create_JavaBeansPropertyChangeSupport_initWithId_(id sourceBean);

J2OBJC_TYPE_LITERAL_HEADER(JavaBeansPropertyChangeSupport)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_JavaBeansPropertyChangeSupport")
