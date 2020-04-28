#include "KT38234_override.h"

@implementation Base
-(int)foo {
    return 1;
}
-(int)callFoo {
    return [self foo];
}
@end;
