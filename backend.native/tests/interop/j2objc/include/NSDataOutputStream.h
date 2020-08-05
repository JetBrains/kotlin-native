//
//  NSDataOutputStream.h
//  JreEmulation
//
//  Created by Pankaj Kakkar on 5/20/13.
//
//

#ifndef _NSDataOutputStream_H_
#define _NSDataOutputStream_H_

#import "java/io/OutputStream.h"

// A concrete subclass of java.io.InputStream that writes into
// a backing NSData instance, retrievable at any point.
@interface NSDataOutputStream : JavaIoOutputStream

+ (NSDataOutputStream *)stream;

// Retrieve the data written so far. If further writes to the
// stream are possible, callers must copy this instance to insulate
// themselves.
- (NSData *)data;

@end

#endif // _NSDataOutputStream_H_
