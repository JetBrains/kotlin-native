#include "KT35056_foreignException.h"

#import <Foundation/Foundation.h>

void raiseExc(id name) {
    [NSException raise:name format:@"Illegal value %d", 42];
}

id logExc(void* exc) {
    NSLog(@"logExc> handled");
    NSException* e = (__bridge NSException*)exc;  // FixMe: bridged cast
    return e.name;
}

