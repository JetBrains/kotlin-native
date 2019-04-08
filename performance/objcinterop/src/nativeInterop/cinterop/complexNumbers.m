#import <stdio.h>
#import "complexNumbers.h"

@interface Complex ()
- (void)zeroComplex;
@end

@implementation Complex
- (id)init {
    return [self initWithRe: 0.0 andIm: 0.0];
}

- (id)initWithRe: (double)_re andIm: (double)_im {
    if (self = [super init]) {
        re = _re;
        im = _im;
        format = @"re: %.1lf im: %.1lf";
    }
    return self;
}

+ (Complex *)complexWithRe: (double)re andIm: (double)im {
    return [[Complex alloc] initWithRe: re andIm: im];
}
- (Complex *)add: (Complex *)other {
    return [[Complex alloc] initWithRe: re + other->re andIm: im + other->im];
}
- (Complex *)sub: (Complex *)other {
    return [[Complex alloc] initWithRe: re - other.re andIm: im - other.im];
}

- (NSString *)description {
    return [NSString stringWithFormat: format, re, im];
}

- (void)setFormat: (NSString *)newFormat {
    format = newFormat
}

- (void)zeroComplex {
    re = 0;
    im = 0;
}
@end

@implementation Complex (CategorizedComplex)
- (Complex *)mul: (Complex *)other {
    return [Complex complexWithRe: re * other.re - im * other.im andIm: re * other.im + im * other.re];
}
- (Complex *)div: (Complex *)other {
    double retRe;
    double retIm;
    double denominator;
    denominator = other.re * other.re + other.im * other.im;
    if (!denominator)
        return nil;
    retRe = (re * other.re + im * other.im) / denominator;
    retIm = (im * other.re - re * other.im) / denominator;
    return [Complex complexWithRe: retRe andIm: retIm];
}
@end