#import <Foundation/Foundation.h>

@interface Foo : NSObject
- (instancetype)init;

- (int)returnNum:(int)x;
- (int)return100;
- (int)add2:(int)x secondparam:(int)y;
+ (int)return100Static;
@end
