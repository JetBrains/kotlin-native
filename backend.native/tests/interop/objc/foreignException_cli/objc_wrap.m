#include "objc_wrap.h"

#import <Foundation/Foundation.h>

void raiseExc(id name, id reason) {
    [NSException raise:name format:@"%@", reason];
}

id logExc(id exc) {
    assert([exc isKindOfClass:[NSException class]]);
    return ((NSException*)exc).name;
}
