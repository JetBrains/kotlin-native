#import <Foundation/NSObject.h>

@protocol KT38234
-(int)foo;
@end;

@interface KT38234 : NSObject <KT38234>
-(int)callFoo;
@end;
