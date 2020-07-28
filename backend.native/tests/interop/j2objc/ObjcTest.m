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

- (NSString *)returnString:(NSString *)s {
  return s;
}
+ (int)return100Static {
    return 100;
}
@end

@implementation ExtendsFoo

- (int)add2:(int)x secondparam:(int)y {
  return x - y;
}
- (int)add3:(int)x secondparam:(int)y thirdparam:(int)z {
  return [self add2:x secondparam:[self add2:y secondparam:z]];
}

- (Foo *)returnFoo {
  return [Foo new];
}
@end
