#if defined(__linux__) || defined(__APPLE__)
#include <dlfcn.h>
#include <stdio.h>
#include <signal.h>
#if defined(__linux__)
#include <link.h>
#endif

typedef void (*toggleCrashRecovery_t)(int);
typedef int (*sigaction_t)(int, const struct sigaction *, struct sigaction *);

static sigaction_t realSigaction = 0;

static int mySigaction(int sig, const struct sigaction *act, struct sigaction * oact) {
  if (sig == SIGILL || sig == SIGFPE|| sig ==  SIGSEGV || sig == SIGBUS || sig == SIGUSR1|| sig == SIGUSR2) return 0;
  return realSigaction(sig, act, oact);
}

__attribute__((constructor))
static void initSignalChaining() {
  toggleCrashRecovery_t clang_sym = 0;
  void** base = 0;
  Dl_info info;

  clang_sym = (toggleCrashRecovery_t)dlsym(RTLD_DEFAULT, "clang_toggleCrashRecovery");
  if (!clang_sym) {
    fprintf(stderr, "Cannot find clang_toggleCrashRecovery\n");
    return;
  }
  if (dladdr((void*)clang_sym, &info) == 0) return;
  base = (void**)info.dli_fbase;

  // Force resolving of lazy symbols.
  clang_sym(1);
  clang_sym(0);

  // And then patch GOT.
#if defined(__linux__)
  {
    // On Linux we have to be a bit tricky, as there's unmapped gap between code and GOT.
     struct link_map* linkmap = 0;
     if (dladdr1((void*)clang_sym, &info, (void**)&linkmap, RTLD_DL_LINKMAP) == 0) return;
     base = (void**)linkmap->l_ld;
  }
#endif
  realSigaction = (sigaction_t)dlsym(RTLD_DEFAULT, "sigaction");
  for (int index = 0, patched = 0; patched < 1; index++) {
    void* value = base[index];
    if (value == realSigaction) {
        base[index] = (void*)mySigaction;
        patched++;
    }
    if (value == mySigaction) {
        patched++;
    }
  }
}
#endif // defined(__linux__) || defined(__APPLE__)
