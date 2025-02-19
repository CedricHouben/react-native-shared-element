//
//  RNSharedElementCornerRadii_m
//  react-native-shared-element
//

#import "RNSharedElementCornerRadii.h"
#import <React/RCTUtils.h>
#import <React/RCTI18nUtil.h>

static CGFloat RNSharedElementDefaultIfNegativeTo(CGFloat defaultValue, CGFloat x)
{
  return x >= 0 ? x : defaultValue;
};

#define RADII_COUNT 9

@implementation RNSharedElementCornerRadii {
  CGFloat _radii[RADII_COUNT];
  BOOL _invalidated;
  CGRect _cachedBounds;
  RCTCornerRadii _cachedRadii;
}

- (instancetype)init
{
  if (self = [super init]) {
    _invalidated = YES;
    _layoutDirection = UIUserInterfaceLayoutDirectionLeftToRight;
    for (int i = 0; i < RADII_COUNT; i++) {
      _radii[i] = -1;
    }
  }
  return self;
}


#pragma mark Properties

- (void)setLayoutDirection:(UIUserInterfaceLayoutDirection)layoutDirection
{
  if (_layoutDirection != layoutDirection) {
    _layoutDirection = layoutDirection;
    _invalidated = YES;
  }
}


#pragma mark Methods

- (CGFloat)radiusForCorner:(RNSharedElementCorner)corner
{
  return _radii[corner];
}

- (BOOL)setRadius:(CGFloat)radius corner:(RNSharedElementCorner)corner
{
  if (_radii[corner] != radius) {
    _radii[corner] = radius;
    _invalidated = YES;
    return YES;
  }
  return NO;
}

- (void)updateClipMaskForLayer:(CALayer *)layer bounds:(CGRect)bounds
{
  RCTCornerRadii radii = [self radiiForBounds:bounds];
  
  CALayer *mask = nil;
  CGFloat cornerRadius = 0;
  
  if (RCTCornerRadiiAreEqualAndSymmetrical(radii)) {
    cornerRadius = radii.topLeftHorizontal;
  } else {
    CAShapeLayer *shapeLayer = [CAShapeLayer layer];
    RCTCornerInsets cornerInsets = RCTGetCornerInsets(radii, UIEdgeInsetsZero);
    CGPathRef path = RCTPathCreateWithRoundedRect(bounds, cornerInsets, NULL);
    shapeLayer.path = path;
    CGPathRelease(path);
    mask = shapeLayer;
  }
  
  layer.cornerRadius = cornerRadius;
  layer.mask = mask;
}

- (void)updateShadowPathForLayer:(CALayer *)layer bounds:(CGRect)bounds
{
  RCTCornerRadii radii = [self radiiForBounds:bounds];
  
  BOOL hasShadow = layer.shadowOpacity * CGColorGetAlpha(layer.shadowColor) > 0;
  if (!hasShadow) {
    layer.shadowPath = nil;
    return;
  }
  
  RCTCornerInsets cornerInsets = RCTGetCornerInsets(radii, UIEdgeInsetsZero);
  CGPathRef path = RCTPathCreateWithRoundedRect(bounds, cornerInsets, NULL);
  layer.shadowPath = path;
  CGPathRelease(path);
}

- (RCTCornerRadii)radiiForBounds:(CGRect)bounds;
{
  if (!_invalidated && CGRectEqualToRect(_cachedBounds, bounds)) {
    return _cachedRadii;
  }
  
  const BOOL isRTL = _layoutDirection == UIUserInterfaceLayoutDirectionRightToLeft;
  const CGFloat radius = MAX(0, _radii[RNSharedElementCornerAll]);
  RCTCornerRadii result;
  
  if ([[RCTI18nUtil sharedInstance] doLeftAndRightSwapInRTL]) {
    const CGFloat topStartRadius = RNSharedElementDefaultIfNegativeTo(_radii[RNSharedElementCornerTopLeft], _radii[RNSharedElementCornerTopStart]);
    const CGFloat topEndRadius = RNSharedElementDefaultIfNegativeTo(_radii[RNSharedElementCornerTopRight], _radii[RNSharedElementCornerTopEnd]);
    const CGFloat bottomStartRadius = RNSharedElementDefaultIfNegativeTo(_radii[RNSharedElementCornerBottomLeft], _radii[RNSharedElementCornerBottomStart]);
    const CGFloat bottomEndRadius = RNSharedElementDefaultIfNegativeTo(_radii[RNSharedElementCornerBottomRight], _radii[RNSharedElementCornerBottomEnd]);
    
    const CGFloat directionAwareTopLeftRadius = isRTL ? topEndRadius : topStartRadius;
    const CGFloat directionAwareTopRightRadius = isRTL ? topStartRadius : topEndRadius;
    const CGFloat directionAwareBottomLeftRadius = isRTL ? bottomEndRadius : bottomStartRadius;
    const CGFloat directionAwareBottomRightRadius = isRTL ? bottomStartRadius : bottomEndRadius;
    
    result.topLeftHorizontal = RNSharedElementDefaultIfNegativeTo(radius, directionAwareTopLeftRadius);
    result.topRightHorizontal = RNSharedElementDefaultIfNegativeTo(radius, directionAwareTopRightRadius);
    result.bottomLeftHorizontal = RNSharedElementDefaultIfNegativeTo(radius, directionAwareBottomLeftRadius);
    result.bottomRightHorizontal = RNSharedElementDefaultIfNegativeTo(radius, directionAwareBottomRightRadius);
    result.topLeftVertical = RNSharedElementDefaultIfNegativeTo(radius, directionAwareTopLeftRadius);
    result.topRightVertical = RNSharedElementDefaultIfNegativeTo(radius, directionAwareTopRightRadius);
    result.bottomLeftVertical = RNSharedElementDefaultIfNegativeTo(radius, directionAwareBottomLeftRadius);
    result.bottomRightVertical = RNSharedElementDefaultIfNegativeTo(radius, directionAwareBottomRightRadius);
  } else {
    const CGFloat directionAwareTopLeftRadius = isRTL ? _radii[RNSharedElementCornerTopEnd] : _radii[RNSharedElementCornerTopStart];
    const CGFloat directionAwareTopRightRadius = isRTL ? _radii[RNSharedElementCornerTopStart] : _radii[RNSharedElementCornerTopEnd];
    const CGFloat directionAwareBottomLeftRadius = isRTL ? _radii[RNSharedElementCornerBottomEnd] : _radii[RNSharedElementCornerBottomStart];
    const CGFloat directionAwareBottomRightRadius = isRTL ? _radii[RNSharedElementCornerBottomStart] : _radii[RNSharedElementCornerBottomEnd];
    
    result.topLeftHorizontal =
    RNSharedElementDefaultIfNegativeTo(radius, RNSharedElementDefaultIfNegativeTo(_radii[RNSharedElementCornerTopLeft], directionAwareTopLeftRadius));
    result.topRightHorizontal =
    RNSharedElementDefaultIfNegativeTo(radius, RNSharedElementDefaultIfNegativeTo(_radii[RNSharedElementCornerTopRight], directionAwareTopRightRadius));
    result.bottomLeftHorizontal =
    RNSharedElementDefaultIfNegativeTo(radius, RNSharedElementDefaultIfNegativeTo(_radii[RNSharedElementCornerBottomLeft], directionAwareBottomLeftRadius));
    result.bottomRightHorizontal = RNSharedElementDefaultIfNegativeTo(
    radius, RNSharedElementDefaultIfNegativeTo(_radii[RNSharedElementCornerBottomRight], directionAwareBottomRightRadius));
    result.topLeftVertical =
    RNSharedElementDefaultIfNegativeTo(radius, RNSharedElementDefaultIfNegativeTo(_radii[RNSharedElementCornerTopLeft], directionAwareTopLeftRadius));
    result.topRightVertical =
    RNSharedElementDefaultIfNegativeTo(radius, RNSharedElementDefaultIfNegativeTo(_radii[RNSharedElementCornerTopRight], directionAwareTopRightRadius));
    result.bottomLeftVertical =
    RNSharedElementDefaultIfNegativeTo(radius, RNSharedElementDefaultIfNegativeTo(_radii[RNSharedElementCornerBottomLeft], directionAwareBottomLeftRadius));
    result.bottomRightVertical = RNSharedElementDefaultIfNegativeTo(
                                                            radius, RNSharedElementDefaultIfNegativeTo(_radii[RNSharedElementCornerBottomRight], directionAwareBottomRightRadius));
  }
  
  // Get scale factors required to prevent radii from overlapping
  const CGFloat topScaleFactor = RCTZeroIfNaN(MIN(1, bounds.size.width / (result.topLeftHorizontal + result.topRightHorizontal)));
  const CGFloat bottomScaleFactor = RCTZeroIfNaN(MIN(1, bounds.size.width / (result.bottomLeftHorizontal + result.bottomRightHorizontal)));
  const CGFloat rightScaleFactor = RCTZeroIfNaN(MIN(1, bounds.size.height / (result.topRightHorizontal + result.bottomRightHorizontal)));
  const CGFloat leftScaleFactor = RCTZeroIfNaN(MIN(1, bounds.size.height / (result.topLeftHorizontal + result.bottomLeftHorizontal)));

  result.topLeftHorizontal *= MIN(topScaleFactor, leftScaleFactor);
  result.topRightHorizontal *= MIN(topScaleFactor, rightScaleFactor);
  result.bottomLeftHorizontal *= MIN(bottomScaleFactor, leftScaleFactor);
  result.bottomRightHorizontal *= MIN(bottomScaleFactor, rightScaleFactor);
  result.topLeftVertical *= MIN(topScaleFactor, leftScaleFactor);
  result.topRightVertical *= MIN(topScaleFactor, rightScaleFactor);
  result.bottomLeftVertical *= MIN(bottomScaleFactor, leftScaleFactor);
  result.bottomRightVertical *= MIN(bottomScaleFactor, rightScaleFactor);
  
  _cachedBounds = bounds;
  _cachedRadii = result;
  _invalidated = NO;
  
  return result;
}

@end
