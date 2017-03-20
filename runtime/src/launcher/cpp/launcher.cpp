#include "Memory.h"
#include "Natives.h"
#include "Runtime.h"
#include "KString.h"
#include "Types.h"

//--- Setup args --------------------------------------------------------------//

OBJ_GETTER(setupArgs, int argc, char** argv) {
  // The count is one less, because we skip argv[0] which is the binary name.
  ObjHeader* result = AllocArrayInstance(theArrayTypeInfo, argc - 1, OBJ_RESULT);
  ArrayHeader* array = result->array();
  for (int index = 1; index < argc; index++) {
    CreateStringFromCString(
      argv[index], ArrayAddressOfElementAt(array, index - 1));
  }
  return result;
}

//--- main --------------------------------------------------------------------//
extern "C" KInt Konan_start(const ObjHeader*);

extern "C" int Konan_main(int argc, char** argv) {
  RuntimeState* state = InitRuntime();

  if (state == nullptr) {
    return 2;
  }

  KInt exitStatus;
  {
    ObjHolder args;
    setupArgs(argc, argv, args.slot());
    exitStatus = Konan_start(args.obj());
  }

  DeinitRuntime(state);

  return exitStatus;
}

// Allow override of entry point for cases like SDL.
int  __attribute__((weak)) main(int argc, char** argv) {
  return Konan_main(argc, argv);
}
