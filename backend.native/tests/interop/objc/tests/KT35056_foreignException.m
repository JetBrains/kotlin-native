#include "KT35056_foreignException.h"

#import <Foundation/Foundation.h>

void raiseExc(id name, id reason) {
    [NSException raise:name format:@"%@", reason];
}

id logExc(id exc) {
NSLog(@"logExc> handled");
    assert([exc isKindOfClass:[NSException class]]);
    return ((NSException*)exc).name;
}
