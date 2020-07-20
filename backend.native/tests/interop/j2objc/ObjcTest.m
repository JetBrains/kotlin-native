#import "ObjcTest.h"

@implementation Foo

- (instancetype)init {
  return self;
}

- (int)returnNum:(int)x {
    return x;
}

- (int)return100 {
    return 100;
}

- (int)add2:(int)x secondparam:(int)y {
    return x + y;
}

+ (int)return100Static {
    return 100;
}
@end

@implementation ExtendsFoo

- (instancetype)init {
  return self;
}

- (int)add3:(int)x secondparam:(int)y thirdparam:(int)z {
  return [self add2:x secondparam:[self add2:y secondparam:z]];
}
@end