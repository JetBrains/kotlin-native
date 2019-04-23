#import <Foundation/Foundation.h>

@protocol CustomNumber
@required
- (id<CustomNumber>)add: (id<CustomNumber>)other;
- (id<CustomNumber>)sub: (id<CustomNumber>)other;
@end

@interface Complex : NSObject<CustomNumber>

@property (nonatomic, readonly) double re;
@property (nonatomic, readonly) double im;
@property (nonatomic, readwrite) NSString *format;

- (id)initWithRe: (double)re andIm: (double)im;
+ (Complex *)complexWithRe: (double)re im: (double)im;
@end

@interface Complex (CategorizedComplex)
- (Complex *)mul: (Complex *)other;
- (Complex *)div: (Complex *)other;
@end
