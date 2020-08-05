//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: android/platform/libcore/ojluni/src/main/java/java/sql/SQLOutput.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_JavaSqlSQLOutput")
#ifdef RESTRICT_JavaSqlSQLOutput
#define INCLUDE_ALL_JavaSqlSQLOutput 0
#else
#define INCLUDE_ALL_JavaSqlSQLOutput 1
#endif
#undef RESTRICT_JavaSqlSQLOutput

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (JavaSqlSQLOutput_) && (INCLUDE_ALL_JavaSqlSQLOutput || defined(INCLUDE_JavaSqlSQLOutput))
#define JavaSqlSQLOutput_

@class IOSByteArray;
@class JavaIoInputStream;
@class JavaIoReader;
@class JavaMathBigDecimal;
@class JavaNetURL;
@class JavaSqlDate;
@class JavaSqlTime;
@class JavaSqlTimestamp;
@protocol JavaSqlArray;
@protocol JavaSqlBlob;
@protocol JavaSqlClob;
@protocol JavaSqlNClob;
@protocol JavaSqlRef;
@protocol JavaSqlRowId;
@protocol JavaSqlSQLData;
@protocol JavaSqlSQLXML;
@protocol JavaSqlStruct;

/*!
 @brief The output stream for writing the attributes of a user-defined
  type back to the database.This interface, used
  only for custom mapping, is used by the driver, and its
  methods are never directly invoked by a programmer.
 <p>When an object of a class implementing the interface 
 <code>SQLData</code> is passed as an argument to an SQL statement, the
  JDBC driver calls the method <code>SQLData.getSQLType</code> to
  determine the  kind of SQL
  datum being passed to the database.
  The driver then creates an instance of <code>SQLOutput</code> and
  passes it to the method <code>SQLData.writeSQL</code>.
  The method <code>writeSQL</code> in turn calls the
  appropriate <code>SQLOutput</code> <i>writer</i> methods 
 <code>writeBoolean</code>, <code>writeCharacterStream</code>, and so on)
  to write data from the <code>SQLData</code> object to
  the <code>SQLOutput</code> output stream as the
  representation of an SQL user-defined type.
 @since 1.2
 */
@protocol JavaSqlSQLOutput < JavaObject >

/*!
 @brief Writes the next attribute to the stream as a <code>String</code>
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeStringWithNSString:(NSString *)x;

/*!
 @brief Writes the next attribute to the stream as a Java boolean.
 Writes the next attribute to the stream as a <code>String</code>
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeBooleanWithBoolean:(jboolean)x;

/*!
 @brief Writes the next attribute to the stream as a Java byte.
 Writes the next attribute to the stream as a <code>String</code>
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeByteWithByte:(jbyte)x;

/*!
 @brief Writes the next attribute to the stream as a Java short.
 Writes the next attribute to the stream as a <code>String</code>
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeShortWithShort:(jshort)x;

/*!
 @brief Writes the next attribute to the stream as a Java int.
 Writes the next attribute to the stream as a <code>String</code>
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeIntWithInt:(jint)x;

/*!
 @brief Writes the next attribute to the stream as a Java long.
 Writes the next attribute to the stream as a <code>String</code>
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeLongWithLong:(jlong)x;

/*!
 @brief Writes the next attribute to the stream as a Java float.
 Writes the next attribute to the stream as a <code>String</code>
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeFloatWithFloat:(jfloat)x;

/*!
 @brief Writes the next attribute to the stream as a Java double.
 Writes the next attribute to the stream as a <code>String</code>
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeDoubleWithDouble:(jdouble)x;

/*!
 @brief Writes the next attribute to the stream as a java.math.BigDecimal object.
 Writes the next attribute to the stream as a <code>String</code>
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeBigDecimalWithJavaMathBigDecimal:(JavaMathBigDecimal *)x;

/*!
 @brief Writes the next attribute to the stream as an array of bytes.
 Writes the next attribute to the stream as a <code>String</code>
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeBytesWithByteArray:(IOSByteArray *)x;

/*!
 @brief Writes the next attribute to the stream as a java.sql.Date object.
 Writes the next attribute to the stream as a <code>java.sql.Date</code> object
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeDateWithJavaSqlDate:(JavaSqlDate *)x;

/*!
 @brief Writes the next attribute to the stream as a java.sql.Time object.
 Writes the next attribute to the stream as a <code>java.sql.Date</code> object
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeTimeWithJavaSqlTime:(JavaSqlTime *)x;

/*!
 @brief Writes the next attribute to the stream as a java.sql.Timestamp object.
 Writes the next attribute to the stream as a <code>java.sql.Date</code> object
  in the Java programming language.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeTimestampWithJavaSqlTimestamp:(JavaSqlTimestamp *)x;

/*!
 @brief Writes the next attribute to the stream as a stream of Unicode characters.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeCharacterStreamWithJavaIoReader:(JavaIoReader *)x;

/*!
 @brief Writes the next attribute to the stream as a stream of ASCII characters.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeAsciiStreamWithJavaIoInputStream:(JavaIoInputStream *)x;

/*!
 @brief Writes the next attribute to the stream as a stream of uninterpreted
  bytes.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeBinaryStreamWithJavaIoInputStream:(JavaIoInputStream *)x;

/*!
 @brief Writes to the stream the data contained in the given 
 <code>SQLData</code> object.
 When the <code>SQLData</code> object is <code>null</code>, this
  method writes an SQL <code>NULL</code> to the stream.
  Otherwise, it calls the <code>SQLData.writeSQL</code>
  method of the given object, which
  writes the object's attributes to the stream.
  The implementation of the method <code>SQLData.writeSQ</code>
  calls the appropriate <code>SQLOutput</code> writer method(s)
  for writing each of the object's attributes in order.
  The attributes must be read from an <code>SQLInput</code>
  input stream and written to an <code>SQLOutput</code>
  output stream in the same order in which they were
  listed in the SQL definition of the user-defined type.
 @param x the object representing data of an SQL structured or  distinct type
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeObjectWithJavaSqlSQLData:(id<JavaSqlSQLData>)x;

/*!
 @brief Writes an SQL <code>REF</code> value to the stream.
 @param x a  <code> Ref </code>  object representing data of an SQL
    <code> REF </code>  value
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeRefWithJavaSqlRef:(id<JavaSqlRef>)x;

/*!
 @brief Writes an SQL <code>BLOB</code> value to the stream.
 @param x a  <code> Blob </code>  object representing data of an SQL
    <code> BLOB </code>  value
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeBlobWithJavaSqlBlob:(id<JavaSqlBlob>)x;

/*!
 @brief Writes an SQL <code>CLOB</code> value to the stream.
 @param x a  <code> Clob </code>  object representing data of an SQL
    <code> CLOB </code>  value
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeClobWithJavaSqlClob:(id<JavaSqlClob>)x;

/*!
 @brief Writes an SQL structured type value to the stream.
 @param x a  <code> Struct </code>  object representing data of an SQL
   structured type
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeStructWithJavaSqlStruct:(id<JavaSqlStruct>)x;

/*!
 @brief Writes an SQL <code>ARRAY</code> value to the stream.
 @param x an  <code> Array </code>  object representing data of an SQL
    <code> ARRAY </code>  type
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.2
 */
- (void)writeArrayWithJavaSqlArray:(id<JavaSqlArray>)x;

/*!
 @brief Writes a SQL <code>DATALINK</code> value to the stream.
 @param x a  <code> java.net.URL </code>  object representing the data
   of SQL DATALINK type
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.4
 */
- (void)writeURLWithJavaNetURL:(JavaNetURL *)x;

/*!
 @brief Writes the next attribute to the stream as a <code>String</code>
  in the Java programming language.The driver converts this to a
  SQL <code>NCHAR</code> or 
 <code>NVARCHAR</code> or <code>LONGNVARCHAR</code> value
  (depending on the argument's
  size relative to the driver's limits on <code>NVARCHAR</code> values)
  when it sends it to the stream.
 @param x the value to pass to the database
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.6
 */
- (void)writeNStringWithNSString:(NSString *)x;

/*!
 @brief Writes an SQL <code>NCLOB</code> value to the stream.
 @param x a  <code> NClob </code>  object representing data of an SQL
    <code> NCLOB </code>  value
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.6
 */
- (void)writeNClobWithJavaSqlNClob:(id<JavaSqlNClob>)x;

/*!
 @brief Writes an SQL <code>ROWID</code> value to the stream.
 @param x a  <code> RowId </code>  object representing data of an SQL
    <code> ROWID </code>  value
 @throw SQLExceptionif a database access error occurs
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.6
 */
- (void)writeRowIdWithJavaSqlRowId:(id<JavaSqlRowId>)x;

/*!
 @brief Writes an SQL <code>XML</code> value to the stream.
 @param x a  <code> SQLXML </code>  object representing data of an SQL
    <code> XML </code>  value
 @throw SQLExceptionif a database access error occurs,
  the <code>java.xml.transform.Result</code>,
   <code>Writer</code> or <code>OutputStream</code> has not been closed for the <code>SQLXML</code> object or
   if there is an error processing the XML value.  The <code>getCause</code> method
   of the exception may provide a more detailed exception, for example, if the
   stream does not contain valid XML.
 @throw SQLFeatureNotSupportedExceptionif the JDBC driver does not support
  this method
 @since 1.6
 */
- (void)writeSQLXMLWithJavaSqlSQLXML:(id<JavaSqlSQLXML>)x;

@end

J2OBJC_EMPTY_STATIC_INIT(JavaSqlSQLOutput)

J2OBJC_TYPE_LITERAL_HEADER(JavaSqlSQLOutput)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_JavaSqlSQLOutput")
