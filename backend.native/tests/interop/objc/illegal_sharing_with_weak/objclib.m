#import "objclib.h"

static NSObject* __weak globalObject = nil;

void setObject(NSObject* obj) {
  globalObject = obj;
}

bool isObjectAliveShouldCrash() {
  return globalObject != nil;
}
