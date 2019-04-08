#import <Foundation/Foundation.h>

@protocol CustomNumber
@required
- (id<CustomNumber>)add: (id<CustomNumber>)other;
- (id<CustomNumber>)sub: (id<CustomNumber>)other;
@end

@interface Complex : NSObject<CustomNumber> {
    double re;
    double im;
    NSString *format;
}

@property (nonatomic, readonly) double re;
@property (nonatomic, readonly) double im;
@property (retain, nonatomic, readwrite) NSString *format;

- (id)initWithRe: (double)re andIm: (double)im;
+ (Complex *)complexWithRe: (double)re andIm: (double)im;
- (void)setFormat: (NSString *)newFormat;
@end

@interface Complex (CategorizedComplex)
- (Complex *)mul: (Complex *)other;
- (Complex *)div: (Complex *)other;
@end
