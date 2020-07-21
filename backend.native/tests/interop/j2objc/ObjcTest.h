// TODO: Replace code with J2ObjC generated code once nameMangling is complete

#import <Foundation/Foundation.h>

@interface Foo : NSObject
- (instancetype)init;

- (int)returnNum:(int)x;
- (int)return100;
- (int)add2:(int)x secondparam:(int)y;
- (NSString *)returnString:(NSString *)s;
+ (int)return100Static;
@end

@interface ExtendsFoo: Foo
- (instancetype)init;

- (int)add3:(int)x secondparam:(int)y thirdparam:(int)z;
- (Foo *)returnFoo;
@end
