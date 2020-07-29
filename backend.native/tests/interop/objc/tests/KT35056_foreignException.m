#include "KT35056_foreignException.h"

#import <Foundation/Foundation.h>

void raiseExc(id name) {
    [NSException raise:name format:@"Illegal value %d", 42];
}

id logExc(id exc) {
    NSLog(@"logExc> handled");
    assert([exc isKindOfClass:[NSException class]]);
    return ((NSException*)exc).name;
}

