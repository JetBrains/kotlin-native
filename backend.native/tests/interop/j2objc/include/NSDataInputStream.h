//
//  NSDataInputStream.h
//  JreEmulation
//
//  Created by Pankaj Kakkar on 5/20/13.
//
//

#ifndef _NSDataInputStream_H_
#define _NSDataInputStream_H_

#import "java/io/InputStream.h"

// A concrete subclass of java.io.InputStream that reads from
// a given NSData instance. The NSData instance is copied at
// initialization, so further modifications (if it happened to
// be mutable) will not be visible.
@interface NSDataInputStream : JavaIoInputStream

- (instancetype)initWithData:(NSData *)data;
+ (NSDataInputStream *)streamWithData:(NSData *)data;

@end

#endif // _NSDataInputStream_H_
