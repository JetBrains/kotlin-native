#import <Foundation/NSObject.h>

@protocol P1
-(int)foo;
@end;

@interface Base : NSObject <P1>
-(int)callFoo;
@end;
