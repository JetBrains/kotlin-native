//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/net/InetAddresses.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonNetInetAddresses")
#ifdef RESTRICT_ComGoogleCommonNetInetAddresses
#define INCLUDE_ALL_ComGoogleCommonNetInetAddresses 0
#else
#define INCLUDE_ALL_ComGoogleCommonNetInetAddresses 1
#endif
#undef RESTRICT_ComGoogleCommonNetInetAddresses

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonNetInetAddresses_) && (INCLUDE_ALL_ComGoogleCommonNetInetAddresses || defined(INCLUDE_ComGoogleCommonNetInetAddresses))
#define ComGoogleCommonNetInetAddresses_

@class ComGoogleCommonNetInetAddresses_TeredoInfo;
@class IOSByteArray;
@class JavaNetInet4Address;
@class JavaNetInet6Address;
@class JavaNetInetAddress;

/*!
 @brief Static utility methods pertaining to <code>InetAddress</code> instances.
 <p><b>Important note:</b> Unlike <code>InetAddress.getByName()</code>, the methods of this class never
  cause DNS services to be accessed. For this reason, you should prefer these methods as much as
  possible over their JDK equivalents whenever you are expecting to handle only IP address string
  literals -- there is no blocking DNS penalty for a malformed string. 
 <p>When dealing with <code>Inet4Address</code> and <code>Inet6Address</code> objects as byte arrays (vis. 
 <code>InetAddress.getAddress()</code>) they are 4 and 16 bytes in length, respectively, and represent
  the address in network byte order. 
 <p>Examples of IP addresses and their byte representations: 
 <dl>
    <dt>The IPv4 loopback address, <code>"127.0.0.1"</code>.
    <dd><code>7f 00 00 01</code>
    <dt>The IPv6 loopback address, <code>"::1"</code>.
    <dd><code>00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01</code>
    <dt>From the IPv6 reserved documentation prefix (<code>2001:db8::/32</code>), <code>"2001:db8::1"</code>.
    <dd><code>20 01 0d b8 00 00 00 00 00 00 00 00 00 00 00 01</code>
    <dt>An IPv6 "IPv4 compatible" (or "compat") address, <code>"::192.168.0.1"</code>.
    <dd><code>00 00 00 00 00 00 00 00 00 00 00 00 c0 a8 00 01</code>
    <dt>An IPv6 "IPv4 mapped" address, <code>"::ffff:192.168.0.1"</code>.
    <dd><code>00 00 00 00 00 00 00 00 00 00 ff ff c0 a8 00 01</code>
  </dl>
  
 <p>A few notes about IPv6 "IPv4 mapped" addresses and their observed use in Java. 
 <p>"IPv4 mapped" addresses were originally a representation of IPv4 addresses for use on an IPv6
  socket that could receive both IPv4 and IPv6 connections (by disabling the <code>IPV6_V6ONLY</code>
  socket option on an IPv6 socket). Yes, it's confusing. Nevertheless, these "mapped" addresses
  were never supposed to be seen on the wire. That assumption was dropped, some say mistakenly, in
  later RFCs with the apparent aim of making IPv4-to-IPv6 transition simpler. 
 <p>Technically one <i>can</i> create a 128bit IPv6 address with the wire format of a "mapped"
  address, as shown above, and transmit it in an IPv6 packet header. However, Java's InetAddress
  creation methods appear to adhere doggedly to the original intent of the "mapped" address: all
  "mapped" addresses return <code>Inet4Address</code> objects. 
 <p>For added safety, it is common for IPv6 network operators to filter all packets where either
  the source or destination address appears to be a "compat" or "mapped" address. Filtering
  suggestions usually recommend discarding any packets with source or destination addresses in the
  invalid range <code>::/3</code>, which includes both of these bizarre address formats. For more
  information on "bogons", including lists of IPv6 bogon space, see: 
 <ul>
    <li><a target="_parent" href="http://en.wikipedia.org/wiki/Bogon_filtering">
 http://en.wikipedia.
        org/wiki/Bogon_filtering</a>
    <li><a target="_parent" href="http://www.cymru.com/Bogons/ipv6.txt">
 http://www.cymru.com/Bogons/ ipv6.txt</a>
    <li><a target="_parent" href="http://www.cymru.com/Bogons/v6bogon.html">http://www.cymru.com/
        Bogons/v6bogon.html</a>
    <li><a target="_parent" href="http://www.space.net/~gert/RIPE/ipv6-filters.html">http://www.
        space.net/~gert/RIPE/ipv6-filters.html</a>
  </ul>
 @author Erik Kline
 @since 5.0
 */
@interface ComGoogleCommonNetInetAddresses : NSObject

#pragma mark Public

/*!
 @brief Returns an integer representing an IPv4 address regardless of whether the supplied argument is
  an IPv4 address or not.
 <p>IPv6 addresses are <b>coerced</b> to IPv4 addresses before being converted to integers. 
 <p>As long as there are applications that assume that all IP addresses are IPv4 addresses and
  can therefore be converted safely to integers (for whatever purpose) this function can be used
  to handle IPv6 addresses as well until the application is suitably fixed. 
 <p>NOTE: an IPv6 address coerced to an IPv4 address can only be used for such purposes as
  rudimentary identification or indexing into a collection of real <code>InetAddress</code>es. They
  cannot be used as real addresses for the purposes of network communication.
 @param ip<code>InetAddress</code>  to convert
 @return <code>int</code>, "coerced" if ip is not an IPv4 address
 @since 7.0
 */
+ (jint)coerceToIntegerWithJavaNetInetAddress:(JavaNetInetAddress * __nonnull)ip;

/*!
 @brief Returns a new InetAddress that is one less than the passed in address.This method works for
  both IPv4 and IPv6 addresses.
 @param address the InetAddress to decrement
 @return a new InetAddress that is one less than the passed in address
 @throw IllegalArgumentExceptionif InetAddress is at the beginning of its range
 @since 18.0
 */
+ (JavaNetInetAddress *)decrementWithJavaNetInetAddress:(JavaNetInetAddress * __nonnull)address;

/*!
 @brief Returns the <code>InetAddress</code> having the given string representation.
 <p>This deliberately avoids all nameservice lookups (e.g. no DNS).
 @param ipString<code>String</code>  containing an IPv4 or IPv6 string literal, e.g. <code>"192.168.0.1"</code>
   or <code>"2001:db8::1"</code>
 @return <code>InetAddress</code> representing the argument
 @throw IllegalArgumentExceptionif the argument is not a valid IP string literal
 */
+ (JavaNetInetAddress *)forStringWithNSString:(NSString * __nonnull)ipString;

/*!
 @brief Returns an InetAddress representing the literal IPv4 or IPv6 host portion of a URL, encoded in
  the format specified by RFC 3986 section 3.2.2.
 <p>This function is similar to <code>InetAddresses.forString(String)</code>, however, it requires
  that IPv6 addresses are surrounded by square brackets. 
 <p>This function is the inverse of <code>InetAddresses.toUriString(java.net.InetAddress)</code>.
 @param hostAddr A RFC 3986 section 3.2.2 encoded IPv4 or IPv6 address
 @return an InetAddress representing the address in <code>hostAddr</code>
 @throw IllegalArgumentExceptionif <code>hostAddr</code> is not a valid IPv4 address, or IPv6
      address surrounded by square brackets
 */
+ (JavaNetInetAddress *)forUriStringWithNSString:(NSString * __nonnull)hostAddr;

/*!
 @brief Returns an Inet4Address having the integer value specified by the argument.
 @param address<code>int</code> , the 32bit integer address to be converted
 @return <code>Inet4Address</code> equivalent of the argument
 */
+ (JavaNetInet4Address *)fromIntegerWithInt:(jint)address;

/*!
 @brief Returns an address from a <b>little-endian ordered</b> byte array (the opposite of what <code>InetAddress.getByAddress</code>
  expects).
 <p>IPv4 address byte array must be 4 bytes long and IPv6 byte array must be 16 bytes long.
 @param addr the raw IP address in little-endian byte order
 @return an InetAddress object created from the raw IP address
 @throw UnknownHostExceptionif IP address is of illegal length
 */
+ (JavaNetInetAddress *)fromLittleEndianByteArrayWithByteArray:(IOSByteArray * __nonnull)addr;

/*!
 @brief Returns the IPv4 address embedded in a 6to4 address.
 @param ip<code>Inet6Address</code>  to be examined for embedded IPv4 in 6to4 address
 @return <code>Inet4Address</code> of embedded IPv4 in 6to4 address
 @throw IllegalArgumentExceptionif the argument is not a valid IPv6 6to4 address
 */
+ (JavaNetInet4Address *)get6to4IPv4AddressWithJavaNetInet6Address:(JavaNetInet6Address * __nonnull)ip;

/*!
 @brief Coerces an IPv6 address into an IPv4 address.
 <p>HACK: As long as applications continue to use IPv4 addresses for indexing into tables,
  accounting, et cetera, it may be necessary to <b>coerce</b> IPv6 addresses into IPv4 addresses.
  This function does so by hashing the upper 64 bits into <code>224.0.0.0/3</code> (64 bits into 29
  bits). 
 <p>A "coerced" IPv4 address is equivalent to itself. 
 <p>NOTE: This function is failsafe for security purposes: ALL IPv6 addresses (except localhost
  (::1)) are hashed to avoid the security risk associated with extracting an embedded IPv4
  address that might permit elevated privileges.
 @param ip<code>InetAddress</code>  to "coerce"
 @return <code>Inet4Address</code> represented "coerced" address
 @since 7.0
 */
+ (JavaNetInet4Address *)getCoercedIPv4AddressWithJavaNetInetAddress:(JavaNetInetAddress * __nonnull)ip;

/*!
 @brief Returns the IPv4 address embedded in an IPv4 compatible address.
 @param ip<code>Inet6Address</code>  to be examined for an embedded IPv4 address
 @return <code>Inet4Address</code> of the embedded IPv4 address
 @throw IllegalArgumentExceptionif the argument is not a valid IPv4 compatible address
 */
+ (JavaNetInet4Address *)getCompatIPv4AddressWithJavaNetInet6Address:(JavaNetInet6Address * __nonnull)ip;

/*!
 @brief Examines the Inet6Address to extract the embedded IPv4 client address if the InetAddress is an
  IPv6 address of one of the specified address types that contain an embedded IPv4 address.
 <p>NOTE: ISATAP addresses are explicitly excluded from this method due to their trivial
  spoofability. With other transition addresses spoofing involves (at least) infection of one's
  BGP routing table.
 @param ip<code>Inet6Address</code>  to be examined for embedded IPv4 client address
 @return <code>Inet4Address</code> of embedded IPv4 client address
 @throw IllegalArgumentExceptionif the argument does not have a valid embedded IPv4 address
 */
+ (JavaNetInet4Address *)getEmbeddedIPv4ClientAddressWithJavaNetInet6Address:(JavaNetInet6Address * __nonnull)ip;

/*!
 @brief Returns the IPv4 address embedded in an ISATAP address.
 @param ip<code>Inet6Address</code>  to be examined for embedded IPv4 in ISATAP address
 @return <code>Inet4Address</code> of embedded IPv4 in an ISATAP address
 @throw IllegalArgumentExceptionif the argument is not a valid IPv6 ISATAP address
 */
+ (JavaNetInet4Address *)getIsatapIPv4AddressWithJavaNetInet6Address:(JavaNetInet6Address * __nonnull)ip;

/*!
 @brief Returns the Teredo information embedded in a Teredo address.
 @param ip<code>Inet6Address</code>  to be examined for embedded Teredo information
 @return extracted <code>TeredoInfo</code>
 @throw IllegalArgumentExceptionif the argument is not a valid IPv6 Teredo address
 */
+ (ComGoogleCommonNetInetAddresses_TeredoInfo *)getTeredoInfoWithJavaNetInet6Address:(JavaNetInet6Address * __nonnull)ip;

/*!
 @brief Examines the Inet6Address to determine if it is an IPv6 address of one of the specified address
  types that contain an embedded IPv4 address.
 <p>NOTE: ISATAP addresses are explicitly excluded from this method due to their trivial
  spoofability. With other transition addresses spoofing involves (at least) infection of one's
  BGP routing table.
 @param ip<code>Inet6Address</code>  to be examined for embedded IPv4 client address
 @return <code>true</code> if there is an embedded IPv4 client address
 @since 7.0
 */
+ (jboolean)hasEmbeddedIPv4ClientAddressWithJavaNetInet6Address:(JavaNetInet6Address * __nonnull)ip;

/*!
 @brief Returns a new InetAddress that is one more than the passed in address.This method works for
  both IPv4 and IPv6 addresses.
 @param address the InetAddress to increment
 @return a new InetAddress that is one more than the passed in address
 @throw IllegalArgumentExceptionif InetAddress is at the end of its range
 @since 10.0
 */
+ (JavaNetInetAddress *)incrementWithJavaNetInetAddress:(JavaNetInetAddress * __nonnull)address;

/*!
 @brief Evaluates whether the argument is a 6to4 address.
 <p>6to4 addresses begin with the <code>"2002::/16"</code> prefix. The next 32 bits are the IPv4
  address of the host to which IPv6-in-IPv4 tunneled packets should be routed. 
 <p>For more on 6to4 addresses see section 2 of <a target="_parent" href="http://tools.ietf.org/html/rfc3056#section-2">
 RFC 3056</a>.
 @param ip<code>Inet6Address</code>  to be examined for 6to4 address format
 @return <code>true</code> if the argument is a 6to4 address
 */
+ (jboolean)is6to4AddressWithJavaNetInet6Address:(JavaNetInet6Address * __nonnull)ip;

/*!
 @brief Evaluates whether the argument is an IPv6 "compat" address.
 <p>An "IPv4 compatible", or "compat", address is one with 96 leading bits of zero, with the
  remaining 32 bits interpreted as an IPv4 address. These are conventionally represented in
  string literals as <code>"::192.168.0.1"</code>, though <code>"::c0a8:1"</code> is also considered an
  IPv4 compatible address (and equivalent to <code>"::192.168.0.1"</code>).
  
 <p>For more on IPv4 compatible addresses see section 2.5.5.1 of <a target="_parent" href="http://tools.ietf.org/html/rfc4291#section-2.5.5.1">
 RFC 4291</a>.
  
 <p>NOTE: This method is different from <code>Inet6Address.isIPv4CompatibleAddress</code> in that it
  more correctly classifies <code>"::"</code> and <code>"::1"</code> as proper IPv6 addresses (which they
  are), NOT IPv4 compatible addresses (which they are generally NOT considered to be).
 @param ip<code>Inet6Address</code>  to be examined for embedded IPv4 compatible address format
 @return <code>true</code> if the argument is a valid "compat" address
 */
+ (jboolean)isCompatIPv4AddressWithJavaNetInet6Address:(JavaNetInet6Address * __nonnull)ip;

/*!
 @brief Returns <code>true</code> if the supplied string is a valid IP string literal, <code>false</code>
  otherwise.
 @param ipString<code>String</code>  to evaluated as an IP string literal
 @return <code>true</code> if the argument is a valid IP string literal
 */
+ (jboolean)isInetAddressWithNSString:(NSString * __nonnull)ipString;

/*!
 @brief Evaluates whether the argument is an ISATAP address.
 <p>From RFC 5214: "ISATAP interface identifiers are constructed in Modified EUI-64 format [...]
  by concatenating the 24-bit IANA OUI (00-00-5E), the 8-bit hexadecimal value 0xFE, and a 32-bit
  IPv4 address in network byte order [...]" 
 <p>For more on ISATAP addresses see section 6.1 of <a target="_parent" href="http://tools.ietf.org/html/rfc5214#section-6.1">
 RFC 5214</a>.
 @param ip<code>Inet6Address</code>  to be examined for ISATAP address format
 @return <code>true</code> if the argument is an ISATAP address
 */
+ (jboolean)isIsatapAddressWithJavaNetInet6Address:(JavaNetInet6Address * __nonnull)ip;

/*!
 @brief Evaluates whether the argument is an "IPv4 mapped" IPv6 address.
 <p>An "IPv4 mapped" address is anything in the range ::ffff:0:0/96 (sometimes written as
  ::ffff:0.0.0.0/96), with the last 32 bits interpreted as an IPv4 address. 
 <p>For more on IPv4 mapped addresses see section 2.5.5.2 of <a target="_parent" href="http://tools.ietf.org/html/rfc4291#section-2.5.5.2">
 RFC 4291</a>.
  
 <p>Note: This method takes a <code>String</code> argument because <code>InetAddress</code> automatically
  collapses mapped addresses to IPv4. (It is actually possible to avoid this using one of the
  obscure <code>Inet6Address</code> methods, but it would be unwise to depend on such a
  poorly-documented feature.)
 @param ipString<code>String</code>  to be examined for embedded IPv4-mapped IPv6 address format
 @return <code>true</code> if the argument is a valid "mapped" address
 @since 10.0
 */
+ (jboolean)isMappedIPv4AddressWithNSString:(NSString * __nonnull)ipString;

/*!
 @brief Returns true if the InetAddress is either 255.255.255.255 for IPv4 or
  ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff for IPv6.
 @return true if the InetAddress is either 255.255.255.255 for IPv4 or
      ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff for IPv6
 @since 10.0
 */
+ (jboolean)isMaximumWithJavaNetInetAddress:(JavaNetInetAddress * __nonnull)address;

/*!
 @brief Evaluates whether the argument is a Teredo address.
 <p>Teredo addresses begin with the <code>"2001::/32"</code> prefix.
 @param ip<code>Inet6Address</code>  to be examined for Teredo address format
 @return <code>true</code> if the argument is a Teredo address
 */
+ (jboolean)isTeredoAddressWithJavaNetInet6Address:(JavaNetInet6Address * __nonnull)ip;

/*!
 @brief Returns <code>true</code> if the supplied string is a valid URI IP string literal, <code>false</code>
  otherwise.
 @param ipString<code>String</code>  to evaluated as an IP URI host string literal
 @return <code>true</code> if the argument is a valid IP URI host
 */
+ (jboolean)isUriInetAddressWithNSString:(NSString * __nonnull)ipString;

/*!
 @brief Returns the string representation of an <code>InetAddress</code>.
 <p>For IPv4 addresses, this is identical to <code>InetAddress.getHostAddress()</code>, but for IPv6
  addresses, the output follows <a href="http://tools.ietf.org/html/rfc5952">RFC 5952</a> section
  4. The main difference is that this method uses "::" for zero compression, while Java's version
  uses the uncompressed form. 
 <p>This method uses hexadecimal for all IPv6 addresses, including IPv4-mapped IPv6 addresses
  such as "::c000:201". The output does not include a Scope ID.
 @param ip<code>InetAddress</code>  to be converted to an address string
 @return <code>String</code> containing the text-formatted IP address
 @since 10.0
 */
+ (NSString *)toAddrStringWithJavaNetInetAddress:(JavaNetInetAddress * __nonnull)ip;

/*!
 @brief Returns the string representation of an <code>InetAddress</code> suitable for inclusion in a URI.
 <p>For IPv4 addresses, this is identical to <code>InetAddress.getHostAddress()</code>, but for IPv6
  addresses it compresses zeroes and surrounds the text with square brackets; for example <code>"[2001:db8::1]"</code>
 .
  
 <p>Per section 3.2.2 of <a target="_parent" href="http://tools.ietf.org/html/rfc3986#section-3.2.2">
 RFC 3986</a>, a URI containing an IPv6
  string literal is of the form <code>"http://[2001:db8::1]:8888/index.html"</code>.
  
 <p>Use of either <code>InetAddresses.toAddrString</code>, <code>InetAddress.getHostAddress()</code>, or
  this method is recommended over <code>InetAddress.toString()</code> when an IP address string
  literal is desired. This is because <code>InetAddress.toString()</code> prints the hostname and the
  IP address string joined by a "/".
 @param ip<code>InetAddress</code>  to be converted to URI string literal
 @return <code>String</code> containing URI-safe string literal
 */
+ (NSString *)toUriStringWithJavaNetInetAddress:(JavaNetInetAddress * __nonnull)ip;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonNetInetAddresses)

FOUNDATION_EXPORT JavaNetInetAddress *ComGoogleCommonNetInetAddresses_forStringWithNSString_(NSString *ipString);

FOUNDATION_EXPORT jboolean ComGoogleCommonNetInetAddresses_isInetAddressWithNSString_(NSString *ipString);

FOUNDATION_EXPORT NSString *ComGoogleCommonNetInetAddresses_toAddrStringWithJavaNetInetAddress_(JavaNetInetAddress *ip);

FOUNDATION_EXPORT NSString *ComGoogleCommonNetInetAddresses_toUriStringWithJavaNetInetAddress_(JavaNetInetAddress *ip);

FOUNDATION_EXPORT JavaNetInetAddress *ComGoogleCommonNetInetAddresses_forUriStringWithNSString_(NSString *hostAddr);

FOUNDATION_EXPORT jboolean ComGoogleCommonNetInetAddresses_isUriInetAddressWithNSString_(NSString *ipString);

FOUNDATION_EXPORT jboolean ComGoogleCommonNetInetAddresses_isCompatIPv4AddressWithJavaNetInet6Address_(JavaNetInet6Address *ip);

FOUNDATION_EXPORT JavaNetInet4Address *ComGoogleCommonNetInetAddresses_getCompatIPv4AddressWithJavaNetInet6Address_(JavaNetInet6Address *ip);

FOUNDATION_EXPORT jboolean ComGoogleCommonNetInetAddresses_is6to4AddressWithJavaNetInet6Address_(JavaNetInet6Address *ip);

FOUNDATION_EXPORT JavaNetInet4Address *ComGoogleCommonNetInetAddresses_get6to4IPv4AddressWithJavaNetInet6Address_(JavaNetInet6Address *ip);

FOUNDATION_EXPORT jboolean ComGoogleCommonNetInetAddresses_isTeredoAddressWithJavaNetInet6Address_(JavaNetInet6Address *ip);

FOUNDATION_EXPORT ComGoogleCommonNetInetAddresses_TeredoInfo *ComGoogleCommonNetInetAddresses_getTeredoInfoWithJavaNetInet6Address_(JavaNetInet6Address *ip);

FOUNDATION_EXPORT jboolean ComGoogleCommonNetInetAddresses_isIsatapAddressWithJavaNetInet6Address_(JavaNetInet6Address *ip);

FOUNDATION_EXPORT JavaNetInet4Address *ComGoogleCommonNetInetAddresses_getIsatapIPv4AddressWithJavaNetInet6Address_(JavaNetInet6Address *ip);

FOUNDATION_EXPORT jboolean ComGoogleCommonNetInetAddresses_hasEmbeddedIPv4ClientAddressWithJavaNetInet6Address_(JavaNetInet6Address *ip);

FOUNDATION_EXPORT JavaNetInet4Address *ComGoogleCommonNetInetAddresses_getEmbeddedIPv4ClientAddressWithJavaNetInet6Address_(JavaNetInet6Address *ip);

FOUNDATION_EXPORT jboolean ComGoogleCommonNetInetAddresses_isMappedIPv4AddressWithNSString_(NSString *ipString);

FOUNDATION_EXPORT JavaNetInet4Address *ComGoogleCommonNetInetAddresses_getCoercedIPv4AddressWithJavaNetInetAddress_(JavaNetInetAddress *ip);

FOUNDATION_EXPORT jint ComGoogleCommonNetInetAddresses_coerceToIntegerWithJavaNetInetAddress_(JavaNetInetAddress *ip);

FOUNDATION_EXPORT JavaNetInet4Address *ComGoogleCommonNetInetAddresses_fromIntegerWithInt_(jint address);

FOUNDATION_EXPORT JavaNetInetAddress *ComGoogleCommonNetInetAddresses_fromLittleEndianByteArrayWithByteArray_(IOSByteArray *addr);

FOUNDATION_EXPORT JavaNetInetAddress *ComGoogleCommonNetInetAddresses_decrementWithJavaNetInetAddress_(JavaNetInetAddress *address);

FOUNDATION_EXPORT JavaNetInetAddress *ComGoogleCommonNetInetAddresses_incrementWithJavaNetInetAddress_(JavaNetInetAddress *address);

FOUNDATION_EXPORT jboolean ComGoogleCommonNetInetAddresses_isMaximumWithJavaNetInetAddress_(JavaNetInetAddress *address);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonNetInetAddresses)

#endif

#if !defined (ComGoogleCommonNetInetAddresses_TeredoInfo_) && (INCLUDE_ALL_ComGoogleCommonNetInetAddresses || defined(INCLUDE_ComGoogleCommonNetInetAddresses_TeredoInfo))
#define ComGoogleCommonNetInetAddresses_TeredoInfo_

@class JavaNetInet4Address;

/*!
 @brief A simple immutable data class to encapsulate the information to be found in a Teredo address.
 <p>All of the fields in this class are encoded in various portions of the IPv6 address as part
  of the protocol. More protocols details can be found at: <a target="_parent" href="http://en.wikipedia.org/wiki/Teredo_tunneling">
 http://en.wikipedia.
  org/wiki/Teredo_tunneling</a>.
  
 <p>The RFC can be found here: <a target="_parent" href="http://tools.ietf.org/html/rfc4380">RFC
  4380</a>.
 @since 5.0
 */
@interface ComGoogleCommonNetInetAddresses_TeredoInfo : NSObject

#pragma mark Public

/*!
 @brief Constructs a TeredoInfo instance.
 <p>Both server and client can be <code>null</code>, in which case the value <code>"0.0.0.0"</code> will
  be assumed.
 @throw IllegalArgumentExceptionif either of the <code>port</code> or the <code>flags</code> arguments
      are out of range of an unsigned short
 */
- (instancetype __nonnull)initWithJavaNetInet4Address:(JavaNetInet4Address * __nullable)server
                              withJavaNetInet4Address:(JavaNetInet4Address * __nullable)client
                                              withInt:(jint)port
                                              withInt:(jint)flags;

- (JavaNetInet4Address *)getClient;

- (jint)getFlags;

- (jint)getPort;

- (JavaNetInet4Address *)getServer;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonNetInetAddresses_TeredoInfo)

FOUNDATION_EXPORT void ComGoogleCommonNetInetAddresses_TeredoInfo_initWithJavaNetInet4Address_withJavaNetInet4Address_withInt_withInt_(ComGoogleCommonNetInetAddresses_TeredoInfo *self, JavaNetInet4Address *server, JavaNetInet4Address *client, jint port, jint flags);

FOUNDATION_EXPORT ComGoogleCommonNetInetAddresses_TeredoInfo *new_ComGoogleCommonNetInetAddresses_TeredoInfo_initWithJavaNetInet4Address_withJavaNetInet4Address_withInt_withInt_(JavaNetInet4Address *server, JavaNetInet4Address *client, jint port, jint flags) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonNetInetAddresses_TeredoInfo *create_ComGoogleCommonNetInetAddresses_TeredoInfo_initWithJavaNetInet4Address_withJavaNetInet4Address_withInt_withInt_(JavaNetInet4Address *server, JavaNetInet4Address *client, jint port, jint flags);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonNetInetAddresses_TeredoInfo)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonNetInetAddresses")
